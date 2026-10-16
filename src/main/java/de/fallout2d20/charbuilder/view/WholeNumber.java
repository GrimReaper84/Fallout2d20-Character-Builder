package de.fallout2d20.charbuilder.view;

import java.util.OptionalInt;
import org.jspecify.annotations.NonNull;

/** Reads whole numbers typed into text fields. */
public final class WholeNumber {

  private WholeNumber() {}

  /** The typed whole number (surrounding blanks ignored); empty for anything else. */
  public static @NonNull OptionalInt parse(@NonNull String text) {
    try {
      return OptionalInt.of(Integer.parseInt(text.strip()));
    } catch (NumberFormatException e) {
      return OptionalInt.empty();
    }
  }
}
