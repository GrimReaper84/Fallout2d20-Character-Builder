package de.fallout2d20.charbuilder.view;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Screen;
import org.jspecify.annotations.NonNull;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * The info button of a perk card: hovering it shows a floating panel above the card with the perk's
 * description (user decision 2026-10-17). The panel is a popup window of its own, so the perk grid below
 * never moves. It sits above the card (below it if there is no room on the screen) and never covers the
 * button, so showing it cannot end the hover; it is hidden when the mouse leaves the button or the button
 * leaves the scene (the view is rebuilt after every change).
 */
final class PerkInfo {

  private static final double PANEL_WIDTH = 300;
  private static final double GAP = 6;
  private static final @NonNull String PANEL_STYLE = "-fx-background-color: #fdfdfd; -fx-border-color: #9a9a9a; "
      + "-fx-border-radius: 4; -fx-background-radius: 4;";

  private PerkInfo() {}

  /** The info icon; {@link #attach} connects it to its card and text. */
  static @NonNull StackPane button() {
    StackPane button = new StackPane(new FontIcon("fas-info-circle"));
    // The whole square reacts to the mouse, not only the glyph's outline.
    button.setPickOnBounds(true);
    return button;
  }

  /** Shows {@code text} above {@code card} while the mouse is over the button. */
  static void attach(@NonNull Node button, @NonNull Node card, @NonNull String text) {
    Popup[] panel = new Popup[1];
    button.setOnMouseEntered(event -> {
      if (panel[0] == null) {
        panel[0] = panel(text);
      }
      show(panel[0], button, card);
    });
    button.setOnMouseExited(event -> {
      if (panel[0] != null) {
        panel[0].hide();
      }
    });
    button.sceneProperty().addListener((obs, old, scene) -> {
      if (scene == null && panel[0] != null) {
        panel[0].hide();
      }
    });
  }

  private static @NonNull Popup panel(@NonNull String text) {
    Label label = new Label(text);
    label.setWrapText(true);
    label.setMinWidth(PANEL_WIDTH);
    label.setPrefWidth(PANEL_WIDTH);
    label.setMaxWidth(PANEL_WIDTH);
    VBox content = new VBox(label);
    content.setPadding(new Insets(8));
    content.setStyle(PANEL_STYLE);
    content.setEffect(new DropShadow(8, Color.gray(0, 0.3)));
    content.setMouseTransparent(true);
    Popup popup = new Popup();
    popup.getContent().add(content);
    popup.setAutoFix(false);
    return popup;
  }

  /**
   * Shows the panel above the card. Its size is computed before it is shown, so it never appears over the
   * button for a moment (that would end the hover and hide it again).
   */
  private static void show(@NonNull Popup panel, @NonNull Node button, @NonNull Node card) {
    Bounds bounds = card.localToScreen(card.getBoundsInLocal());
    if (bounds == null || button.getScene() == null) {
      return;
    }
    Region content = (Region) panel.getContent().getFirst();
    content.applyCss();
    double width = content.prefWidth(-1);
    double height = content.prefHeight(width);
    Rectangle2D screen = Screen.getScreensForRectangle(bounds.getMinX(), bounds.getMinY(), 1, 1).stream()
        .findFirst().orElse(Screen.getPrimary()).getVisualBounds();
    double above = bounds.getMinY() - height - GAP;
    double y = above >= screen.getMinY() ? above : bounds.getMaxY() + GAP;
    double x = Math.max(screen.getMinX(), Math.min(bounds.getMinX(), screen.getMaxX() - width));
    panel.show(button, x, y);
  }
}
