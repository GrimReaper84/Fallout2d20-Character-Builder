package de.fallout2d20.charbuilder.model.character.derived;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Reads the weight printed in the catalogs. CRB p. 87: items of weight {@code <1} count nothing; tables
 * without a weight ("–", robot items, backpacks) count nothing either. All other printed weights are whole
 * pounds.
 */
public final class Weight {

  private static final @NonNull String LIGHT = "<1";

  private Weight() {}

  /**
   * The weight in whole lbs.
   *
   * @throws IllegalArgumentException for a value that is neither empty, {@code <1}, a dash nor a whole number
   */
  public static int pounds(@Nullable String printed) {
    if (printed == null || printed.isBlank() || printed.equals(LIGHT) || printed.equals("–") || printed.equals("-")) {
      return 0;
    }
    try {
      int pounds = Integer.parseInt(printed.strip());
      if (pounds < 0) {
        throw new IllegalArgumentException("Negative weight: " + printed);
      }
      return pounds;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Weight is not a whole number of lbs.: " + printed, e);
    }
  }
}
