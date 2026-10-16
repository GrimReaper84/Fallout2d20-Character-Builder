package de.fallout2d20.charbuilder.export.sheet;

import org.jspecify.annotations.NonNull;

/**
 * A table column of the sheet: header text, relative width (the table scales the weights to its width)
 * and how the cells are shown.
 */
record Column(@NonNull String header, float weight, @NonNull ColumnKind kind) {

  static @NonNull Column text(@NonNull String header, float weight) {
    return new Column(header, weight, ColumnKind.TEXT);
  }

  static @NonNull Column centered(@NonNull String header, float weight) {
    return new Column(header, weight, ColumnKind.CENTERED);
  }

  static @NonNull Column checkbox(@NonNull String header, float weight) {
    return new Column(header, weight, ColumnKind.CHECKBOX);
  }
}
