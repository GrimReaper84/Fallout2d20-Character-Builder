package de.fallout2d20.charbuilder.view;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.jspecify.annotations.NonNull;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A panel that slides in from the right over a base node, opened by a floating button. It covers the right third of
 * the host, at least the content's width. While it is open, a transparent scrim lies over the base node: a click on it
 * only closes the panel and reaches nothing below. The scrim covers the base node only, so the menu bar and the tabs of
 * other characters stay usable and leave the panel open. The button can be dragged up and down along the right edge.
 */
final class SlidePanel {

  private static final @NonNull Duration SLIDE_TIME = Duration.millis(180);
  private static final double HOST_SHARE = 1.0 / 3;
  /** Room for the panel's scroll bar and padding beside the content. */
  private static final double CONTENT_EXTRA = 40;
  private static final double BUTTON_MARGIN = 16;
  private static final int BUTTON_SIZE = 63;
  private static final int ICON_SIZE = 27;
  private static final double SHADOW_RADIUS = 12;
  private static final double SHADOW_OPACITY = 0.4;
  /** Below this distance a press and release still counts as a click. */
  private static final double DRAG_THRESHOLD = 5;

  private final @NonNull ScrollPane panel = new ScrollPane();
  private final @NonNull Button button = new Button();
  private final @NonNull Pane scrim = new Pane();
  private final @NonNull StackPane host;
  private final @NonNull TranslateTransition slide = new TranslateTransition(SLIDE_TIME, panel);
  private boolean available = true;
  /** The press of a possible drag of the button. */
  private double dragStartSceneY;
  private double dragStartPosition;
  /** Whether the current press became a drag, which must not open the panel. */
  private boolean dragged;

  /**
   * Creates the closed panel over the base node.
   *
   * @param iconLiteral the Ikonli icon of the floating button
   * @param tooltip the button's tooltip
   * @param position where the button sits on the right edge
   */
  SlidePanel(@NonNull Node base, @NonNull String iconLiteral, @NonNull String tooltip,
      @NonNull FloatingButtonPosition position) {
    FontIcon icon = new FontIcon(iconLiteral);
    icon.setIconSize(ICON_SIZE);
    button.setGraphic(icon);
    button.setTooltip(new Tooltip(tooltip));
    button.setStyle("-fx-background-radius: " + BUTTON_SIZE / 2 + ";");
    button.setMinSize(BUTTON_SIZE, BUTTON_SIZE);
    button.setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
    button.setMaxSize(BUTTON_SIZE, BUTTON_SIZE);
    button.setEffect(shadow());
    button.setOnAction(_ -> {
      if (!dragged) {
        open();
      }
    });
    scrim.setPickOnBounds(true);
    scrim.setVisible(false);
    scrim.setOnMousePressed(event -> {
      close();
      event.consume();
    });
    panel.setFitToWidth(true);
    panel.setEffect(shadow());
    panel.setVisible(false);
    host = new StackPane(base, button, scrim, panel);
    StackPane.setAlignment(button, Pos.BOTTOM_RIGHT);
    StackPane.setMargin(button, new Insets(BUTTON_MARGIN));
    StackPane.setAlignment(panel, Pos.CENTER_RIGHT);
    host.widthProperty().addListener((_, _, _) -> {
      if (panel.isVisible()) {
        fitWidth();
      }
    });
    // A binding listens weakly, so the shared position keeps no closed character's view alive.
    button.translateYProperty().bind(Bindings.createDoubleBinding(() -> -moveRange() * position.value().get(),
        host.heightProperty(), position.value()));
    makeDraggable(position);
  }

  private void makeDraggable(@NonNull FloatingButtonPosition position) {
    button.setOnMousePressed(event -> {
      dragStartSceneY = event.getSceneY();
      dragStartPosition = position.value().get();
      dragged = false;
    });
    button.setOnMouseDragged(event -> {
      double deltaY = event.getSceneY() - dragStartSceneY;
      if (dragged || Math.abs(deltaY) >= DRAG_THRESHOLD) {
        dragged = true;
        button.setCursor(Cursor.V_RESIZE);
        position.value().set(FloatingButtonPosition.afterDrag(dragStartPosition, deltaY, moveRange()));
      }
    });
    button.setOnMouseReleased(_ -> {
      if (dragged) {
        button.setCursor(null);
        position.save();
        // The button fires its action during this release; only afterwards it may open the panel again.
        Platform.runLater(() -> dragged = false);
      }
    });
  }

  /** The pixels between the button's lowest and highest position. */
  private double moveRange() {
    return Math.max(0, host.getHeight() - BUTTON_SIZE - 2 * BUTTON_MARGIN);
  }

  @NonNull Node node() {
    return host;
  }

  /** Replaces the panel's content; an open panel stays open at its scroll position. */
  void setContent(@NonNull Node content) {
    double position = panel.getVvalue();
    panel.setContent(content);
    Platform.runLater(() -> panel.setVvalue(position));
    if (panel.isVisible()) {
      fitWidth();
    }
  }

  /** Whether the panel can be used; an unavailable one is closed and has no button. */
  void setAvailable(boolean value) {
    available = value;
    if (!value) {
      slide.stop();
      showClosed();
    }
    button.setVisible(value && !panel.isVisible());
  }

  /** The panel's width: its share of the host, at least the content's width, never beyond the host. */
  static double width(double hostWidth, double contentWidth) {
    return Math.min(hostWidth, Math.max(hostWidth * HOST_SHARE, contentWidth + CONTENT_EXTRA));
  }

  private void open() {
    slide.stop();
    slide.setOnFinished(null);
    button.setVisible(false);
    scrim.setVisible(true);
    panel.setVisible(true);
    fitWidth();
    panel.setTranslateX(panel.getPrefWidth());
    slide.setToX(0);
    slide.playFromStart();
  }

  /** Slides out; scrim and button change only at the end, so nothing below takes the rest of the click. */
  private void close() {
    slide.stop();
    slide.setToX(panel.getPrefWidth());
    slide.setOnFinished(_ -> showClosed());
    slide.playFromStart();
  }

  private void showClosed() {
    panel.setVisible(false);
    scrim.setVisible(false);
    button.setVisible(available);
  }

  private void fitWidth() {
    double contentWidth = 0;
    Node content = panel.getContent();
    if (content != null) {
      content.applyCss();
      contentWidth = content.prefWidth(-1);
    }
    double width = width(host.getWidth(), contentWidth);
    panel.setPrefWidth(width);
    panel.setMaxWidth(width);
  }

  private static @NonNull DropShadow shadow() {
    return new DropShadow(SHADOW_RADIUS, Color.gray(0, SHADOW_OPACITY));
  }
}
