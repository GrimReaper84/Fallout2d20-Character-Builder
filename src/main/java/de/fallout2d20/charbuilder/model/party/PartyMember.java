package de.fallout2d20.charbuilder.model.party;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.derived.CarryLoad;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import de.fallout2d20.charbuilder.model.character.session.SessionValues;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;

/**
 * One member of the GM's party overview (H2): the GM's own quick notes on a character, typed in or copied once from
 * an open character ({@link #of}); no link to the character is kept and nothing is written back (user decision
 * 2026-11-02). Numbers are never negative; current HP never exceed the maximum lowered by rads (CRB p. 31).
 *
 * @param name the character's name
 * @param player the player's name
 * @param level the character's level
 * @param currentHp current health points
 * @param maxHp maximum health points before rads
 * @param rads radiation damage taken
 * @param initiative the character's Initiative
 * @param injuries injured hit locations as text, e.g. "Left arm, Head"
 */
public record PartyMember(@NonNull String name, @NonNull String player, int level, int currentHp, int maxHp,
    int rads, int initiative, @NonNull String injuries) {

  /** A new, empty row. */
  public static final @NonNull PartyMember NEW = new PartyMember("New member", "", 1, 0, 0, 0, 0, "");

  public PartyMember {
    level = Math.max(0, level);
    maxHp = Math.max(0, maxHp);
    rads = Math.max(0, rads);
    initiative = Math.max(0, initiative);
    currentHp = Math.clamp(currentHp, 0, SessionValues.effectiveMaxHp(maxHp, rads));
  }

  /**
   * The character's current values, with the given player: level, current and maximum HP, rads, Initiative (lowered
   * by an over-encumbered load, 0 when immobile, CRB p. 87) and the injured hit locations.
   */
  public static @NonNull PartyMember of(@NonNull PlayerCharacter character, @NonNull String player) {
    SessionValues values = SessionValues.of(character);
    CarryLoad load = CarryLoad.of(character);
    int initiative = load.initiative(DerivedStats.of(character).initiative());
    String injuries = character.hitLocations().stream().filter(values.injuries()::contains)
        .map(HitLocation::displayName).collect(Collectors.joining(", "));
    String name = character.getName().isBlank() ? "Unnamed" : character.getName();
    return new PartyMember(name, player, character.advancement().getLevel(), values.currentHp(), values.maxHp(),
        values.rads(), initiative, injuries);
  }

  /** The maximum HP lowered by the rads (CRB p. 31). */
  public int effectiveMaxHp() {
    return SessionValues.effectiveMaxHp(maxHp, rads);
  }

  public @NonNull PartyMember withName(@NonNull String newName) {
    return new PartyMember(newName, player, level, currentHp, maxHp, rads, initiative, injuries);
  }

  public @NonNull PartyMember withPlayer(@NonNull String newPlayer) {
    return new PartyMember(name, newPlayer, level, currentHp, maxHp, rads, initiative, injuries);
  }

  public @NonNull PartyMember withLevel(int newLevel) {
    return new PartyMember(name, player, newLevel, currentHp, maxHp, rads, initiative, injuries);
  }

  public @NonNull PartyMember withCurrentHp(int newCurrentHp) {
    return new PartyMember(name, player, level, newCurrentHp, maxHp, rads, initiative, injuries);
  }

  /** A new maximum; a member at full HP stays at full HP. */
  public @NonNull PartyMember withMaxHp(int newMaxHp) {
    boolean full = currentHp == effectiveMaxHp();
    PartyMember changed = new PartyMember(name, player, level, currentHp, newMaxHp, rads, initiative, injuries);
    return full ? changed.healed() : changed;
  }

  /** New rads; current HP above the new effective maximum fall with it (CRB p. 31), lowering rads does not heal. */
  public @NonNull PartyMember withRads(int newRads) {
    return new PartyMember(name, player, level, currentHp, maxHp, newRads, initiative, injuries);
  }

  public @NonNull PartyMember withInitiative(int newInitiative) {
    return new PartyMember(name, player, level, currentHp, maxHp, rads, newInitiative, injuries);
  }

  public @NonNull PartyMember withInjuries(@NonNull String newInjuries) {
    return new PartyMember(name, player, level, currentHp, maxHp, rads, initiative, newInjuries);
  }

  /** Current HP at the maximum lowered by rads. */
  public @NonNull PartyMember healed() {
    return withCurrentHp(effectiveMaxHp());
  }
}
