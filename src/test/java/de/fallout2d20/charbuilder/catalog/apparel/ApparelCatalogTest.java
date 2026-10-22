package de.fallout2d20.charbuilder.catalog.apparel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.weapon.ModPerk;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Spot checks of the apparel catalog against CRB p. 124–132 and 146–147 and WG pp. 85–93. */
class ApparelCatalogTest {

  private static final Set<Book> CORE = Set.of(Book.CRB);
  private static final Set<Book> WANDERER = Set.of(Book.WG);

  @Test
  void containsAllCoreItemsPerKind() {
    Map<ApparelKind, Long> perKind = ApparelCatalog.all().stream().filter(item -> item.isAvailableWith(CORE))
        .collect(Collectors.groupingBy(ApparelData::kind, Collectors.counting()));
    assertEquals(Map.of(ApparelKind.CLOTHING, 7L, ApparelKind.OUTFIT, 12L, ApparelKind.HEADGEAR, 10L,
        ApparelKind.ARMOR, 56L, ApparelKind.ROBOT_PLATING, 2L, ApparelKind.ROBOT_ARMOR, 33L), perKind);
  }

  /** CRB p. 130. */
  @Test
  void raiderChestPiece() {
    assertEquals(new ApparelData("raider-chest-piece", "Raider Chest Piece", List.of(new Source(Book.CRB, 130)),
        ApparelKind.ARMOR, List.of(Coverage.TORSO), 1, 1, 0, false, "7", 18, 0, 0, null,
        List.of("Raider armor chest piece", "Raider armor torso"), ApparelModTable.RAIDER, true, false, null, 0),
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
    Map<ApparelModSlot, Long> perSlot = ApparelCatalog.mods().stream().filter(mod -> mod.isAvailableWith(CORE))
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
   * Every piece of the five CRB armor types takes its own table, Vault-Tec Security armor none (CRB pp. 133–136); the
   * clothing the books name takes Ballistic Weave (CRB pp. 125–128, WG p. 86).
   */
  @Test
  void modTables() {
    for (ApparelData item : ApparelCatalog.all()) {
      ApparelModTable table = item.modTable();
      if (item.kind() == ApparelKind.ARMOR && !item.id().startsWith("vault-tec") && item.isAvailableWith(CORE)) {
        assertTrue(table != null && item.id().contains(table.name().toLowerCase(Locale.ROOT)), item.id());
      }
    }
    assertNull(ApparelCatalog.byId("vault-tec-security-armor").orElseThrow().modTable());
    assertNull(ApparelCatalog.byId("vault-tec-security-helmet").orElseThrow().modTable());
    assertEquals(List.of("casual-clothing", "military-fatigues", "formal-clothing", "heavy-coat", "lab-coat",
        "formal-hat", "brotherhood-armored-battlecoat", "brotherhood-bomber-jacket"), ApparelCatalog.all().stream()
        .filter(item -> item.modTable() == ApparelModTable.BALLISTIC_WEAVE).map(ApparelData::id).toList());
  }

  /** CRB p. 122: Raider armor is the armor made to fit super mutants. */
  @Test
  void superMutantFit() {
    List<String> fitting = ApparelCatalog.all().stream().filter(ApparelData::superMutantFit).map(ApparelData::id)
        .toList();
    assertEquals(9, fitting.size());
    assertTrue(fitting.stream().allMatch(id -> id.contains("raider")));
  }

  /**
   * WG pp. 85–93: 4 headgear (the Hunter's Hood, printed under Outfits, user decision), 2 clothing, 5 outfits, 40
   * armor rows.
   */
  @Test
  void containsAllWanderersGuideItemsPerKind() {
    Map<ApparelKind, Long> perKind = ApparelCatalog.all().stream().filter(item -> item.isAvailableWith(WANDERER))
        .collect(Collectors.groupingBy(ApparelData::kind, Collectors.counting()));
    assertEquals(Map.of(ApparelKind.HEADGEAR, 4L, ApparelKind.CLOTHING, 2L, ApparelKind.OUTFIT, 5L,
        ApparelKind.ARMOR, 40L), perKind);
    ApparelData legatus = ApparelCatalog.byId("legatus-helmet").orElseThrow();
    assertEquals(List.of(5, 5, 3, 250, 5), List.of(legatus.physical(), legatus.energy(), legatus.radiation(),
        legatus.cost(), legatus.rarity()));
    assertEquals("16", legatus.weight());
    ApparelData pelt = ApparelCatalog.byId("hunters-pelt-outfit").orElseThrow();
    assertEquals(4, pelt.shadowedPieces());
    assertEquals(List.of("15", 50, 2), List.of(pelt.weight(), pelt.cost(), pelt.rarity()));
    assertNotNull(ApparelCatalog.byId("chinese-stealth-armor").orElseThrow().special());
    assertTrue(ApparelCatalog.byId("beer-hat").orElseThrow().isHat());
  }

  /**
   * WG pp. 87–93: 4 Linings, 18 Material mods; no armor without its own table but the three one-piece suits and the
   * Scout mask.
   */
  @Test
  void wanderersGuideModTables() {
    Map<ApparelModSlot, Long> perSlot = ApparelCatalog.mods().stream().filter(mod -> mod.isAvailableWith(WANDERER))
        .collect(Collectors.groupingBy(ApparelModData::slot, Collectors.counting()));
    assertEquals(Map.of(ApparelModSlot.LINING, 4L, ApparelModSlot.MATERIAL, 18L), perSlot);
    assertEquals(-1, ApparelCatalog.modById("wood-shrouded").orElseThrow().weight());
    assertEquals(4, ApparelCatalog.modById("marine-buttressed").orElseThrow().radiation());
    List<String> withoutTable = ApparelCatalog.all().stream()
        .filter(item -> item.isAvailableWith(WANDERER) && item.kind() == ApparelKind.ARMOR && item.modTable() == null)
        .map(ApparelData::id).toList();
    assertEquals(List.of("chinese-stealth-armor", "diving-suit", "recon-armor", "scout-armor-mask"), withoutTable);
    assertEquals(ApparelModTable.LINING, ApparelCatalog.byId("underarmor-suit").orElseThrow().modTable());
  }

  /** Every table has mods; WG tables are on offer only with the Wanderer's Guide. */
  @Test
  void fittingTablesFollowTheBooks() {
    for (ApparelModTable table : ApparelModTable.values()) {
      assertTrue(ApparelCatalog.mods().stream().anyMatch(mod -> mod.table() == table), table.name());
    }
    assertEquals(List.of(ApparelModTable.RAIDER, ApparelModTable.LEATHER, ApparelModTable.METAL,
        ApparelModTable.COMBAT, ApparelModTable.SYNTH), ApparelModTable.fitting(ApparelKind.ARMOR, CORE));
    assertEquals(List.of(ApparelModTable.RAIDER, ApparelModTable.LEATHER, ApparelModTable.METAL,
        ApparelModTable.COMBAT, ApparelModTable.SYNTH, ApparelModTable.LEGION, ApparelModTable.MARINE,
        ApparelModTable.SCOUT, ApparelModTable.ROBOT, ApparelModTable.WOOD),
        ApparelModTable.fitting(ApparelKind.ARMOR, Set.of(Book.CRB, Book.WG)));
    assertEquals(List.of(ApparelModTable.BALLISTIC_WEAVE), ApparelModTable.fitting(ApparelKind.CLOTHING, CORE));
    assertEquals(List.of(ApparelModTable.BALLISTIC_WEAVE, ApparelModTable.LINING),
        ApparelModTable.fitting(ApparelKind.CLOTHING, Set.of(Book.CRB, Book.WG)));
  }
}
