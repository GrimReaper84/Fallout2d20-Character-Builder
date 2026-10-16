package de.fallout2d20.charbuilder.catalog.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Spot checks of the item and magazine catalogs against CRB pp. 149–187. */
class ItemCatalogTest {

  @Test
  void containsEveryTableRow() {
    Map<ItemCategory, Long> perCategory = ItemCatalog.all().stream()
        .collect(Collectors.groupingBy(ItemData::category, Collectors.counting()));
    assertEquals(Map.of(ItemCategory.FOOD, 75L, ItemCategory.BEVERAGE, 22L, ItemCategory.CHEM, 32L,
        ItemCategory.OTHER_CONSUMABLE, 3L, ItemCategory.TOOL, 19L, ItemCategory.ROBOT_MODULE, 13L), perCategory);
    assertEquals(164, ItemCatalog.available(EnumSet.of(Book.CRB)).size());
  }

  /** CRB p. 165: Stimpak heals 4 HP, instant, not addictive, <1 lb., 50 caps, rarity 2. */
  @Test
  void stimpakAndJet() {
    ItemData stimpak = ItemCatalog.byId("stimpak").orElseThrow();
    assertEquals(List.of(new Source(Book.CRB, 165)), stimpak.sources());
    assertEquals(ChemDuration.INSTANT, stimpak.duration());
    assertNull(stimpak.addictive());
    assertEquals("<1", stimpak.weight());
    assertEquals(50, stimpak.cost());
    assertEquals(2, stimpak.rarity());
    ItemData jet = ItemCatalog.byId("jet").orElseThrow();
    assertEquals(ChemDuration.BRIEF, jet.duration());
    assertEquals(2, jet.addictive());
  }

  /** CRB pp. 149, 160: HP healed and irradiation of food and beverages. */
  @Test
  void foodAndBeverages() {
    ItemData cola = ItemCatalog.byName("Nuka-Cola").orElseThrow();
    assertEquals(ItemCategory.BEVERAGE, cola.category());
    assertEquals(2, cola.hpHealed());
    assertTrue(cola.irradiated());
    ItemData pie = ItemCatalog.byId("perfectly-preserved-pie").orElseThrow();
    assertEquals(5, pie.hpHealed());
    assertFalse(pie.irradiated());
    assertEquals("nuka-cherry", ItemCatalog.byName("Nuka-Cherrys").orElseThrow().id(), "pack wording");
  }

  /** CRB p. 181: backpacks raise carry weight per STR; CRB p. 185: robot modules lower a robot's carry weight. */
  @Test
  void carryWeightEffects() {
    assertEquals(10, ItemCatalog.byId("backpack-large").orElseThrow().carryWeightPerStrength());
    assertEquals(5, ItemCatalog.byId("backpack-small").orElseThrow().carryWeightPerStrength());
    assertEquals(-15, ItemCatalog.byId("sensor-array").orElseThrow().robotCarryWeight());
    assertEquals(0, ItemCatalog.byId("diagnosis-mod").orElseThrow().robotCarryWeight());
    assertEquals("Robotics Expert 2, Science! 1", ItemCatalog.byId("tesla-coils").orElseThrow().perks());
  }

  /** CRB pp. 172–180: 20 publications; 10 with issue tables (85 issues). */
  @Test
  void magazines() {
    List<MagazineData> magazines = ItemCatalog.magazines();
    assertEquals(20, magazines.size());
    assertEquals(10, magazines.stream().filter(magazine -> !magazine.issues().isEmpty()).count());
    assertEquals(85, magazines.stream().mapToInt(magazine -> magazine.issues().size()).sum());
    MagazineData grognak = ItemCatalog.magazineById("grognak-the-barbarian").orElseThrow();
    assertEquals(10, grognak.issues().size());
    assertEquals("1-2", grognak.issues().get(0).roll());
    assertEquals("The Lair of the Virgin Eaters", grognak.issue("The Lair of the Virgin Eaters").orElseThrow().title());
    assertEquals(5, ItemCatalog.magazineById("tumblers-today").orElseThrow().issues().size());
    assertTrue(ItemCatalog.magazineById("la-fantoma").orElseThrow().issues().isEmpty());
    assertTrue(ItemCatalog.byId("la-fantoma").isEmpty(), "magazines are not items");
  }
}
