package de.fallout2d20.charbuilder.model.character;

import org.jspecify.annotations.Nullable;

/**
 * Minimal play-session state (decision Q2 of the feature checklist).
 *
 * @param currentHp current health points, {@code null} for "at maximum"
 * @param rads radiation damage taken
 * @param radiationPoints Rad Sponge points of a Child of Atom (0–5)
 */
public record Vitals(@Nullable Integer currentHp, int rads, int radiationPoints) {

  public static final Vitals FULL = new Vitals(null, 0, 0);

  public Vitals {
    if ((currentHp != null && currentHp < 0) || rads < 0 || radiationPoints < 0) {
      throw new IllegalArgumentException("Vitals must not be negative");
    }
  }
}
