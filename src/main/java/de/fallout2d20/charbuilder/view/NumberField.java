package de.fallout2d20.charbuilder.view;

import java.util.OptionalInt;
import java.util.function.IntConsumer;
import javafx.scene.control.TextField;
import org.jspecify.annotations.NonNull;

/**
 * A number field that takes its value on Enter or when it loses the focus, once: the commit rebuilds the
 * overview, and the old field must not commit again when it loses the focus on removal. Anything but a
 * whole number of at least the minimum resets it.
 */
final class NumberField {

  private static final double WIDTH = 60;

  private NumberField() {}

  static @NonNull TextField of(int value, int minimum, @NonNull IntConsumer commit) {
    TextField field = new TextField(Integer.toString(value));
    field.setPrefWidth(WIDTH);
    Runnable take = () -> {
      if (field.getUserData() != null) {
        return;
      }
      OptionalInt typed = WholeNumber.parse(field.getText());
      if (typed.isPresent() && typed.getAsInt() >= minimum) {
        if (typed.getAsInt() != value) {
          field.setUserData(true);
          commit.accept(typed.getAsInt());
        }
      } else {
        field.setText(Integer.toString(value));
      }
    };
    field.setOnAction(_ -> take.run());
    field.focusedProperty().addListener((_, _, focused) -> {
      if (!focused) {
        take.run();
      }
    });
    return field;
  }
}
