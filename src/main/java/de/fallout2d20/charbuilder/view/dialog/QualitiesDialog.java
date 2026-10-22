package de.fallout2d20.charbuilder.view.dialog;

import java.util.Optional;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Changes the qualities of a custom weapon ({@link QualityCheckBoxes}). */
public final class QualitiesDialog {

  private QualitiesDialog() {}

  /** Shows the dialog with the current qualities ticked; the new quality text, empty if cancelled. */
  public static @NonNull Optional<String> show(@Nullable Window owner, @NonNull String weaponName,
      @NonNull String current) {
    QualityCheckBoxes boxes = new QualityCheckBoxes(current);
    Dialog<String> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle("Qualities");
    dialog.setHeaderText("Which qualities does " + weaponName + " have?");
    dialog.getDialogPane().setContent(boxes.node());
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dialog.setResultConverter(button -> button == ButtonType.OK ? boxes.text() : null);
    return dialog.showAndWait();
  }
}
