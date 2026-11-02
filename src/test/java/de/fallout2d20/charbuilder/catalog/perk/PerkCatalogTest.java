package de.fallout2d20.charbuilder.catalog.perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.weapon.DamageEffect;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponQuality;
import de.fallout2d20.charbuilder.model.character.Attribute;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PerkCatalogTest {

  /** CRB p. 59–73: 94 perks; WG pp. 44–53: 54 more and pp. 109–113: 21 rare perks, each from one book and page. */
  @Test
  void loadsAllCoreAndWandererPerks() {
    List<PerkDefinition> perks = PerkCatalog.all();
    assertEquals(169, perks.size());
    assertEquals("action-boy-girl", perks.getFirst().id());
    assertEquals("toughness", perks.get(93).id());
    for (PerkDefinition perk : perks) {
      assertEquals(1, perk.sources().size(), perk.id());
      Book book = perk.sources().getFirst().book();
      int page = perk.sources().getFirst().page();
      boolean rare = perk.requirements().rare();
      assertTrue(book == Book.CRB ? page >= 59 && page <= 73 && !rare
          : book == Book.WG && (rare ? page >= 109 && page <= 113 : page >= 44 && page <= 53), perk.id());
    }
    assertEquals(94, PerkCatalog.available(EnumSet.of(Book.CRB)).size());
    assertEquals(75, PerkCatalog.available(EnumSet.of(Book.WG)).size());
    assertEquals(21, perks.stream().filter(perk -> perk.requirements().rare()).count());
    assertEquals(0, PerkCatalog.available(EnumSet.of(Book.WOA)).size());
  }

  /** WG pp. 44–53: spot checks of ranks, requirements and the new requirement kinds. */
  @Test
  void wandererPerks() {
    PerkDefinition blocker = perk("blocker");
    assertEquals(3, blocker.ranks());
    assertEquals(Map.of(Attribute.STRENGTH, 6), blocker.requirements().attributes());
    assertEquals(List.of(1, 6, 11), List.of(blocker.requirements().levelForRank(1),
        blocker.requirements().levelForRank(2), blocker.requirements().levelForRank(3)));
    PerkDefinition cannibal = perk("cannibal");
    assertTrue(cannibal.requirements().notRobot());
    assertEquals(19, cannibal.requirements().levelForRank(2));
    assertEquals(List.of("super-mutant", "nightkin"), cannibal.requirements().firstRankLevelWaivedFor());
    assertEquals(List.of("ghoul"), perk("ghoulish").requirements().excludedOrigins());
    assertTrue(perk("radicool").requirements().notRadiationImmune());
    assertEquals(Map.of(Attribute.CHARISMA, 8, Attribute.INTELLIGENCE, 7),
        perk("responder").requirements().attributes());
    assertTrue(perk("evasive").drByAgility());
    assertEquals(1, perk("ironclad").wornArmorDrPerRank());
    assertEquals(1, perk("junk-shield").junkDrPerRank());
    assertEquals(1, perk("sturdy-frame").armorWeightQuartersPerRank());
    assertTrue(perk("pack-rat").junkWeightHalved());
    assertEquals("Butcher’s Bounty", perk("butchers-bounty").name(), "apostrophe as printed");
  }

  /** CRB p. 62: Cautious Nature and Daring Nature exclude each other; ids must exist. */
  @Test
  void excludedPerksExist() {
    for (PerkDefinition perk : PerkCatalog.all()) {
      for (String excluded : perk.requirements().excludedPerks()) {
        assertTrue(PerkCatalog.byId(excluded).isPresent(), perk.id() + " -> " + excluded);
      }
    }
    assertEquals(List.of("daring-nature"), perk("cautious-nature").requirements().excludedPerks());
    assertEquals(List.of("cautious-nature"), perk("daring-nature").requirements().excludedPerks());
  }

  /** CRB p. 66, 73: Intense Training chooses an attribute, Tag! a skill. */
  @Test
  void choicePerks() {
    assertEquals(PerkChoice.ATTRIBUTE, perk("intense-training").choice());
    assertEquals(10, perk("intense-training").ranks());
    assertEquals(PerkChoice.SKILL, perk("tag").choice());
    assertEquals(2, PerkCatalog.all().stream().filter(perk -> perk.choice() != null).count());
  }

  @Test
  void requirementsAreStructured() {
    PerkRequirements barbarian = perk("barbarian").requirements();
    assertEquals(Map.of(Attribute.STRENGTH, 7), barbarian.attributes());
    assertEquals(4, barbarian.level());
    assertTrue(barbarian.notRobot());
    assertEquals(PerkRequirements.NONE, perk("action-boy-girl").requirements());
  }

  /** CRB p. 60, 66, 70–73: the passive effects the builder computes. */
  @Test
  void effectFieldsMatchTheBook() {
    assertEquals(1, perk("life-giver").maxHpPerEndurance());
    assertEquals(25, perk("strong-back").carryWeightPerRank());
    assertEquals(1, perk("toughness").physicalDrPerRank());
    assertEquals(1, perk("refractor").energyDrPerRank());
    assertEquals(1, perk("rad-resistance").radiationDrPerRank());
    assertEquals(2, perk("snakeater").poisonDrPerRank());
    assertTrue(perk("barbarian").physicalDrByStrength());
    assertEquals(2, perk("skilled").skillPointsPerRank());
  }

  private static PerkDefinition perk(String id) {
    return PerkCatalog.byId(id).orElseThrow();
  }

  /** Plan C8: 18 perks change weapon values, 21 more show a hint with the weapons they can help. */
  @Test
  void weaponBonuses() {
    List<String> computed = PerkCatalog.all().stream()
        .filter(perk -> perk.weaponBonuses().stream().anyMatch(bonus -> !bonus.isHint())).map(PerkDefinition::id)
        .toList();
    List<String> hints = PerkCatalog.all().stream()
        .filter(perk -> perk.weaponBonuses().stream().anyMatch(PerkWeaponBonus::isHint)).map(PerkDefinition::id)
        .toList();
    assertEquals(18, computed.size(), computed.toString());
    assertEquals(21, hints.size(), hints.toString());
    PerkWeaponBonus ironFist = perk("iron-fist").weaponBonuses().getLast();
    assertEquals(2, ironFist.fromRank());
    assertEquals(List.of("Vicious"), ironFist.gainedEffects());
    PerkWeaponBonus commando = perk("commando").weaponBonuses().getFirst();
    assertEquals(2, commando.at(commando.damage(), 2));
    assertEquals(3, commando.weapons().minFireRate());
  }

  /** A misspelt name or placeholder in a weapon bonus would go unnoticed, so every one must be known. */
  @Test
  void weaponBonusNamesExist() {
    for (PerkDefinition perk : PerkCatalog.all()) {
      for (PerkWeaponBonus bonus : perk.weaponBonuses()) {
        for (String weapon : bonus.weapons().weapons()) {
          assertTrue(WeaponCatalog.byId(weapon).isPresent(), perk.id() + " -> " + weapon);
        }
        for (String quality : bonus.weapons().qualities()) {
          assertTrue(isQuality(quality), perk.id() + " -> " + quality);
        }
        for (String quality : bonus.removedQualities()) {
          assertTrue(isQuality(quality), perk.id() + " -> " + quality);
        }
        for (String effect : bonus.gainedEffects()) {
          assertTrue(DamageEffect.of(effect).isPresent(), perk.id() + " -> " + effect);
        }
        String hint = bonus.hintAt(1);
        assertTrue(hint == null || !hint.contains("{"), perk.id() + ": unknown placeholder in " + hint);
      }
    }
  }

  private static boolean isQuality(String name) {
    return Arrays.stream(WeaponQuality.values()).anyMatch(quality -> quality.displayName().equals(name));
  }

  @Test
  void weaponBonusIsEitherComputedOrHint() {
    assertThrows(IllegalArgumentException.class,
        () -> new PerkWeaponBonus(1, WeaponFilter.ANY, 1, 0, 0, false, List.of(), List.of(), "Text."));
    assertThrows(IllegalArgumentException.class,
        () -> new PerkWeaponBonus(1, WeaponFilter.ANY, 0, 0, 0, false, List.of(), List.of(), null));
    assertThrows(IllegalArgumentException.class,
        () -> new PerkWeaponBonus(2, WeaponFilter.ANY, 1, 0, 0, true, List.of(), List.of(), null));
    assertEquals(1, new PerkWeaponBonus(0, null, 0, 0, 0, false, List.of(), List.of(), "Text.").fromRank());
  }
}
