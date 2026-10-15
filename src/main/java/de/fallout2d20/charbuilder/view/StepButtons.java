package de.fallout2d20.charbuilder.view;

import javafx.scene.control.Button;
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
