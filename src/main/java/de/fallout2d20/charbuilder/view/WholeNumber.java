package de.fallout2d20.charbuilder.view;

import java.util.OptionalInt;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Reads whole numbers typed into text fields. */
public final class WholeNumber {

  private WholeNumber() {}

  /** The typed whole number (surrounding blanks ignored); empty for anything else. */
  public static @NonNull OptionalInt parse(@NonNull String text) {
    try {
      return OptionalInt.of(Integer.parseInt(text.strip()));
    } catch (NumberFormatException _) {
      return OptionalInt.empty();
    }
  }

  /** The typed whole number if it is at least {@code minimum}; {@code null} for anything else. */
  public static @Nullable Integer atLeast(@NonNull String text, int minimum) {
    return between(text, minimum, Integer.MAX_VALUE);
  }

  /** The typed whole number if it lies in {@code minimum}–{@code maximum}; {@code null} for anything else. */
  public static @Nullable Integer between(@NonNull String text, int minimum, int maximum) {
    OptionalInt value = parse(text);
    return value.isPresent() && value.getAsInt() >= minimum && value.getAsInt() <= maximum ? value.getAsInt() : null;
  }
}
