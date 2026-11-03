package de.fallout2d20.charbuilder.view.compendium;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A table cell of a compendium entry (H2).
 *
 * @param text what the cell shows
 * @param number the sort key of a number column; null in text columns
 */
record Cell(@NonNull String text, @Nullable Integer number) {

  static @NonNull Cell text(@NonNull String text) {
    return new Cell(text, null);
  }

  /** A number column cell; a missing value shows a dash and sorts first. */
  static @NonNull Cell number(@Nullable Integer value) {
    return value == null ? new Cell("–", Integer.MIN_VALUE) : new Cell(Integer.toString(value), value);
  }

  /** A number column cell with its own text, e.g. "4 CD" sorted by 4. */
  static @NonNull Cell number(int value, @NonNull String text) {
    return new Cell(text, value);
  }

  int sortKey() {
    return number == null ? 0 : number;
  }
}
