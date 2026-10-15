package de.fallout2d20.charbuilder.model.character.derived;

import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Formulas of the derived statistics and the only place that knows which perks, traits and origins
 * modify them. Until the catalogs exist (feature A2), effects are looked up by catalog id; the id
 * tables below are meant to move into catalog data then.
 *
 * <p>Effects of imported perk ranks and traits count fully: a legacy baseline only contains
 * attribute and skill values, never derived statistics.
 *
 * <p>Deliberate deviations from the legacy app: the melee bonus is a number that includes Heavy
 * Handed instead of an appended text, the maximum Luck points are computed (missing in the legacy
 * app), and immunity is a flag instead of a magic string value.
 */
final class DerivedStatsCalculator {

  private static final int CARRY_BASE = 150;
  private static final int STRONG_BACK_CARRY_PER_RANK = 25;
  private static final int SNAKEATER_POISON_DR = 2;

  private static final String LIFE_GIVER = "life-giver";
  private static final String STRONG_BACK = "strong-back";
  private static final String TOUGHNESS = "toughness";
  private static final String BARBARIAN = "barbarian";
  private static final String REFRACTOR = "refractor";
  private static final String RAD_RESISTANCE = "rad-resistance";
  private static final String SNAKEATER = "snakeater";

  private static final String SMALL_FRAME = "small-frame";
  private static final String HEAVY_HANDED = "heavy-handed";
  private static final String GIFTED = "gifted";

  /**
   * Origins whose carry weight is fixed and not increased by STR or perks: Mister Handy (CRB p. 55),
   * Protectron (WOA p. 21), Assaultron (WG p. 11).
   */
  private static final Map<String, Integer> FIXED_CARRY_WEIGHT =
      Map.of("mister-handy", 150, "protectron", 225, "assaultron", 150);

  /**
   * Origins immune to radiation and poison damage: Super Mutant (CRB p. 53), Mister Handy
   * (CRB p. 55), Generation 3 Synth (WOA p. 19), Protectron (WOA p. 21), Assaultron (WG p. 11),
   * Nightkin (WG p. 14).
   */
  private static final Set<String> RADIATION_AND_POISON_IMMUNE = Set.of("super-mutant",
      "mister-handy", "generation-3-synth", "protectron", "assaultron", "nightkin");

  /** CRB p. 52: a ghoul is immune to radiation damage (and is healed by it). */
  private static final String GHOUL = "ghoul";

  /** WOA p. 25, WG p. 13 (Rad Sponge): a Child of Atom has a base radiation DR of 1. */
  private static final String CHILD_OF_ATOM = "child-of-atom";

  private DerivedStatsCalculator() {}

  static @NonNull DerivedStats calculate(@NonNull Character character) {
    String origin = originId(character);
    return new DerivedStats(
        maxHp(character),
        initiative(character),
        defense(character),
        carryWeight(character, origin),
        meleeBonusDice(character),
        maxLuckPoints(character),
        physicalResistance(character),
        Resistance.of(perkRank(character, REFRACTOR)),
        radiationResistance(character, origin),
        poisonResistance(character, origin));
  }

  /**
   * CRB p. 74: END + LCK at level 1; CRB p. 49: +1 per further level; CRB p. 66: Life Giver adds
   * END per rank. The book does not say whether a later END increase raises the Life Giver bonus;
   * the current END is used (user decision, as in the legacy app).
   */
  private static int maxHp(@NonNull Character character) {
    int endurance = character.attributeValue(Attribute.ENDURANCE);
    return endurance + character.attributeValue(Attribute.LUCK) + character.getLevel() - 1
        + endurance * perkRank(character, LIFE_GIVER);
  }

  /** CRB p. 47: PER + AGI. */
  private static int initiative(@NonNull Character character) {
    return character.attributeValue(Attribute.PERCEPTION) + character.attributeValue(Attribute.AGILITY);
  }

  /** CRB p. 47: Defense 1, or 2 from AGI 9. */
  private static int defense(@NonNull Character character) {
    return character.attributeValue(Attribute.AGILITY) >= 9 ? 2 : 1;
  }

  /** CRB p. 47, 74: 150 + 10 × STR; Small Frame 5 × STR (CRB p. 56); Strong Back +25 per rank (CRB p. 73). */
  private static int carryWeight(@NonNull Character character, @Nullable String origin) {
    Integer fixed = origin == null ? null : FIXED_CARRY_WEIGHT.get(origin);
    if (fixed != null) {
      return fixed;
    }
    int perStrength = hasTrait(character, SMALL_FRAME) ? 5 : 10;
    return CARRY_BASE + perStrength * character.attributeValue(Attribute.STRENGTH)
        + STRONG_BACK_CARRY_PER_RANK * perkRank(character, STRONG_BACK);
  }

  /** CRB p. 47: STR 7–8 +1, 9–10 +2, 11+ +3 Combat Dice; Heavy Handed +1 (CRB p. 56). */
  private static int meleeBonusDice(@NonNull Character character) {
    return strengthTier(character.attributeValue(Attribute.STRENGTH))
        + (hasTrait(character, HEAVY_HANDED) ? 1 : 0);
  }

  /** CRB p. 21: at most as many Luck points as the Luck attribute; Gifted one fewer (CRB p. 56). */
  private static int maxLuckPoints(@NonNull Character character) {
    int luck = character.attributeValue(Attribute.LUCK) - (hasTrait(character, GIFTED) ? 1 : 0);
    return Math.max(0, luck);
  }

  /**
   * Toughness +1 per rank (CRB p. 73); Barbarian +1/+2/+3 by STR (CRB p. 60). Barbarian does not
   * apply in Power Armor; that exclusion follows with the armor feature.
   */
  private static @NonNull Resistance physicalResistance(@NonNull Character character) {
    int barbarian = perkRank(character, BARBARIAN) > 0
        ? strengthTier(character.attributeValue(Attribute.STRENGTH)) : 0;
    return Resistance.of(perkRank(character, TOUGHNESS) + barbarian);
  }

  /** Rad Resistance +1 per rank (CRB p. 70); origin immunity or Child of Atom base DR 1. */
  private static @NonNull Resistance radiationResistance(@NonNull Character character,
      @Nullable String origin) {
    if (isRadiationAndPoisonImmune(origin) || GHOUL.equals(origin)) {
      return Resistance.IMMUNITY;
    }
    int base = CHILD_OF_ATOM.equals(origin) ? 1 : 0;
    return Resistance.of(base + perkRank(character, RAD_RESISTANCE));
  }

  /** Snakeater +2 (CRB p. 72); origin immunity. */
  private static @NonNull Resistance poisonResistance(@NonNull Character character,
      @Nullable String origin) {
    if (isRadiationAndPoisonImmune(origin)) {
      return Resistance.IMMUNITY;
    }
    return Resistance.of(SNAKEATER_POISON_DR * perkRank(character, SNAKEATER));
  }

  /** Shared STR thresholds of the melee bonus (CRB p. 47) and Barbarian (CRB p. 60). */
  private static int strengthTier(int strength) {
    if (strength >= 11) {
      return 3;
    }
    if (strength >= 9) {
      return 2;
    }
    return strength >= 7 ? 1 : 0;
  }

  private static boolean isRadiationAndPoisonImmune(@Nullable String origin) {
    return origin != null && RADIATION_AND_POISON_IMMUNE.contains(origin);
  }

  private static @Nullable String originId(@NonNull Character character) {
    CatalogRef origin = character.getOrigin();
    return origin == null ? null : origin.id();
  }

  private static int perkRank(@NonNull Character character, @NonNull String perkId) {
    int rank = 0;
    for (PerkSelection perk : character.getPerks()) {
      if (perk.perk().id().equals(perkId)) {
        rank += perk.rank();
      }
    }
    return rank;
  }

  private static boolean hasTrait(@NonNull Character character, @NonNull String traitId) {
    for (TraitSelection trait : character.getTraits()) {
      if (trait.trait().id().equals(traitId)) {
        return true;
      }
    }
    return false;
  }
}
