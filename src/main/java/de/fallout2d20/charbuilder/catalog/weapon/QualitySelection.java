package de.fallout2d20.charbuilder.catalog.weapon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Qualities picked for a weapon ({@link WeaponQuality#parse}).
 *
 * @param qualities the book's qualities
 * @param thrownRange the range of Thrown (Close or Medium); set exactly when {@code qualities} holds Thrown
 * @param other qualities outside the book, comma-separated as typed; empty for none
 */
public record QualitySelection(
    @NonNull Set<WeaponQuality> qualities,
    @Nullable WeaponRange thrownRange,
    @NonNull String other) {

  public QualitySelection {
    if (qualities.contains(WeaponQuality.THROWN) == (thrownRange == null)) {
      throw new IllegalArgumentException("Thrown needs its range, and a range needs Thrown");
    }
    qualities = qualities.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(qualities));
    other = other.strip();
  }

  /** The qualities as stored in a weapon: book order, then the others, e.g. {@code Thrown (M), Two-Handed}. */
  public @NonNull String text() {
    List<String> parts = new ArrayList<>();
    for (WeaponQuality quality : WeaponQuality.values()) {
      if (qualities.contains(quality)) {
        parts.add(quality == WeaponQuality.THROWN && thrownRange != null
            ? quality.displayName() + " (" + thrownRange.code() + ")" : quality.displayName());
      }
    }
    if (!other.isEmpty()) {
      parts.add(other);
    }
    return String.join(", ", parts);
  }
}
