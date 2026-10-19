package de.fallout2d20.charbuilder.model.character.derived;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import org.jspecify.annotations.NonNull;

/**
 * Derived statistics of a character (CRB p. 47, p. 74). They are computed from attributes, level,
 * origin, traits and perks on every call and never stored.
 *
 * <p>Damage Resistance values are innate (origin and perks); armor is not included.
 *
 * @param maxHp maximum health points: END + LCK, +1 per level after the first, + END per rank of
 *     Life Giver (CRB p. 49, 66, 74)
 * @param initiative PER + AGI (CRB p. 47), unencumbered; {@link CarryLoad} names the reduction (CRB p. 87)
 * @param defense 1, or 2 from AGI 9 (CRB p. 47)
 * @param carryWeight carry weight in lbs.: 150 + 10 × STR (Small Frame: 5 × STR), +25 per rank of
 *     Strong Back, + 5 or 10 × STR for a backpack; fixed for Mister Handy, Protectron and Assaultron,
 *     changed by robot armor and modules (CRB p. 47, 55, 56, 73, 146, 181, 185; WOA p. 21; WG p. 11)
 * @param meleeBonusDice bonus Combat Dice on melee and unarmed damage from STR, +1 for Heavy
 *     Handed (CRB p. 47, 56)
 * @param maxLuckPoints Luck points a character can hold: the Luck attribute, one fewer with Gifted
 *     (CRB p. 21, 56)
 * @param physical physical Damage Resistance from Toughness and Barbarian (CRB p. 60, 73)
 * @param energy energy Damage Resistance from Refractor (CRB p. 70)
 * @param radiation radiation Damage Resistance from Rad Resistance, Child of Atom, or immunity by
 *     origin (CRB p. 52, 53, 55, 70; WOA p. 19, 21, 25; WG p. 11, 14)
 * @param poison poison Damage Resistance (a single value for the whole body) from Snakeater, or
 *     immunity by origin (CRB p. 47, 72)
 */
public record DerivedStats(
    int maxHp,
    int initiative,
    int defense,
    int carryWeight,
    int meleeBonusDice,
    int maxLuckPoints,
    @NonNull Resistance physical,
    @NonNull Resistance energy,
    @NonNull Resistance radiation,
    @NonNull Resistance poison) {

  /** Computes the current derived statistics of the character. */
  public static @NonNull DerivedStats of(@NonNull PlayerCharacter character) {
    return DerivedStatsCalculator.calculate(character);
  }

  /** The melee bonus as shown in the app and on the sheet: "+0" or e.g. "+2 CD". */
  public @NonNull String meleeDamageText() {
    return meleeBonusDice == 0 ? "+0" : "+" + meleeBonusDice + " CD";
  }
}
