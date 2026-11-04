package de.fallout2d20.charbuilder.view.compendium;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** The small controls the forms of the Scavenging section share. */
final class ScavengingControls {

  private static final double SMALL_FIELD_WIDTH = 50;

  private ScavengingControls() {}

  static @NonNull TextField smallField(int value) {
    TextField field = new TextField(Integer.toString(value));
    field.setPrefWidth(SMALL_FIELD_WIDTH);
    return field;
  }

  /** A secondary line of text. */
  static @NonNull Label muted(@NonNull String text) {
    Label label = new Label(text);
    label.setStyle("-fx-font-size: 90%; -fx-opacity: 0.75;");
    label.setWrapText(true);
    return label;
  }

  static void copyToClipboard(@NonNull String text) {
    ClipboardContent content = new ClipboardContent();
    content.putString(text);
    Clipboard.getSystemClipboard().setContent(content);
  }

  /**
   * The button, disabled with the reason as tooltip if there is one. The tooltip sits on a wrapper because a disabled
   * control shows none.
   */
  static @NonNull Node blockable(@NonNull Button button, @Nullable String reason) {
    button.setDisable(reason != null);
    HBox wrapper = new HBox(button);
    if (reason != null) {
      Tooltip.install(wrapper, new Tooltip(reason));
    }
    return wrapper;
  }
}
