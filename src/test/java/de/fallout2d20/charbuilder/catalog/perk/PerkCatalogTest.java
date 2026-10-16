package de.fallout2d20.charbuilder.catalog.perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.model.character.Attribute;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PerkCatalogTest {

  /** CRB p. 59–73: 94 perks, all from the Core Rulebook. */
  @Test
  void loadsAllCorePerks() {
    List<PerkDefinition> perks = PerkCatalog.all();
    assertEquals(94, perks.size());
    assertEquals("action-boy-girl", perks.getFirst().id());
    assertEquals("toughness", perks.get(93).id());
    for (PerkDefinition perk : perks) {
      assertEquals(1, perk.sources().size(), perk.id());
      assertEquals(Book.CRB, perk.sources().getFirst().book(), perk.id());
      int page = perk.sources().getFirst().page();
      assertTrue(page >= 59 && page <= 73, perk.id());
    }
    assertEquals(94, PerkCatalog.available(EnumSet.of(Book.CRB)).size());
    assertEquals(0, PerkCatalog.available(EnumSet.of(Book.WOA)).size());
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
}
