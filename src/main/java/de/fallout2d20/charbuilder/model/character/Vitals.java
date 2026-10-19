package de.fallout2d20.charbuilder.model.character;

import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Play-session state (D5, decision Q2 of the feature checklist). Limits that depend on the character
 * (maximum HP, Luck points, the body's hit locations) are applied by {@code session.SessionValues} when
 * read and by the {@link PlayerCharacter} mutators when written.
 *
 * <p>Changes that lower a limit elsewhere (GM lowers END or LCK, removes a perk, changes the origin) do not
 * rewrite the stored values; they are only read as the lower limit. If the limit rises again, the old
 * stored value shows again (e.g. stored 8 HP, END lowered: 6 shown, END restored: 8 shown). Read HP, rads,
 * Radiation Points, Luck points and injuries through {@code SessionValues}.
 *
 * @param currentHp current health points, {@code null} for "at the (rad-reduced) maximum"
 * @param rads radiation damage taken; lowers the maximum HP (CRB p. 31)
 * @param radiationPoints Rad Sponge points of a Child of Atom (WOA p. 25)
 * @param luckPoints current Luck points, {@code null} for "full" (CRB p. 21)
 * @param injuries hit locations with an untreated injury (CRB p. 32)
 * @param notes free-text session notes
 */
public record Vitals(
    @Nullable Integer currentHp,
    int rads,
    int radiationPoints,
    @Nullable Integer luckPoints,
    @NonNull Set<HitLocation> injuries,
    @NonNull String notes) {

  public static final Vitals FULL = new Vitals(null, 0, 0);

  public Vitals {
    if ((currentHp != null && currentHp < 0) || rads < 0 || radiationPoints < 0
        || (luckPoints != null && luckPoints < 0)) {
      throw new IllegalArgumentException("Vitals must not be negative");
    }
    injuries = injuries.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(injuries));
  }

  /** HP, rads and radiation points only (the legacy vitals); Luck full, no injuries, no notes. */
  public Vitals(@Nullable Integer currentHp, int rads, int radiationPoints) {
    this(currentHp, rads, radiationPoints, null, Set.of(), "");
  }

  public @NonNull Vitals withCurrentHp(@Nullable Integer newCurrentHp) {
    return new Vitals(newCurrentHp, rads, radiationPoints, luckPoints, injuries, notes);
  }

  public @NonNull Vitals withRads(int newRads) {
    return new Vitals(currentHp, newRads, radiationPoints, luckPoints, injuries, notes);
  }

  public @NonNull Vitals withRadiationPoints(int newRadiationPoints) {
    return new Vitals(currentHp, rads, newRadiationPoints, luckPoints, injuries, notes);
  }

  public @NonNull Vitals withLuckPoints(@Nullable Integer newLuckPoints) {
    return new Vitals(currentHp, rads, radiationPoints, newLuckPoints, injuries, notes);
  }

  public @NonNull Vitals withInjury(@NonNull HitLocation location, boolean injured) {
    Set<HitLocation> changed = EnumSet.noneOf(HitLocation.class);
    changed.addAll(injuries);
    if (injured) {
      changed.add(location);
    } else {
      changed.remove(location);
    }
    return new Vitals(currentHp, rads, radiationPoints, luckPoints, changed, notes);
  }

  public @NonNull Vitals withNotes(@NonNull String newNotes) {
    return new Vitals(currentHp, rads, radiationPoints, luckPoints, injuries, newNotes);
  }
}
