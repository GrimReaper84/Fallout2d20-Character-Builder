package de.fallout2d20.charbuilder.model.character.derived;

import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.trait.TraitCatalog;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Formulas of the derived statistics. Origin effects (fixed carry weight, immunities, base radiation
 * DR) come from the origin catalog, trait effects (Small Frame, Heavy Handed, Gifted) from the trait
 * catalog; perk effects are looked up by catalog id here, until the perk catalog exists. A
 * character without origin or with an origin that is not in the catalog (e.g. an unsupported legacy
 * origin) gets no origin effects.
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
  private static final int CARRY_PER_STRENGTH = 10;
  private static final int STRONG_BACK_CARRY_PER_RANK = 25;
  private static final int SNAKEATER_POISON_DR = 2;

  private static final String LIFE_GIVER = "life-giver";
  private static final String STRONG_BACK = "strong-back";
  private static final String TOUGHNESS = "toughness";
  private static final String BARBARIAN = "barbarian";
  private static final String REFRACTOR = "refractor";
  private static final String RAD_RESISTANCE = "rad-resistance";
  private static final String SNAKEATER = "snakeater";

  private DerivedStatsCalculator() {}

  static @NonNull DerivedStats calculate(@NonNull Character character) {
    OriginData origin = character.getOriginData().orElse(null);
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
  private static int carryWeight(@NonNull Character character, @Nullable OriginData origin) {
    if (origin != null && origin.fixedCarryWeight() != null) {
      return origin.fixedCarryWeight();
    }
    int perStrength = traits(character).stream().map(TraitDefinition::carryWeightPerStrength)
        .filter(Objects::nonNull).min(Integer::compare).orElse(CARRY_PER_STRENGTH);
    return CARRY_BASE + perStrength * character.attributeValue(Attribute.STRENGTH)
        + STRONG_BACK_CARRY_PER_RANK * perkRank(character, STRONG_BACK);
  }

  /** CRB p. 47: STR 7–8 +1, 9–10 +2, 11+ +3 Combat Dice; Heavy Handed +1 (CRB p. 56). */
  private static int meleeBonusDice(@NonNull Character character) {
    return strengthTier(character.attributeValue(Attribute.STRENGTH))
        + traits(character).stream().mapToInt(TraitDefinition::meleeBonusDice).sum();
  }

  /** CRB p. 21: at most as many Luck points as the Luck attribute; Gifted one fewer (CRB p. 56). */
  private static int maxLuckPoints(@NonNull Character character) {
    int luck = character.attributeValue(Attribute.LUCK)
        - traits(character).stream().mapToInt(TraitDefinition::luckPointPenalty).sum();
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

  /** Rad Resistance +1 per rank (CRB p. 70); origin immunity or base DR (Child of Atom). */
  private static @NonNull Resistance radiationResistance(@NonNull Character character,
      @Nullable OriginData origin) {
    if (origin != null && origin.radiationImmune()) {
      return Resistance.IMMUNITY;
    }
    int base = origin == null ? 0 : origin.baseRadiationDr();
    return Resistance.of(base + perkRank(character, RAD_RESISTANCE));
  }

  /** Snakeater +2 (CRB p. 72); origin immunity. */
  private static @NonNull Resistance poisonResistance(@NonNull Character character,
      @Nullable OriginData origin) {
    if (origin != null && origin.poisonImmune()) {
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

  private static int perkRank(@NonNull Character character, @NonNull String perkId) {
    int rank = 0;
    for (PerkSelection perk : character.getPerks()) {
      if (perk.perk().id().equals(perkId)) {
        rank += perk.rank();
      }
    }
    return rank;
  }

  /** Catalog entries of the character's traits; imported traits count too (not in the baseline). */
  private static @NonNull List<TraitDefinition> traits(@NonNull Character character) {
    return character.getTraits().stream()
        .map(selection -> TraitCatalog.byId(selection.trait().id()))
        .flatMap(Optional::stream).toList();
  }
}
