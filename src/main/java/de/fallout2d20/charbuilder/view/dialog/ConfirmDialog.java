package de.fallout2d20.charbuilder.view.dialog;

import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A yes/cancel question before a step that cannot be undone. Cancel is the default button, so pressing
 * Enter changes nothing by accident.
 */
public final class ConfirmDialog {

  private ConfirmDialog() {}

  /** Returns whether the user chose the action. */
  public static boolean confirm(@Nullable Window owner, @NonNull String title, @NonNull String header,
      @NonNull String text, @NonNull String action) {
    ButtonType ok = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ok, ButtonType.CANCEL);
    alert.initOwner(owner);
    alert.setTitle(title);
    alert.setHeaderText(header);
    ((Button) alert.getDialogPane().lookupButton(ok)).setDefaultButton(false);
    ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);
    Optional<ButtonType> answer = alert.showAndWait();
    return answer.isPresent() && answer.get() == ok;
  }
}
