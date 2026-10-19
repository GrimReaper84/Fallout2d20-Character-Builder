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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.jspecify.annotations.Nullable;

/**
 * Main window: file menu and character overview.
 *
 * <p>A character imported from a legacy file has no file path, so its first save always asks for
 * a new file; the legacy file is never overwritten.
 */
public class MainWindowController {

  private static final String APP_TITLE = AppInfo.NAME;

  private final CharacterStore store = new CharacterStore();

  @FXML private MenuBar menuBar;
  @FXML private StackPane contentPane;
  @FXML private MenuItem createCharacterItem;
  @FXML private MenuItem exportSheetItem;
  @FXML private CheckMenuItem gmModeItem;

  private @Nullable Stage stage;
  private @Nullable PlayerCharacter character;
  private @Nullable CharacterView view;
  private @Nullable Path file;
  private @Nullable Path suggestedDirectory;
  private boolean dirty;
  /** Whether the character was opened from the Beta menu and has not been saved yet (E9). */
  private boolean betaSample;

  @FXML
  private void initialize() {
    showEmptyState();
  }

  /** Connects the controller to its window; asks before closing with unsaved changes. */
  public void attach(Stage stage) {
    this.stage = stage;
    stage.setOnCloseRequest(this::onCloseRequest);
    BetaMenu.folder().ifPresent(folder -> menuBar.getMenus().add(
        BetaMenu.create(folder, sample -> openFromBeta(sample, true), legacy -> openFromBeta(legacy, false))));
    updateTitle();
  }

  @FXML
  private void onNew() {
    if (!confirmDiscard()) {
      return;
    }
    show(PlayerCharacter.createNew(AppInfo.version()), null);
  }

  @FXML
  private void onOpen() {
    if (!confirmDiscard()) {
      return;
    }
    File chosen = fileChooser("Open character").showOpenDialog(stage);
    if (chosen == null) {
      return;
    }
    open(chosen.toPath(), false, false);
  }

  /** Opens a file from the Beta menu (E9); a sample opens as an unsaved copy, a legacy file as usual. */
  private void openFromBeta(Path path, boolean asCopy) {
    if (confirmDiscard()) {
      open(path, true, asCopy);
    }
  }

  /**
   * Loads a character file and shows it. A legacy file opens unsaved with its import report. With
   * {@code fromBeta} the folder is never proposed for later saves (it holds the test fixtures), and a sample
   * opens as a copy without a file path, so saving asks for a target and the sample is never overwritten.
   */
  private void open(Path path, boolean fromBeta, boolean asCopy) {
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
      show(loaded.character(), null);
      dirty = true;
      updateTitle();
      if (stage != null) {
        ImportReportDialog.show(stage, path, report);
      }
    } else {
      show(loaded.character(), asCopy ? null : path);
      betaSample = asCopy;
      updateTitle();
    }
  }

  @FXML
  private void onSave() {
    save(false);
  }

  @FXML
  private void onSaveAs() {
    save(true);
  }

  /**
   * Writes the printable character sheet (E1/E2) as PDF; works in every mode and shows the current state,
   * unsaved changes included. The PDF goes next to the character file unless the user picks another place.
   */
  @FXML
  private void onExportSheet() {
    if (character == null) {
      return;
    }
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export character sheet");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));
    if (suggestedDirectory != null && suggestedDirectory.toFile().isDirectory()) {
      chooser.setInitialDirectory(suggestedDirectory.toFile());
    }
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
    if (confirmDiscard() && stage != null) {
      stage.close();
    }
  }

  @FXML
  private void onAbout() {
    AboutDialog.show(stage);
  }

  @FXML
  private void onCreateCharacter() {
    completeCreation();
  }

  /**
   * Switches GM free edit (D2) on after a confirmation, or off; never saved (user decision 2026-10-16).
   */
  @FXML
  private void onGmMode() {
    if (character == null) {
      return;
    }
    boolean on = gmModeItem.isSelected();
    if (on && !ConfirmDialog.confirm(stage, "GM mode", "Switch on GM mode?",
        "Budgets and level-up points no longer limit changes, perk requirements are only hints, and every "
            + "change counts at once – also for a complete character (origin, traits, S.P.E.C.I.A.L., Tags, "
            + "skills, perks, pack, level and XP). For an imported character the imported values are not "
            + "recalculated; a new pack adds no inventory items once the old pack's items were added. GM mode is "
            + "not saved.", "Switch on")) {
      gmModeItem.setSelected(false);
      return;
    }
    character.setGmMode(on);
    showOverview();
  }

  /**
   * Ends character creation (one-way, user decision) after the player confirms; open creation hints are
   * listed in the confirmation.
   */
  private void completeCreation() {
    if (character == null || character.isCreationComplete()) {
      return;
    }
    // The same hints as the header's counter (creation and apparel hints).
    List<CreationProblem> problems = CharacterView.hints(character);
    if (!CreateCharacterDialog.confirm(stage, problems)) {
      return;
    }
    character.completeCreation();
    onChanged();
  }

  private void onCloseRequest(WindowEvent event) {
    if (!confirmDiscard()) {
      event.consume();
    }
  }

  /** Saves the character; returns whether it was written. */
  private boolean save(boolean askForFile) {
    if (character == null) {
      return false;
    }
    Path target = file;
    if (askForFile || target == null) {
      FileChooser chooser = fileChooser("Save character");
      chooser.setInitialFileName(suggestedFileName(character));
      File chosen = chooser.showSaveDialog(stage);
      if (chosen == null) {
        return false;
      }
      target = chosen.toPath();
    }
    try {
      store.save(character, target);
    } catch (IOException e) {
      showError("The character could not be saved.", e.getMessage());
      return false;
    }
    file = target;
    suggestedDirectory = target.getParent();
    dirty = false;
    betaSample = false;
    updateTitle();
    return true;
  }

  /** Returns true if there are no unsaved changes or the user saved or discarded them. */
  private boolean confirmDiscard() {
    if (!dirty || character == null) {
      return true;
    }
    ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.YES);
    ButtonType discardButton = new ButtonType("Discard", ButtonBar.ButtonData.NO);
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
        "The character has unsaved changes.", saveButton, discardButton, ButtonType.CANCEL);
    alert.initOwner(stage);
    alert.setHeaderText("Save changes?");
    Optional<ButtonType> answer = alert.showAndWait();
    if (answer.isEmpty() || answer.get() == ButtonType.CANCEL) {
      return false;
    }
    return answer.get() == discardButton || save(false);
  }

  private void show(PlayerCharacter newCharacter, @Nullable Path newFile) {
    character = newCharacter;
    file = newFile;
    dirty = false;
    betaSample = false;
    CharacterView newView = new CharacterView(newCharacter,
        new ViewCallbacks(this::onEdited, this::onChanged, this::completeCreation));
    view = newView;
    contentPane.getChildren().setAll(newView.node());
    showOverview();
    updateTitle();
  }

  private void showOverview() {
    if (character == null) {
      return;
    }
    createCharacterItem.setDisable(character.isCreationComplete());
    exportSheetItem.setDisable(false);
    gmModeItem.setDisable(false);
    gmModeItem.setSelected(character.isGmMode());
    if (view != null) {
      view.refresh();
    }
  }

  /** A creation edit changed the character: mark it unsaved and rebuild the overview afterwards. */
  private void onChanged() {
    dirty = true;
    updateTitle();
    Platform.runLater(this::showOverview);
  }

  /** An edit that needs no rebuild (typed text): only mark the character unsaved. */
  private void onEdited() {
    dirty = true;
    updateTitle();
  }

  private void showEmptyState() {
    Label label = new Label("No character loaded. Use File → New or File → Open.");
    label.setStyle("-fx-padding: 24;");
    contentPane.getChildren().setAll(label);
  }

  private void updateTitle() {
    if (stage == null) {
      return;
    }
    if (character == null) {
      stage.setTitle(APP_TITLE);
      return;
    }
    String name = character.getName().isBlank() ? "Unnamed" : character.getName();
    stage.setTitle(APP_TITLE + " – " + name + (betaSample ? " (beta sample)" : "") + (dirty ? " *" : ""));
  }

  /**
   * File name for the save dialog. For an imported character the dialog starts in the folder of the
   * legacy file; the name gets a {@code _v3} suffix so the legacy file is not proposed.
   */
  private String suggestedFileName(PlayerCharacter current) {
    String base = fileBaseName(current);
    return current.isLegacyImport() && file == null ? base + "_v3.json" : base + ".json";
  }

  /** The character's name without characters that file systems reject, or "character". */
  private static String fileBaseName(PlayerCharacter current) {
    return current.getName().isBlank() ? "character" : current.getName().replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  private FileChooser fileChooser(String title) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(title);
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Character files (*.json)", "*.json"));
    if (suggestedDirectory != null && suggestedDirectory.toFile().isDirectory()) {
      chooser.setInitialDirectory(suggestedDirectory.toFile());
    }
    return chooser;
  }

  private void showError(String header, @Nullable String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "" : message);
    alert.initOwner(stage);
    alert.setHeaderText(header);
    alert.showAndWait();
  }
}
