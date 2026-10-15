package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.persistence.CharacterFile;
import de.fallout2d20.charbuilder.persistence.CharacterStore;
import de.fallout2d20.charbuilder.persistence.SaveFormatException;
import de.fallout2d20.charbuilder.persistence.legacy.ImportReport;
import de.fallout2d20.charbuilder.view.dialog.ImportReportDialog;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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

  private static final String APP_TITLE = "Fallout 2d20 Character Builder";

  private final CharacterStore store = new CharacterStore();

  @FXML private ScrollPane overviewPane;

  private @Nullable Stage stage;
  private @Nullable Character character;
  private @Nullable Path file;
  private @Nullable Path suggestedDirectory;
  private boolean dirty;

  @FXML
  private void initialize() {
    showEmptyState();
  }

  /** Connects the controller to its window; asks before closing with unsaved changes. */
  public void attach(Stage stage) {
    this.stage = stage;
    stage.setOnCloseRequest(this::onCloseRequest);
    updateTitle();
  }

  @FXML
  private void onNew() {
    if (!confirmDiscard()) {
      return;
    }
    show(Character.createNew(), null);
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
    Path path = chosen.toPath();
    CharacterFile loaded;
    try {
      loaded = store.load(path);
    } catch (IOException | SaveFormatException e) {
      showError("The file could not be opened.", e.getMessage());
      return;
    }
    suggestedDirectory = path.getParent();
    ImportReport report = loaded.importReport();
    if (report != null) {
      show(loaded.character(), null);
      dirty = true;
      updateTitle();
      if (stage != null) {
        ImportReportDialog.show(stage, path, report);
      }
    } else {
      show(loaded.character(), path);
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

  @FXML
  private void onExit() {
    if (confirmDiscard() && stage != null) {
      stage.close();
    }
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

  private void show(Character newCharacter, @Nullable Path newFile) {
    character = newCharacter;
    file = newFile;
    dirty = false;
    showOverview();
    updateTitle();
  }

  private void showOverview() {
    if (character == null) {
      return;
    }
    overviewPane.setContent(CharacterOverview.build(character, this::onEdited, this::onChanged));
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
    overviewPane.setContent(label);
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
    stage.setTitle(APP_TITLE + " – " + name + (dirty ? " *" : ""));
  }

  /**
   * File name for the save dialog. For an imported character the dialog starts in the folder of the
   * legacy file; the name gets a {@code _v3} suffix so the legacy file is not proposed.
   */
  private String suggestedFileName(Character current) {
    String base = current.getName().isBlank() ? "character" : current.getName().replaceAll("[\\\\/:*?\"<>|]", "_");
    return current.isLegacyImport() && file == null ? base + "_v3.json" : base + ".json";
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
