package de.fallout2d20.charbuilder.model.character;

import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import de.fallout2d20.charbuilder.model.character.session.SessionValues;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The play-session values of a character (D5): current HP, rads, Rad Sponge points, Luck points, injuries and
 * session notes. Part of a {@link PlayerCharacter} ({@link PlayerCharacter#session()}); apart from the notes,
 * they change only after creation.
 */
public final class Session {

  private final @NonNull PlayerCharacter owner;
  private @NonNull Vitals vitals;

  Session(@NonNull PlayerCharacter owner, @NonNull Vitals vitals) {
    this.owner = owner;
    this.vitals = vitals;
  }

  public @NonNull Vitals getVitals() {
    return vitals;
  }

  /**
   * Sets the current HP, clamped to 0 and the maximum lowered by rads (CRB p. 31); the maximum itself is
   * stored as "at maximum", so later level-ups and healing keep it full.
   *
   * @throws IllegalStateException during creation
   */
  public void setCurrentHp(int currentHp) {
    owner.requireComplete();
    int max = SessionValues.of(owner).effectiveMaxHp();
    int clamped = Math.clamp(currentHp, 0, max);
    vitals = vitals.withCurrentHp(clamped == max ? null : clamped);
  }

  /** Heals to the (rad-lowered) maximum HP. */
  public void healFully() {
    owner.requireComplete();
    vitals = vitals.withCurrentHp(null);
  }

  /**
   * Sets the rads, clamped to 0 and the maximum HP; current HP above the new effective maximum fall with it
   * (CRB p. 31). Lowering rads does not heal (CRB p. 35: treatment removes rads, HP heal separately).
   *
   * @throws IllegalStateException during creation
   */
  public void setRads(int rads) {
    owner.requireComplete();
    int current = SessionValues.of(owner).currentHp();
    vitals = vitals.withRads(Math.clamp(rads, 0, DerivedStats.of(owner).maxHp()));
    int max = SessionValues.of(owner).effectiveMaxHp();
    vitals = vitals.withCurrentHp(current >= max ? null : current);
  }

  /**
   * Sets the stored Rad Sponge points, clamped to 0 and the origin's maximum (WOA p. 25: up to 5).
   *
   * @throws IllegalStateException during creation
   */
  public void setRadiationPoints(int points) {
    owner.requireComplete();
    vitals = vitals.withRadiationPoints(Math.clamp(points, 0, SessionValues.of(owner).maxRadiationPoints()));
  }

  /**
   * Sets the current Luck points, clamped to 0 and the maximum (CRB p. 21); the maximum is stored as "full".
   *
   * @throws IllegalStateException during creation
   */
  public void setLuckPoints(int points) {
    owner.requireComplete();
    int max = SessionValues.of(owner).maxLuckPoints();
    int clamped = Math.clamp(points, 0, max);
    vitals = vitals.withLuckPoints(clamped == max ? null : clamped);
  }

  /** CRB p. 21: the GM refreshes all Luck points at a milestone or a new quest. */
  public void refreshLuck() {
    owner.requireComplete();
    vitals = vitals.withLuckPoints(null);
  }

  /**
   * Marks or clears an injury at a hit location of the character's body (CRB p. 32).
   *
   * @throws IllegalArgumentException for a location the body does not have
   * @throws IllegalStateException during creation
   */
  public void setInjured(@NonNull HitLocation location, boolean injured) {
    owner.requireComplete();
    if (!owner.hitLocations().contains(location)) {
      throw new IllegalArgumentException("No such hit location on this body: " + location);
    }
    vitals = vitals.withInjury(location, injured);
  }

  /** Free-text session notes (conditions, chem durations, ...); editable in every mode. */
  public void setNotes(@Nullable String notes) {
    vitals = vitals.withNotes(notes == null ? "" : notes);
  }

  /**
   * Raises a stored current HP by the levels gained (+1 maximum HP each, CRB p. 49), so damage taken stays
   * (user decision, no book rule); "at maximum" stays at maximum.
   */
  void raiseCurrentHp(int levels) {
    Integer currentHp = vitals.currentHp();
    vitals = vitals.withCurrentHp(currentHp == null ? null : currentHp + levels);
  }

  /** Lowers a stored current HP that is above the (rad-lowered) maximum, e.g. after losing levels. */
  void capCurrentHp() {
    Integer currentHp = vitals.currentHp();
    int max = SessionValues.of(owner).effectiveMaxHp();
    if (currentHp != null && currentHp >= max) {
      vitals = vitals.withCurrentHp(null);
    }
  }
}
