package de.fallout2d20.charbuilder.view;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.jspecify.annotations.NonNull;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * The Remove button above the tree of a master/detail tab. Its tooltip sits on a wrapper, so the reason also
 * shows while the button is disabled; it is updated whenever the selection changes.
 */
final class RemoveButton {

  private final @NonNull Button button = new Button("Remove");
  private final @NonNull Tooltip tooltip = new Tooltip();
  private final @NonNull HBox node = new HBox(button);

  RemoveButton(@NonNull Runnable onRemove) {
    button.setGraphic(new FontIcon("fas-trash"));
    button.setOnAction(_ -> onRemove.run());
    Tooltip.install(node, tooltip);
  }

  /** The wrapper to add to the layout. */
  @NonNull HBox node() {
    return node;
  }

  /** Enables or disables the button; {@code text} names what it removes or why it cannot. */
  void update(boolean enabled, @NonNull String text) {
    button.setDisable(!enabled);
    tooltip.setText(text);
  }
}
