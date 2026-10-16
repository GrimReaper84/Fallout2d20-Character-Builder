package de.fallout2d20.charbuilder.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.jspecify.annotations.NonNull;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * The −/+ icon buttons that lower or raise a value by one. The tooltip sits on a wrapper because
 * disabled buttons show none; for a disabled button it names the reason.
 */
final class StepButtons {

  private StepButtons() {}

  /** Minimum width of a single number next to its buttons (up to two digits). */
  static final double NUMBER_WIDTH = 22;
  /** Minimum width of a value such as "13 / 13" next to its buttons. */
  static final double FRACTION_WIDTH = 56;

  /**
   * A value with its −/+ buttons directly to the right. The value keeps at least {@code valueWidth}, so the
   * buttons do not move when it grows (9 → 10); a row without buttons shows the value in the same place.
   */
  static @NonNull HBox withValue(@NonNull Label value, double valueWidth, @NonNull HBox buttons) {
    value.setMinWidth(valueWidth);
    HBox box = new HBox(8, value, buttons);
    box.setAlignment(Pos.CENTER_LEFT);
    return box;
  }

  /** A − and a + button side by side. */
  static @NonNull HBox of(@NonNull HBox minus, @NonNull HBox plus) {
    return new HBox(4, minus, plus);
  }

  static @NonNull HBox minus(@NonNull String tooltip, boolean enabled, @NonNull Runnable action) {
    return button("fas-minus", tooltip, enabled, action);
  }

  static @NonNull HBox plus(@NonNull String tooltip, boolean enabled, @NonNull Runnable action) {
    return button("fas-plus", tooltip, enabled, action);
  }

  private static @NonNull HBox button(@NonNull String icon, @NonNull String tooltip, boolean enabled,
      @NonNull Runnable action) {
    Button button = new Button();
    button.setGraphic(new FontIcon(icon));
    button.setDisable(!enabled);
    button.setOnAction(event -> action.run());
    HBox wrapper = new HBox(button);
    Tooltip.install(wrapper, new Tooltip(tooltip));
    return wrapper;
  }
}
