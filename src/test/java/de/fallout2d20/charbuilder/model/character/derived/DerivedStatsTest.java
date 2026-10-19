package de.fallout2d20.charbuilder.model.character.derived;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.LegacyBaseline;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DerivedStatsTest {

  /** CRB p. 47: PER 5 and AGI 7 give Initiative 12. */
  @Test
  void initiativeBookExample() {
    DerivedStats stats = DerivedStats.of(builder().attribute(Attribute.AGILITY, 7).build());
    assertEquals(12, stats.initiative());
  }

  /** JC from the legacy save (fixture test-survivor-v2.json): Survivor, level 2, Small Frame. */
  @Test
  void jcValues() {
    DerivedStats stats = DerivedStats.of(builder().level(2)
        .attribute(Attribute.STRENGTH, 4).attribute(Attribute.PERCEPTION, 7)
        .attribute(Attribute.INTELLIGENCE, 8).attribute(Attribute.AGILITY, 6)
        .trait("Small Frame").perk("Jury Rigging", 1).perk("Comprehension", 1).perk("Gun Nut", 1)
        .build());

    assertEquals(new DerivedStats(11, 13, 1, 170, 0, 5, Resistance.of(0), Resistance.of(0),
        Resistance.of(0), Resistance.of(0)), stats);
  }

  /** CRB p. 49, 74: END + LCK + 1 per further level; CRB p. 66: Life Giver adds END per rank. */
  @Test
  void hpGrowsPerLevelAndLifeGiver() {
    PlayerCharacter character = builder().level(10).attribute(Attribute.ENDURANCE, 7)
        .perk("Life Giver", 2).build();
    assertEquals(35, DerivedStats.of(character).maxHp());
  }

  /** CRB p. 47: Defense 2 from AGI 9. */
  @ParameterizedTest
  @CsvSource({"8, 1", "9, 2"})
  void defenseThreshold(int agility, int defense) {
    assertEquals(defense, DerivedStats.of(builder().attribute(Attribute.AGILITY, agility).build()).defense());
  }

  /** CRB p. 47 table. */
  @ParameterizedTest
  @CsvSource({"6, 0", "7, 1", "8, 1", "9, 2", "10, 2", "11, 3"})
  void meleeThresholds(int strength, int dice) {
    assertEquals(dice,
        DerivedStats.of(builder().attribute(Attribute.STRENGTH, strength).build()).meleeBonusDice());
  }

  /** CRB p. 56: Heavy Handed adds one Combat Die to the melee bonus. */
  @Test
  void heavyHandedAddsOneDie() {
    PlayerCharacter character = builder().attribute(Attribute.STRENGTH, 7).trait("Heavy Handed").build();
    assertEquals(2, DerivedStats.of(character).meleeBonusDice());
  }

  /** CRB p. 47: 150 + 10 × STR; CRB p. 73: Strong Back +25 per rank; CRB p. 56: Small Frame 5 × STR. */
  @Test
  void carryWeight() {
    TestCharacterBuilder strongBack = builder().attribute(Attribute.STRENGTH, 7).perk("Strong Back", 2);
    assertEquals(270, DerivedStats.of(strongBack.build()).carryWeight());
    assertEquals(235, DerivedStats.of(strongBack.trait("Small Frame").build()).carryWeight());
  }

  /** CRB p. 55, WOA p. 21, WG p. 11: fixed carry weight, unaffected by STR and perks. */
  @ParameterizedTest
  @CsvSource({"mister-handy, 150", "protectron, 225", "assaultron, 150"})
  void fixedCarryIgnoresStrengthAndPerks(String origin, int carry) {
    PlayerCharacter character = builder().origin(origin).attribute(Attribute.STRENGTH, 8)
        .perk("Strong Back", 1).trait("Small Frame").build();
    assertEquals(carry, DerivedStats.of(character).carryWeight());
  }

  /** CRB p. 21: Luck points up to LCK; CRB p. 56: Gifted lowers the maximum by one. */
  @Test
  void luckPoints() {
    TestCharacterBuilder lucky = builder().attribute(Attribute.LUCK, 6);
    assertEquals(6, DerivedStats.of(lucky.build()).maxLuckPoints());
    assertEquals(5, DerivedStats.of(lucky.trait("Gifted").build()).maxLuckPoints());
  }

  /** CRB p. 60 (Barbarian by STR), p. 73 (Toughness per rank). */
  @Test
  void physicalDr() {
    TestCharacterBuilder barbarian = builder().attribute(Attribute.STRENGTH, 9)
        .perk("Barbarian", 1).perk("Toughness", 1);
    assertEquals(Resistance.of(3), DerivedStats.of(barbarian.build()).physical());
    assertEquals(Resistance.of(4),
        DerivedStats.of(barbarian.attribute(Attribute.STRENGTH, 11).build()).physical());
  }

  /** CRB p. 60: Barbarian gives no bonus below STR 7. */
  @Test
  void barbarianNeedsStrengthSeven() {
    PlayerCharacter character = builder().attribute(Attribute.STRENGTH, 6).perk("Barbarian", 1).build();
    assertEquals(Resistance.of(0), DerivedStats.of(character).physical());
  }

  /** CRB p. 70 (Refractor, Rad Resistance), p. 72 (Snakeater). */
  @Test
  void energyRadiationPoison() {
    DerivedStats stats = DerivedStats.of(builder().perk("Refractor", 2).perk("Rad Resistance", 1)
        .perk("Snakeater", 1).build());
    assertEquals(Resistance.of(2), stats.energy());
    assertEquals(Resistance.of(1), stats.radiation());
    assertEquals(Resistance.of(2), stats.poison());
  }

  /** CRB p. 53: the STR/END +2 of a new Super Mutant feeds HP, carry weight and melee bonus. */
  @Test
  void superMutantModifiersReachTheDerivedStatistics() {
    PlayerCharacter mutant = builder().origin("super-mutant").build();
    DerivedStats stats = DerivedStats.of(mutant);
    assertEquals(7 + 5, stats.maxHp());
    assertEquals(150 + 70, stats.carryWeight());
    assertEquals(1, stats.meleeBonusDice());
  }

  /** CRB p. 53, 55; WOA p. 19, 21; WG p. 11, 14: immune to radiation and poison. */
  @ParameterizedTest
  @CsvSource({"super-mutant", "mister-handy", "generation-3-synth", "protectron", "assaultron",
      "nightkin"})
  void radiationAndPoisonImmunity(String origin) {
    DerivedStats stats = DerivedStats.of(builder().origin(origin).perk("Snakeater", 1).build());
    assertEquals(Resistance.IMMUNITY, stats.radiation());
    assertEquals(Resistance.IMMUNITY, stats.poison());
  }

  /** CRB p. 52: a ghoul is immune to radiation, but not to poison. */
  @Test
  void ghoulIsOnlyRadiationImmune() {
    DerivedStats stats = DerivedStats.of(builder().origin("ghoul").perk("Snakeater", 1).build());
    assertEquals(Resistance.IMMUNITY, stats.radiation());
    assertEquals(Resistance.of(2), stats.poison());
  }

  /** WOA p. 25, WG p. 13: Rad Sponge gives a base radiation DR of 1. */
  @Test
  void childOfAtomHasBaseRadiationDr() {
    PlayerCharacter character = builder().origin("child-of-atom").perk("Rad Resistance", 1).build();
    assertEquals(Resistance.of(2), DerivedStats.of(character).radiation());
  }

  @Test
  void newCharacterWithoutOriginUsesBaseRules() {
    DerivedStats stats = DerivedStats.of(PlayerCharacter.createNew());
    assertEquals(new DerivedStats(10, 10, 1, 200, 0, 5, Resistance.of(0), Resistance.of(0),
        Resistance.of(0), Resistance.of(0)), stats);
  }

  /** Imported traits and perk ranks are not part of the legacy baseline, so their effects apply. */
  @Test
  void importedTraitsAndPerksCount() {
    Map<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
    for (Attribute attribute : Attribute.values()) {
      attributes.put(attribute, 6);
    }
    Map<Skill, Integer> ranks = new EnumMap<>(Skill.class);
    for (Skill skill : Skill.values()) {
      ranks.put(skill, 0);
    }
    PlayerCharacter character = PlayerCharacter.builder().name("Imported").origin(CatalogRef.of("survivor")).level(3)
        .xp(300).creationComplete(true)
        .traits(List.of(new TraitSelection(CatalogRef.fromLegacyName("Small Frame"), true, List.of())))
        .perks(List.of(new PerkSelection(CatalogRef.fromLegacyName("Life Giver"), 2, 2, List.of(), List.of())))
        .legacyBaseline(new LegacyBaseline(attributes, ranks)).build();

    DerivedStats stats = DerivedStats.of(character);
    assertEquals(6 + 6 + 2 + 12, stats.maxHp());
    assertEquals(150 + 30, stats.carryWeight());
  }

  private static TestCharacterBuilder builder() {
    return new TestCharacterBuilder();
  }

  /** CRB p. 54, 146: fixed 150 lbs., Mister Gutsy Plating −10, Factory Storage Armor +20; an arm piece counts once its arm is chosen. */
  @Test
  void robotArmorChangesTheMisterHandyCarryWeight() {
    PlayerCharacter handy = PlayerCharacter.createNew();
    handy.chooseOrigin(CatalogRef.of("mister-handy"));
    handy.addApparel(WornApparel.of(CatalogRef.of("mister-gutsy-plating")));
    assertEquals(140, DerivedStats.of(handy).carryWeight());
    handy.addApparel(WornApparel.of(CatalogRef.of("factory-storage-armor-main-body")));
    assertEquals(160, DerivedStats.of(handy).carryWeight());
    handy.addApparel(WornApparel.of(CatalogRef.of("primal-plate-arms")));
    assertEquals(160, DerivedStats.of(handy).carryWeight(), "no arm chosen yet");
    handy.setApparelLocation(2, HitLocation.ARM_2);
    assertEquals(150, DerivedStats.of(handy).carryWeight());
  }
}
