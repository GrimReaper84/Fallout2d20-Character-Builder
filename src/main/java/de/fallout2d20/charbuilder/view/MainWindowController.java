package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.AppInfo;
import de.fallout2d20.charbuilder.export.sheet.CharacterSheetPdf;
import de.fallout2d20.charbuilder.export.sheet.SheetContent;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.persistence.CharacterFile;
import de.fallout2d20.charbuilder.persistence.CharacterStore;
import de.fallout2d20.charbuilder.persistence.SaveFormatException;
import de.fallout2d20.charbuilder.persistence.legacy.ImportReport;
import de.fallout2d20.charbuilder.view.dialog.AboutDialog;
import de.fallout2d20.charbuilder.view.dialog.ConfirmDialog;
import de.fallout2d20.charbuilder.view.dialog.CreateCharacterDialog;
import de.fallout2d20.charbuilder.view.dialog.ImportReportDialog;
import de.fallout2d20.charbuilder.view.dialog.RulesDialog;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Main window: file, character and GM menus (the character's books and house rules in {@link RulesDialog}) and one
 * tab per open character (H1), plus the GM compendium tab with the party ({@link CompendiumTab}, H2). New and Open
 * always open a new tab; a file that is already open selects its tab. Export, rules and GM free edit act on the
 * selected character; Save, Save As and Close also on the compendium tab (its party). Closing a tab or the window
 * asks for every character and the party with unsaved changes.
 */
public class MainWindowController {

  private static final String APP_TITLE = AppInfo.NAME;
  private static final @NonNull String STATUS_AS_TAB_KEY = "character.statusAsTab";

  private final CharacterStore store = new CharacterStore();
  private final @NonNull ViewPreferences preferences = ViewPreferences.user();
  private final @NonNull FloatingButtonPosition statusButtonPosition =
      new FloatingButtonPosition(preferences, "character.statusButtonPosition");
  private final TabPane documents = new TabPane();
  private final Label emptyState = new Label("No character loaded. Use File → New or File → Open, or GM → Compendium.");
  private final CompendiumTab compendium = new CompendiumTab(documents, () -> this.stage, this::openCharacters,
      this::updateWindow, this::showError);

  @FXML private MenuBar menuBar;
  @FXML private StackPane contentPane;
  @FXML private MenuItem saveItem;
  @FXML private MenuItem saveAsItem;
  @FXML private MenuItem closeItem;
  @FXML private MenuItem createCharacterItem;
  @FXML private MenuItem exportSheetItem;
  @FXML private CheckMenuItem gmModeItem;
  @FXML private MenuItem rulesItem;
  @FXML private CheckMenuItem statusAsTabItem;

  private @Nullable Stage stage;
  /** The folder of the last file opened or saved; proposed by the file dialogs. */
  private @Nullable Path suggestedDirectory;

  @FXML
  private void initialize() {
    statusAsTabItem.setSelected(preferences.getBoolean(STATUS_AS_TAB_KEY, false));
    emptyState.setStyle("-fx-padding: 24;");
    documents.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    documents.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> updateWindow());
    documents.getTabs().addListener((ListChangeListener<Tab>) change -> {
      while (change.next()) {
        if (change.getRemoved().stream().anyMatch(compendium::isTab)) {
          compendium.closed();
        }
      }
      showDocumentsOrEmptyState();
      updateWindow();
    });
    showDocumentsOrEmptyState();
    updateWindow();
  }

  /** Connects the controller to its window; asks before closing with unsaved changes. */
  public void attach(Stage stage) {
    this.stage = stage;
    stage.setOnCloseRequest(this::onCloseRequest);
    BetaMenu.folder().ifPresent(folder -> menuBar.getMenus().add(
        BetaMenu.create(folder, sample -> open(sample, true), legacy -> open(legacy, true))));
    updateWindow();
  }

  @FXML
  private void onNew() {
    add(CharacterDocument.created(PlayerCharacter.createNew(AppInfo.version())));
  }

  @FXML
  private void onOpen() {
    List<File> chosen = fileChooser("Open character").showOpenMultipleDialog(stage);
    if (chosen == null) {
      return;
    }
    chosen.forEach(file -> open(file.toPath(), false));
  }

  /**
   * Loads a character file into a new tab, or selects its tab when it is already open. A legacy file opens unsaved
   * with its import report. A file from the Beta menu ({@code fromBeta}) opens as a copy without file path and
   * folder, so saving asks for a target, its folder (the test fixtures) is never proposed and never overwritten.
   */
  private void open(@NonNull Path path, boolean fromBeta) {
    Optional<OpenCharacter> already = fromBeta ? Optional.empty() : openWithFile(path, null);
    if (already.isPresent()) {
      documents.getSelectionModel().select(already.get().tab());
      return;
    }
    CharacterFile loaded;
    try {
      loaded = store.load(path);
    } catch (IOException | SaveFormatException e) {
      showError("The file could not be opened.", e.getMessage());
      return;
    }
    if (!fromBeta) {
      suggestedDirectory = path.getParent();
    }
    ImportReport report = loaded.importReport();
    if (report != null) {
      add(CharacterDocument.imported(loaded.character(), fromBeta ? null : path));
      if (stage != null) {
        ImportReportDialog.show(stage, path, report);
      }
    } else {
      add(fromBeta ? CharacterDocument.betaCopy(loaded.character())
          : CharacterDocument.loaded(loaded.character(), path));
    }
  }

  @FXML
  private void onSave() {
    if (compendiumSelected()) {
      onSaveParty();
    } else {
      current().ifPresent(character -> save(character, false));
    }
  }

  @FXML
  private void onSaveAs() {
    if (compendiumSelected()) {
      onSavePartyAs();
    } else {
      current().ifPresent(character -> save(character, true));
    }
  }

  /** Closes the selected tab – a character or the GM compendium – after asking about unsaved changes. */
  @FXML
  private void onClose() {
    if (compendiumSelected()) {
      compendium.close();
    } else {
      current().filter(this::confirmDiscard).ifPresent(character -> documents.getTabs().remove(character.tab()));
    }
  }

  /**
   * Writes the printable character sheet (E1/E2) as PDF; works in every mode and shows the current state,
   * unsaved changes included. The PDF goes next to the character file unless the user picks another place.
   */
  @FXML
  private void onExportSheet() {
    Optional<OpenCharacter> selected = current();
    if (selected.isEmpty()) {
      return;
    }
    PlayerCharacter character = selected.get().document().character();
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export character sheet");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));
    initialDirectory(chooser, selected.get().document());
    chooser.setInitialFileName(fileBaseName(character) + ".pdf");
    File chosen = chooser.showSaveDialog(stage);
    if (chosen == null) {
      return;
    }
    // Rendered in memory first, so a failed export leaves an existing file untouched.
    ByteArrayOutputStream pdf = new ByteArrayOutputStream();
    try {
      CharacterSheetPdf.write(SheetContent.of(character), pdf);
      Files.write(chosen.toPath(), pdf.toByteArray());
    } catch (IOException | RuntimeException e) {
      showError("The character sheet could not be exported.", e.getMessage());
    }
  }

  @FXML
  private void onExit() {
    if (confirmDiscardAll() && stage != null) {
      stage.close();
    }
  }

  /** Opens the GM compendium tab (H2), or selects it. */
  @FXML
  private void onCompendium() {
    compendium.show();
  }

  @FXML
  private void onNewParty() {
    compendium.newParty();
  }

  @FXML
  private void onOpenParty() {
    compendium.openParty();
  }

  @FXML
  private void onSaveParty() {
    compendium.showParty();
    compendium.save(false);
  }

  @FXML
  private void onSavePartyAs() {
    compendium.showParty();
    compendium.save(true);
  }

  @FXML
  private void onAbout() {
    AboutDialog.show(stage);
  }

  @FXML
  private void onCreateCharacter() {
    current().ifPresent(this::completeCreation);
  }

  /**
   * Switches GM free edit (D2) of the selected character on after a confirmation, or off; never saved (user
   * decision 2026-10-16).
   */
  @FXML
  private void onGmMode() {
    Optional<OpenCharacter> selected = current();
    if (selected.isEmpty()) {
      return;
    }
    boolean on = gmModeItem.isSelected();
    if (on && !ConfirmDialog.confirm(stage, "GM free edit", "Switch on GM free edit?",
        "Budgets and level-up points no longer limit changes, perk requirements are only hints, and every "
            + "change counts at once – also for a complete character (origin, traits, S.P.E.C.I.A.L., Tags, "
            + "skills, perks, pack, level and XP). For an imported character the imported values are not "
            + "recalculated; a new pack adds no inventory items once the old pack's items were added. GM free edit "
            + "is not saved.", "Switch on")) {
      gmModeItem.setSelected(false);
      return;
    }
    selected.get().document().character().setGmMode(on);
    refresh(selected.get());
  }

  /** Opens the character's rulebooks and house rules; OK with changes marks it unsaved and rebuilds the view. */
  @FXML
  private void onRules() {
    current().filter(character -> RulesDialog.show(stage, character.document().character()))
        .ifPresent(this::onChanged);
  }

  /**
   * Ends character creation (one-way, user decision) after the player confirms; open creation hints are
   * listed in the confirmation.
   */
  private void completeCreation(@NonNull OpenCharacter selected) {
    PlayerCharacter character = selected.document().character();
    if (character.isCreationComplete()) {
      return;
    }
    // The same hints as the header's counter (creation and apparel hints).
    List<CreationProblem> problems = CharacterView.hints(character);
    if (!CreateCharacterDialog.confirm(stage, problems)) {
      return;
    }
    character.completeCreation();
    onChanged(selected);
  }

  private void onCloseRequest(WindowEvent event) {
    if (!confirmDiscardAll()) {
      event.consume();
    }
  }

  /** Saves the character; returns whether it was written. */
  private boolean save(@NonNull OpenCharacter selected, boolean askForFile) {
    CharacterDocument document = selected.document();
    Path target = document.file();
    if (askForFile || target == null) {
      FileChooser chooser = fileChooser("Save character");
      initialDirectory(chooser, document);
      chooser.setInitialFileName(suggestedFileName(document));
      File chosen = chooser.showSaveDialog(stage);
      if (chosen == null) {
        return false;
      }
      target = chosen.toPath();
      if (openWithFile(target, selected).isPresent()) {
        showError("The character could not be saved.",
            "The file is open in another tab. Close that tab first or choose another file.");
        return false;
      }
    }
    try {
      store.save(document.character(), target);
    } catch (IOException e) {
      showError("The character could not be saved.", e.getMessage());
      return false;
    }
    document.markSaved(target);
    suggestedDirectory = target.getParent();
    updateTitles(selected);
    return true;
  }

  /** Asks for every character and the party with unsaved changes, in tab order; false as soon as one is cancelled. */
  private boolean confirmDiscardAll() {
    for (Tab tab : new ArrayList<>(documents.getTabs())) {
      if (compendium.isTab(tab) && !compendium.confirmDiscard()) {
        return false;
      }
      Optional<OpenCharacter> character = OpenCharacter.of(tab).filter(open -> open.document().isDirty());
      if (character.isPresent()) {
        documents.getSelectionModel().select(tab);
        if (!confirmDiscard(character.get())) {
          return false;
        }
      }
    }
    return true;
  }

  /** Returns true if there are no unsaved changes or the user saved or discarded them. */
  private boolean confirmDiscard(@NonNull OpenCharacter character) {
    if (!character.document().isDirty()) {
      return true;
    }
    return switch (SaveQuestion.ask(stage, character.document().displayName())) {
      case SAVE -> save(character, false);
      case DISCARD -> true;
      case CANCEL -> false;
    };
  }

  /** Shows the character in a new tab and selects it. */
  private void add(@NonNull CharacterDocument document) {
    Tab tab = new Tab();
    CharacterView view = new CharacterView(document.character(),
        new ViewCallbacks(() -> withCharacter(tab, this::onEdited), () -> withCharacter(tab, this::onChanged),
            () -> withCharacter(tab, this::completeCreation)), statusAsTabItem.isSelected(), statusButtonPosition);
    OpenCharacter character = OpenCharacter.attach(document, view, tab);
    tab.setContent(view.node());
    tab.setOnCloseRequest(event -> {
      if (!confirmDiscard(character)) {
        event.consume();
      }
    });
    tab.setText(document.title());
    documents.getTabs().add(tab);
    documents.getSelectionModel().select(tab);
    view.refresh();
  }

  /** Runs the action for the character of the tab while it is open (a late callback of a closed tab does nothing). */
  private void withCharacter(@NonNull Tab tab, @NonNull Consumer<OpenCharacter> action) {
    if (documents.getTabs().contains(tab)) {
      OpenCharacter.of(tab).ifPresent(action);
    }
  }

  /** Settings: the status as a tab or on the slide panel, for all open characters at once; remembered between runs. */
  @FXML
  private void onStatusAsTab() {
    boolean asTab = statusAsTabItem.isSelected();
    preferences.putBoolean(STATUS_AS_TAB_KEY, asTab);
    documents.getTabs().stream().map(OpenCharacter::of).flatMap(Optional::stream)
        .forEach(character -> character.view().setStatusAsTab(asTab));
  }

  /** Whether the GM compendium tab is selected. */
  private boolean compendiumSelected() {
    return compendium.isTab(documents.getSelectionModel().getSelectedItem());
  }

  /** The characters of the open tabs, in tab order. */
  private @NonNull List<PlayerCharacter> openCharacters() {
    return documents.getTabs().stream().map(OpenCharacter::of).flatMap(Optional::stream)
        .map(open -> open.document().character()).toList();
  }

  /** The character of the selected tab. */
  private @NonNull Optional<OpenCharacter> current() {
    Tab selected = documents.getSelectionModel().getSelectedItem();
    return selected == null ? Optional.empty() : OpenCharacter.of(selected);
  }

  /** The open character with this file, other than {@code except}. */
  private @NonNull Optional<OpenCharacter> openWithFile(@NonNull Path path, @Nullable OpenCharacter except) {
    return documents.getTabs().stream().map(OpenCharacter::of).flatMap(Optional::stream)
        .filter(character -> !character.equals(except) && character.document().isFile(path)).findFirst();
  }

  /** A creation edit changed the character: mark it unsaved and rebuild its view afterwards. */
  private void onChanged(@NonNull OpenCharacter character) {
    onEdited(character);
    Platform.runLater(() -> {
      if (documents.getTabs().contains(character.tab())) {
        refresh(character);
      }
    });
  }

  /** An edit that needs no rebuild (typed text): only mark the character unsaved. */
  private void onEdited(@NonNull OpenCharacter character) {
    character.document().markDirty();
    updateTitles(character);
  }

  private void refresh(@NonNull OpenCharacter character) {
    character.view().refresh();
    updateWindow();
  }

  private void updateTitles(@NonNull OpenCharacter character) {
    character.tab().setText(character.document().title());
    updateWindow();
  }

  /** Window title and menu state for the selected tab. */
  private void updateWindow() {
    Optional<OpenCharacter> selected = current();
    Optional<PlayerCharacter> character = selected.map(open -> open.document().character());
    boolean gm = compendiumSelected();
    for (MenuItem item : List.of(saveItem, saveAsItem, closeItem)) {
      item.setDisable(selected.isEmpty() && !gm);
    }
    for (MenuItem item : List.of(exportSheetItem, gmModeItem, rulesItem)) {
      item.setDisable(selected.isEmpty());
    }
    createCharacterItem.setDisable(character.map(PlayerCharacter::isCreationComplete).orElse(true));
    gmModeItem.setSelected(character.map(PlayerCharacter::isGmMode).orElse(false));
    if (stage != null) {
      String title = gm ? compendium.title() : selected.map(open -> open.document().title()).orElse(null);
      stage.setTitle(title == null ? APP_TITLE : APP_TITLE + " – " + title);
    }
  }

  private void showDocumentsOrEmptyState() {
    contentPane.getChildren().setAll(documents.getTabs().isEmpty() ? emptyState : documents);
  }

  /**
   * File name for the save dialog. For an imported character the dialog starts in the folder of the
   * legacy file; the name gets a {@code _v3} suffix so the legacy file is not proposed.
   */
  private static @NonNull String suggestedFileName(@NonNull CharacterDocument document) {
    String base = fileBaseName(document.character());
    return document.character().isLegacyImport() && document.file() == null ? base + "_v3.json" : base + ".json";
  }

  /** The character's name without characters that file systems reject, or "character". */
  private static @NonNull String fileBaseName(@NonNull PlayerCharacter current) {
    return current.getName().isBlank() ? "character" : current.getName().replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  /** Starts the dialog in the character's folder, else in the folder used last. */
  private void initialDirectory(@NonNull FileChooser chooser, @NonNull CharacterDocument document) {
    Path own = document.directory();
    Path directory = own != null ? own : suggestedDirectory;
    if (directory != null && directory.toFile().isDirectory()) {
      chooser.setInitialDirectory(directory.toFile());
    }
  }

  private @NonNull FileChooser fileChooser(@NonNull String title) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(title);
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Character files (*.json)", "*.json"));
    if (suggestedDirectory != null && suggestedDirectory.toFile().isDirectory()) {
      chooser.setInitialDirectory(suggestedDirectory.toFile());
    }
    return chooser;
  }

  private void showError(@NonNull String header, @Nullable String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "" : message);
    alert.initOwner(stage);
    alert.setHeaderText(header);
    alert.showAndWait();
  }
}
