package de.fallout2d20.charbuilder.view;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.jspecify.annotations.NonNull;

/**
 * Where a floating button sits on its edge, shared by all views showing it and remembered between runs: 0 is the
 * bottom, 1 the top of the range it can move in, so the position fits any window height.
 */
final class FloatingButtonPosition {

  private final @NonNull ViewPreferences preferences;
  private final @NonNull String key;
  private final @NonNull DoubleProperty value;

  FloatingButtonPosition(@NonNull ViewPreferences preferences, @NonNull String key) {
    this.preferences = preferences;
    this.key = key;
    value = new SimpleDoubleProperty(Math.clamp(preferences.getDouble(key, 0), 0.0, 1.0));
  }

  @NonNull DoubleProperty value() {
    return value;
  }

  /** Remembers the current position; once per move, not on every step of the drag. */
  void save() {
    preferences.putDouble(key, value.get());
  }

  /**
   * The position after dragging.
   *
   * @param start the position when the drag began
   * @param deltaY the distance dragged, downwards positive
   * @param range the pixels between the lowest and the highest position
   * @return the new position, within 0 and 1; unchanged if the button has no room to move
   */
  static double afterDrag(double start, double deltaY, double range) {
    return range <= 0 ? start : Math.clamp(start - deltaY / range, 0.0, 1.0);
  }
}
