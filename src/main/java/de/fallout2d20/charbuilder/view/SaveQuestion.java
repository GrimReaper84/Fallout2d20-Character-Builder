package de.fallout2d20.charbuilder.view;

import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** The answer to "Save changes?" before something unsaved is closed (a character or the GM's party). */
enum SaveQuestion {
  SAVE,
  DISCARD,
  CANCEL;

  /** Asks whether to save the changes of the named document. */
  static @NonNull SaveQuestion ask(@Nullable Window owner, @NonNull String name) {
    ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.YES);
    ButtonType discardButton = new ButtonType("Discard", ButtonBar.ButtonData.NO);
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "\"" + name + "\" has unsaved changes.", saveButton,
        discardButton, ButtonType.CANCEL);
    alert.initOwner(owner);
    alert.setHeaderText("Save changes?");
    Optional<ButtonType> answer = alert.showAndWait();
    if (answer.isEmpty() || answer.get() == ButtonType.CANCEL) {
      return CANCEL;
    }
    return answer.get() == discardButton ? DISCARD : SAVE;
  }
}
