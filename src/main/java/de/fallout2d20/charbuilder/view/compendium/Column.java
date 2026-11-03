package de.fallout2d20.charbuilder.view.compendium;

import org.jspecify.annotations.NonNull;

/**
 * A table column of a compendium section (H2), after the name column.
 *
 * @param header the column header
 * @param numeric whether the column sorts by {@link Cell#number()}
 */
record Column(@NonNull String header, boolean numeric) {

  static @NonNull Column text(@NonNull String header) {
    return new Column(header, false);
  }

  static @NonNull Column number(@NonNull String header) {
    return new Column(header, true);
  }
}
