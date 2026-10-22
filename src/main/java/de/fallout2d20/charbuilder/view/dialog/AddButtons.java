package de.fallout2d20.charbuilder.view.dialog;

import java.util.function.Supplier;
import javafx.beans.binding.BooleanExpression;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The buttons of an add dialog: "Add" enters the selection and keeps the dialog open for the next one,
 * "Add & close" enters it and closes, "Close" closes. Every add goes to the character at once (user
 * decision 2026-10-20), so closing never takes an add back; the status label names the last one.
 */
final class AddButtons {

  private static final @NonNull ButtonType ADD = new ButtonType("Add", ButtonData.OTHER);
  private static final @NonNull ButtonType ADD_AND_CLOSE = new ButtonType("Add & close", ButtonData.OK_DONE);

  private AddButtons() {}

  /**
   * Puts the buttons into the dialog.
   *
   * @param disabled when nothing can be added (no selection, incomplete custom form)
   * @param add enters the selection; returns the text for the status line, {@code null} if nothing was added
   *     (then "Add & close" keeps the dialog open)
   * @return the status line to place in the dialog's content
   */
  static @NonNull Label install(@NonNull Dialog<ButtonType> dialog, @NonNull BooleanExpression disabled,
      @NonNull Supplier<@Nullable String> add) {
    Label status = new Label();
    dialog.getDialogPane().getButtonTypes().setAll(ADD, ADD_AND_CLOSE, ButtonType.CLOSE);
    Node addButton = dialog.getDialogPane().lookupButton(ADD);
    Node closeAfter = dialog.getDialogPane().lookupButton(ADD_AND_CLOSE);
    addButton.disableProperty().bind(disabled);
    closeAfter.disableProperty().bind(disabled);
    addButton.addEventFilter(ActionEvent.ACTION, event -> {
      event.consume();
      String added = add.get();
      if (added != null) {
        status.setText("Added: " + added);
      }
    });
    closeAfter.addEventFilter(ActionEvent.ACTION, event -> {
      if (add.get() == null) {
        event.consume();
      }
    });
    return status;
  }

  /** Adds like "Add" (e.g. on a double click in the catalog table) while the buttons are enabled. */
  static void addNow(@NonNull Dialog<ButtonType> dialog) {
    if (dialog.getDialogPane().lookupButton(ADD) instanceof Button addButton && !addButton.isDisabled()) {
      addButton.fire();
    }
  }
}
