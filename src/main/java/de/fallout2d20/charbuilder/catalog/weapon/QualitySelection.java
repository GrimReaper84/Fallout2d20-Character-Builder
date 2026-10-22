package de.fallout2d20.charbuilder.catalog.weapon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Qualities picked for a weapon ({@link WeaponQuality#parse}).
 *
 * @param qualities the book's qualities
 * @param thrownRange the range of Thrown (Close or Medium); set exactly when {@code qualities} holds Thrown
 * @param ratings the number X of rated qualities in {@code qualities} (WG pp. 56–57); a rated quality without an
 *     entry is written with {@code (X)}, the number being set at the table (e.g. {@code Delay (X)})
 * @param other qualities outside the books, comma-separated as typed; empty for none
 */
public record QualitySelection(
    @NonNull Set<WeaponQuality> qualities,
    @Nullable WeaponRange thrownRange,
    @NonNull Map<WeaponQuality, Integer> ratings,
    @NonNull String other) {

  public QualitySelection {
    if (qualities.contains(WeaponQuality.THROWN) == (thrownRange == null)) {
      throw new IllegalArgumentException("Thrown needs its range, and a range needs Thrown");
    }
    qualities = qualities.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(qualities));
    for (Map.Entry<WeaponQuality, Integer> rating : ratings.entrySet()) {
      if (!rating.getKey().isRated() || !qualities.contains(rating.getKey()) || rating.getValue() < 1) {
        throw new IllegalArgumentException("A rating needs a rated quality of the weapon and a number of 1 or more");
      }
    }
    ratings = ratings.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(ratings));
    other = other.strip();
  }

  /** Qualities without ratings. */
  public QualitySelection(@NonNull Set<WeaponQuality> qualities, @Nullable WeaponRange thrownRange,
      @NonNull String other) {
    this(qualities, thrownRange, Map.of(), other);
  }

  /**
   * The qualities as stored in a weapon: in alphabetical order of the enum, then the others, e.g.
   * {@code Recoil (7), Thrown (M), Two-Handed}.
   */
  public @NonNull String text() {
    List<String> parts = new ArrayList<>();
    for (WeaponQuality quality : WeaponQuality.values()) {
      if (qualities.contains(quality)) {
        parts.add(written(quality));
      }
    }
    if (!other.isEmpty()) {
      parts.add(other);
    }
    return String.join(", ", parts);
  }

  private @NonNull String written(@NonNull WeaponQuality quality) {
    if (quality == WeaponQuality.THROWN && thrownRange != null) {
      return quality.displayName() + " (" + thrownRange.code() + ")";
    }
    Integer rating = ratings.get(quality);
    if (rating != null) {
      return quality.displayName() + " (" + rating + ")";
    }
    return quality.isRated() ? quality.displayName() + " (X)" : quality.displayName();
  }
}
