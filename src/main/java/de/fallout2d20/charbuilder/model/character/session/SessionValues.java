package de.fallout2d20.charbuilder.model.character.session;

import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Vitals;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * The play-session values of a character (D5), computed from its stored {@link Vitals} and its current
 * derived statistics: stored values above a limit that has since dropped (e.g. a GM lowering END or LCK)
 * are read as the limit.
 *
 * @param maxHp maximum HP before rads ({@link DerivedStats#maxHp()})
 * @param effectiveMaxHp maximum HP lowered by the rads (CRB p. 31), never below 0
 * @param currentHp current HP, at most the effective maximum
 * @param rads radiation damage taken
 * @param showsRads whether rads matter: the character is not immune to radiation damage (Ghoul, robots,
 *     CRB p. 47) or rads are stored anyway
 * @param radiationPoints stored Rad Sponge points, at most the maximum
 * @param maxRadiationPoints the origin's maximum (Child of Atom 5, WOA p. 25); 0 if it has none
 * @param luckPoints current Luck points, at most the maximum
 * @param maxLuckPoints maximum Luck points ({@link DerivedStats#maxLuckPoints()}, CRB p. 21)
 * @param injuries injured hit locations the character's body has
 */
public record SessionValues(
    int maxHp,
    int effectiveMaxHp,
    int currentHp,
    int rads,
    boolean showsRads,
    int radiationPoints,
    int maxRadiationPoints,
    int luckPoints,
    int maxLuckPoints,
    @NonNull Set<HitLocation> injuries) {

  public static @NonNull SessionValues of(@NonNull PlayerCharacter character) {
    Vitals vitals = character.session().getVitals();
    DerivedStats derived = DerivedStats.of(character);
    int effectiveMaxHp = effectiveMaxHp(derived.maxHp(), vitals.rads());
    Integer storedHp = vitals.currentHp();
    int maxRadiationPoints = character.getOriginData().map(OriginData::maxRadiationPoints).orElse(0);
    Integer storedLuck = vitals.luckPoints();
    Set<HitLocation> injuries = EnumSet.noneOf(HitLocation.class);
    injuries.addAll(vitals.injuries());
    injuries.retainAll(character.hitLocations());
    return new SessionValues(
        derived.maxHp(),
        effectiveMaxHp,
        storedHp == null ? effectiveMaxHp : Math.min(storedHp, effectiveMaxHp),
        vitals.rads(),
        !derived.radiation().immune() || vitals.rads() > 0,
        Math.min(vitals.radiationPoints(), maxRadiationPoints),
        maxRadiationPoints,
        storedLuck == null ? derived.maxLuckPoints() : Math.min(storedLuck, derived.maxLuckPoints()),
        derived.maxLuckPoints(),
        Set.copyOf(injuries));
  }

  /** CRB p. 31: every point of radiation damage lowers the maximum HP by one. */
  public static int effectiveMaxHp(int maxHp, int rads) {
    return Math.max(0, maxHp - rads);
  }

  /** Whether the origin can store Radiation Points (Rad Sponge). */
  public boolean showsRadiationPoints() {
    return maxRadiationPoints > 0;
  }
}
