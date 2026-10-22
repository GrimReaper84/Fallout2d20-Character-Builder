package de.fallout2d20.charbuilder.model.character.apparel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModSlot;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModTable;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Which mods fit which apparel (CRB pp. 87, 125–128, 133–136). */
class ApparelModsTest {

  private static final Set<Book> CORE = Set.of(Book.CRB);

  private static WornApparel piece(String id) {
    return WornApparel.of(CatalogRef.of(id));
  }

  private static ApparelModData mod(String id) {
    return ApparelCatalog.modById(id).orElseThrow();
  }

  private static List<String> options(WornApparel item, ApparelModSlot slot) {
    return ApparelMods.options(item, slot, CORE).stream().map(ApparelModData::id).toList();
  }

  /** CRB p. 87: an armor piece takes a Material and an Upgrade; helmets only a Material (CRB p. 134). */
  @Test
  void slots() {
    assertEquals(List.of(ApparelModSlot.MATERIAL, ApparelModSlot.UPGRADE), ApparelMods.slots(piece("metal-arm")));
    assertEquals(List.of(ApparelModSlot.MATERIAL), ApparelMods.slots(piece("combat-helmet")));
    assertEquals(List.of(), ApparelMods.slots(piece("vault-tec-security-armor")), "CRB p. 136: no upgrades, no table");
    assertEquals(List.of(ApparelModSlot.BALLISTIC_WEAVE), ApparelMods.slots(piece("lab-coat")),
        "an outfit the book allows weave on (CRB p. 128)");
    assertEquals(List.of(), ApparelMods.slots(piece("army-helmet")));
    assertEquals(List.of(), ApparelMods.slots(piece("hazmat-suit")));
  }

  /** Material mods come from the piece's own table (CRB pp. 133–135). */
  @Test
  void materialsOfTheOwnTable() {
    assertEquals(List.of("leather-boiled-leather", "leather-girded-leather", "leather-treated-leather",
        "leather-shadowed-leather", "leather-studded-leather"), options(piece("leather-leg"), ApparelModSlot.MATERIAL));
    assertFalse(ApparelMods.fits(piece("leather-leg"), ApparelModSlot.MATERIAL, mod("metal-painted-metal")));
  }

  /** CRB p. 136: all-location upgrades everywhere, the others only on their location. */
  @Test
  void upgradesByLocation() {
    assertEquals(List.of("upgrade-laminated", "upgrade-resin", "upgrade-microcarbon", "upgrade-cushioned",
        "upgrade-muffled"), options(piece("leather-leg"), ApparelModSlot.UPGRADE));
    assertEquals(List.of("upgrade-laminated", "upgrade-resin", "upgrade-microcarbon", "upgrade-padded",
        "upgrade-asbestos-lining", "upgrade-dense", "upgrade-biocommmesh", "upgrade-pneumatic"),
        options(piece("raider-chest-piece"), ApparelModSlot.UPGRADE));
    assertTrue(ApparelMods.fits(piece("synth-arm"), ApparelModSlot.UPGRADE, mod("upgrade-brawling")));
    assertFalse(ApparelMods.fits(piece("synth-arm"), ApparelModSlot.UPGRADE, mod("upgrade-padded")));
  }

  @Test
  void installChecksTheFit() {
    WornApparel arm = ApparelMods.install(piece("metal-arm"), ApparelModSlot.MATERIAL, mod("metal-polished-metal"));
    assertEquals(List.of(mod("metal-polished-metal")), ApparelMods.installedMods(arm));
    assertThrows(IllegalArgumentException.class,
        () -> ApparelMods.install(arm, ApparelModSlot.UPGRADE, mod("upgrade-padded")));
    assertEquals(List.of(), ApparelMods.installedMods(ApparelMods.install(arm, ApparelModSlot.MATERIAL, null)));
  }

  /** Unknown ids and mods stored under a slot they do not fit (hand-edited file) have no effect. */
  @Test
  void installedModsSkipUnknownAndMisfits() {
    WornApparel arm = piece("metal-arm").withMod(ApparelModSlot.MATERIAL, CatalogRef.of("no-such-mod"))
        .withMod(ApparelModSlot.UPGRADE, CatalogRef.of("upgrade-padded"));
    assertEquals(List.of(), ApparelMods.installedMods(arm));
    assertEquals(2, arm.mods().size(), "kept in the file");
  }

  /** User decision 2026-10-22: a custom piece takes the mods of the table chosen for it. */
  @Test
  void customPieceWithTable() {
    WornApparel shirt = WornApparel.of(new CustomApparel("Lined shirt", ApparelKind.CLOTHING,
        Set.of(HitLocation.TORSO, HitLocation.LEFT_ARM, HitLocation.RIGHT_ARM), 0, 0, 0, false, 0,
        ApparelModTable.BALLISTIC_WEAVE));
    assertEquals(List.of(ApparelModSlot.BALLISTIC_WEAVE), ApparelMods.slots(shirt));
    WornApparel plate = WornApparel.of(new CustomApparel("Scrap plate", ApparelKind.ARMOR,
        Set.of(HitLocation.TORSO, HitLocation.LEFT_ARM), 2, 1, 0, false, 0, ApparelModTable.RAIDER));
    assertTrue(options(plate, ApparelModSlot.UPGRADE).contains("upgrade-padded"), "covers the torso");
    assertFalse(options(plate, ApparelModSlot.UPGRADE).contains("upgrade-brawling"), "not only arms");
    assertThrows(IllegalArgumentException.class, () -> new CustomApparel("Wrong", ApparelKind.ARMOR,
        Set.of(HitLocation.HEAD), 0, 0, 0, false, 0, ApparelModTable.BALLISTIC_WEAVE));
  }

  /** Changing the table of a custom piece removes mods that no longer fit. */
  @Test
  void changingTheTableRemovesMisfits() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addApparel(WornApparel.of(new CustomApparel("Scrap leg", ApparelKind.ARMOR,
        Set.of(HitLocation.LEFT_LEG), 1, 1, 0, false, 0, ApparelModTable.RAIDER)));
    character.gear().setApparelMod(0, ApparelModSlot.MATERIAL, mod("raider-welded"));
    character.gear().setApparelMod(0, ApparelModSlot.UPGRADE, mod("upgrade-cushioned"));
    character.gear().setCustomApparelModTable(0, ApparelModTable.METAL);
    assertEquals(Set.of(ApparelModSlot.UPGRADE), character.gear().getApparel().getFirst().mods().keySet());
    character.gear().addApparel(piece("metal-leg"));
    assertThrows(IllegalStateException.class, () -> character.gear().setCustomApparelModTable(1, null),
        "only a custom piece chooses its table");
  }

  /**
   * WG pp. 87–93: Legion armor takes Material + Upgrade, its helmets only Material (user decision 2026-10-22); Wood
   * armor only Material (user decision 2026-10-22); the Scout mask nothing; the Underarmor Suit a Lining; the
   * Brotherhood Battlecoat Ballistic Weave.
   */
  @Test
  void wanderersGuideSlots() {
    assertEquals(List.of(ApparelModSlot.MATERIAL, ApparelModSlot.UPGRADE),
        ApparelMods.slots(piece("legionary-chest-piece")));
    assertEquals(List.of(ApparelModSlot.MATERIAL), ApparelMods.slots(piece("legatus-helmet")));
    assertEquals(List.of(ApparelModSlot.MATERIAL), ApparelMods.slots(piece("wood-armor-chest-piece")));
    assertEquals(List.of(), ApparelMods.slots(piece("scout-armor-mask")));
    assertEquals(List.of(ApparelModSlot.LINING), ApparelMods.slots(piece("underarmor-suit")));
    assertEquals(List.of(ApparelModSlot.BALLISTIC_WEAVE), ApparelMods.slots(piece("brotherhood-armored-battlecoat")));
    Set<Book> wanderer = Set.of(Book.CRB, Book.WG);
    assertEquals(List.of("marine-welded", "marine-tempered", "marine-hardened", "marine-buttressed"),
        ApparelMods.options(piece("marine-armor-leg"), ApparelModSlot.MATERIAL, wanderer).stream()
            .map(ApparelModData::id).toList());
    assertEquals(List.of(), ApparelMods.options(piece("marine-armor-leg"), ApparelModSlot.MATERIAL, CORE),
        "WG mods need the book");
    assertTrue(ApparelMods.fits(piece("robot-armor-arm"), ApparelModSlot.UPGRADE, mod("upgrade-brawling")));
  }
}
