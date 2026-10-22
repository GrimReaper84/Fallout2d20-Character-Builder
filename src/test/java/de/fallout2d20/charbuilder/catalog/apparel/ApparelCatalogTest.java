package de.fallout2d20.charbuilder.catalog.apparel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.weapon.ModPerk;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Spot checks of the apparel catalog against CRB p. 124–132 and 146–147. */
class ApparelCatalogTest {

  @Test
  void containsAllCoreItemsPerKind() {
    Map<ApparelKind, Long> perKind = ApparelCatalog.all().stream()
        .collect(Collectors.groupingBy(ApparelData::kind, Collectors.counting()));
    assertEquals(Map.of(ApparelKind.CLOTHING, 7L, ApparelKind.OUTFIT, 12L, ApparelKind.HEADGEAR, 10L,
        ApparelKind.ARMOR, 56L, ApparelKind.ROBOT_PLATING, 2L, ApparelKind.ROBOT_ARMOR, 33L), perKind);
  }

  /** CRB p. 130. */
  @Test
  void raiderChestPiece() {
    assertEquals(new ApparelData("raider-chest-piece", "Raider Chest Piece", List.of(new Source(Book.CRB, 130)),
        ApparelKind.ARMOR, List.of(Coverage.TORSO), 1, 1, 0, false, "7", 18, 0, 0, null,
        List.of("Raider armor chest piece", "Raider armor torso"), ApparelModTable.RAIDER, true),
        ApparelCatalog.byId("raider-chest-piece").orElseThrow());
    assertTrue(ApparelCatalog.byId("raider-arm").orElseThrow().needsLocation());
  }

  /** CRB p. 124: radiation "Immune"; CRB p. 146–147: plating and robot armor values. */
  @Test
  void hazmatSuitAndRobotArmor() {
    ApparelData hazmat = ApparelCatalog.byId("hazmat-suit").orElseThrow();
    assertTrue(hazmat.radiationImmune());
    assertEquals(List.of(Coverage.HEAD, Coverage.ARMS, Coverage.LEGS, Coverage.TORSO), hazmat.coverage());
    ApparelData gutsy = ApparelCatalog.byId("mister-gutsy-plating").orElseThrow();
    assertEquals(-10, gutsy.carryWeight());
    assertNull(gutsy.cost());
    ApparelData storage = ApparelCatalog.byId("factory-storage-armor-main-body").orElseThrow();
    assertEquals(20, storage.carryWeight());
    assertEquals("Armorer 1", storage.perk());
    assertEquals(List.of(Coverage.MAIN_BODY), storage.coverage());
  }

  /** CRB p. 146: hats are the only headgear a Mister Handy may wear. */
  @Test
  void hats() {
    assertEquals("Brotherhood Scribe’s Hat", ApparelCatalog.byId("brotherhood-scribes-hat").orElseThrow().name());
    assertTrue(ApparelCatalog.byId("casual-hat").orElseThrow().isHat());
    assertFalse(ApparelCatalog.byId("army-helmet").orElseThrow().isHat());
  }

  /** CRB p. 126 (5 Ballistic Weave), pp. 133–135 (22 Material mods), p. 136 (15 Armor Upgrades). */
  @Test
  void containsAllCoreArmorMods() {
    Map<ApparelModSlot, Long> perSlot = ApparelCatalog.mods().stream()
        .collect(Collectors.groupingBy(ApparelModData::slot, Collectors.counting()));
    assertEquals(Map.of(ApparelModSlot.BALLISTIC_WEAVE, 5L, ApparelModSlot.MATERIAL, 22L, ApparelModSlot.UPGRADE, 15L),
        perSlot);
    ApparelModData polished = ApparelCatalog.modById("metal-polished-metal").orElseThrow();
    assertEquals(List.of(4, 4, 0, 4, 40), List.of(polished.physical(), polished.energy(), polished.radiation(),
        polished.weight(), polished.cost()));
    assertEquals(List.of(new ModPerk("armorer", 2)), polished.perks());
    ApparelModData bioCommMesh = ApparelCatalog.modById("upgrade-biocommmesh").orElseThrow();
    assertEquals(ModPlacement.TORSO, bioCommMesh.placement());
    assertEquals(List.of(new ModPerk("armorer", 4), new ModPerk("science", 2)), bioCommMesh.perks());
    assertTrue(ApparelCatalog.modById("combat-shadowed").orElseThrow().shadowed());
    assertNull(ApparelCatalog.modById("ballistic-weave-mk-v").orElseThrow().weight());
  }

  /**
   * Every piece of the five armor types takes its own table, Vault-Tec Security armor none (CRB pp. 133–136); the
   * clothing the book names takes Ballistic Weave (CRB pp. 125–128).
   */
  @Test
  void modTables() {
    for (ApparelData item : ApparelCatalog.all()) {
      ApparelModTable table = item.modTable();
      if (item.kind() == ApparelKind.ARMOR && !item.id().startsWith("vault-tec")) {
        assertTrue(table != null && item.id().contains(table.name().toLowerCase(Locale.ROOT)), item.id());
      }
    }
    assertNull(ApparelCatalog.byId("vault-tec-security-armor").orElseThrow().modTable());
    assertNull(ApparelCatalog.byId("vault-tec-security-helmet").orElseThrow().modTable());
    assertEquals(List.of("casual-clothing", "military-fatigues", "formal-clothing", "heavy-coat", "lab-coat",
        "formal-hat"), ApparelCatalog.all().stream().filter(item -> item.modTable() == ApparelModTable.BALLISTIC_WEAVE)
        .map(ApparelData::id).toList());
  }

  /** CRB p. 122: Raider armor is the armor made to fit super mutants. */
  @Test
  void superMutantFit() {
    List<String> fitting = ApparelCatalog.all().stream().filter(ApparelData::superMutantFit).map(ApparelData::id)
        .toList();
    assertEquals(9, fitting.size());
    assertTrue(fitting.stream().allMatch(id -> id.contains("raider")));
  }
}
