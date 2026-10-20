package de.fallout2d20.charbuilder.view;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import org.jspecify.annotations.NonNull;

/**
 * The layout of a master/detail tab (Weapons, Armor): a fixed-width left side with the tree and a right side
 * with the details that takes the rest of the width. Each side scrolls on its own and keeps its position
 * across rebuilds ({@link MasterDetailState}); the content keeps some space to the scroll bar.
 */
final class MasterDetail {

  private static final double LEFT_WIDTH = 300;
  private static final double PADDING = 16;
  private static final double COLUMN_GAP = 32;
  /** Space between a side's content and its scroll bar. */
  private static final double SCROLL_BAR_GAP = 12;

  private MasterDetail() {}

  /** The left side: fixed width, its content as high as the tab (the tree fills it). */
  static @NonNull ScrollPane left(@NonNull Node content, @NonNull MasterDetailState state) {
    ScrollPane scroll = scrollPane(content, state::listScroll, state::setListScroll);
    scroll.setFitToHeight(true);
    scroll.setMinWidth(LEFT_WIDTH);
    scroll.setPrefWidth(LEFT_WIDTH);
    scroll.setMaxWidth(LEFT_WIDTH);
    return scroll;
  }

  /** The right side with the details. */
  static @NonNull ScrollPane right(@NonNull Node content, @NonNull MasterDetailState state) {
    return scrollPane(content, state::detailsScroll, state::setDetailsScroll);
  }

  /** Shows other details on the right side (another entry was selected), scrolled to the top. */
  static void replace(@NonNull ScrollPane right, @NonNull Node content) {
    right.setContent(padded(content));
    right.setVvalue(0);
  }

  /** Both sides next to each other; a border pane gives the right side all the width the left one leaves. */
  static @NonNull BorderPane layout(@NonNull ScrollPane left, @NonNull ScrollPane right) {
    BorderPane pane = new BorderPane(right);
    pane.setLeft(left);
    pane.setPadding(new Insets(PADDING));
    BorderPane.setMargin(right, new Insets(0, 0, 0, COLUMN_GAP));
    return pane;
  }

  /**
   * A scroll pane filling its width that keeps its position across rebuilds: restored after layout, recorded
   * only while it is shown (a pane leaving the scene must not overwrite the position).
   */
  private static @NonNull ScrollPane scrollPane(@NonNull Node content, @NonNull DoubleSupplier saved,
      @NonNull DoubleConsumer save) {
    ScrollPane scroll = new ScrollPane(padded(content));
    scroll.setFitToWidth(true);
    scroll.setStyle("-fx-background-color: transparent;");
    scroll.vvalueProperty().addListener((_, _, value) -> {
      if (scroll.getScene() != null) {
        save.accept(value.doubleValue());
      }
    });
    double position = saved.getAsDouble();
    Platform.runLater(() -> scroll.setVvalue(position));
    return scroll;
  }

  /** The content with some space to the scroll bar on its right, so it does not stick to it. */
  private static @NonNull Node padded(@NonNull Node content) {
    BorderPane pane = new BorderPane(content);
    pane.setPadding(new Insets(0, SCROLL_BAR_GAP, 0, 0));
    return pane;
  }
}
