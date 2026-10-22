package de.fallout2d20.charbuilder.model.character.apparel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModSlot;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModTable;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Apparel values with mods (CRB pp. 126, 133–136). */
class ModdedApparelTest {

  private static WornApparel modded(String id, ApparelModSlot slot, String mod) {
    return ApparelMods.install(WornApparel.of(CatalogRef.of(id)), slot, ApparelCatalog.modById(mod).orElseThrow());
  }

  private static ModdedApparel values(WornApparel item) {
    return ModdedApparel.of(item).orElseThrow();
  }

  /** Metal Arm 2/1/0, 3 lbs., 15 caps (CRB p. 131) + Polished Metal +4/+4, +4 lbs., +40 caps (CRB p. 134). */
  @Test
  void materialAddsResistanceWeightAndCost() {
    ModdedApparel arm = values(modded("metal-arm", ApparelModSlot.MATERIAL, "metal-polished-metal"));
    assertEquals(List.of(6, 5, 0), List.of(arm.values().physical(), arm.values().energy(), arm.values().radiation()));
    assertEquals("7", arm.weight());
    assertEquals(55, arm.cost());
  }

  /** CRB p. 134: on torso armor a Material mod weighs and costs double (Metal Chest Piece 6 lbs., 40 caps). */
  @Test
  void torsoDoublesMaterialMods() {
    ModdedApparel chest = values(modded("metal-chest-piece", ApparelModSlot.MATERIAL, "metal-polished-metal"));
    assertEquals("14", chest.weight());
    assertEquals(120, chest.cost());
  }

  /** CRB p. 136: the torso-only rows are already doubled (Padded +4 lbs., +1 cap on Raider Chest Piece 7 lbs., 18). */
  @Test
  void torsoOnlyUpgradesAreNotDoubledAgain() {
    ModdedApparel chest = values(modded("raider-chest-piece", ApparelModSlot.UPGRADE, "upgrade-padded"));
    assertEquals("11", chest.weight());
    assertEquals(19, chest.cost());
    assertEquals(List.of("Padded: +2 to every damage resistance against Blast weapons"), chest.specials());
  }

  /** CRB p. 136: Asbestos Lining adds +3 Energy and a rule. */
  @Test
  void asbestosLining() {
    ModdedApparel chest = values(modded("leather-chest-piece", ApparelModSlot.UPGRADE, "upgrade-asbestos-lining"));
    ModdedApparel plain = ModdedApparel.unmodded(WornApparel.of(CatalogRef.of("leather-chest-piece"))).orElseThrow();
    assertEquals(plain.values().energy() + 3, chest.values().energy());
    assertEquals(1, chest.specials().size());
  }

  /** Casual Clothing 0/0/0, 20 caps (CRB p. 124) + Ballistic Weave Mk III +4/+4, +40 caps, no weight (CRB p. 126). */
  @Test
  void ballisticWeaveOnClothing() {
    ModdedApparel clothing = values(modded("casual-clothing", ApparelModSlot.BALLISTIC_WEAVE, "ballistic-weave-mk-iii"));
    assertEquals(List.of(4, 4), List.of(clothing.values().physical(), clothing.values().energy()));
    assertEquals("2", clothing.weight());
    assertEquals(60, clothing.cost(), "covers the torso, but the weave is not doubled");
  }

  /** A custom piece has no weight or cost of its own, only its mods'. */
  @Test
  void customPieceCountsOnlyItsModsWeight() {
    WornApparel plate = ApparelMods.install(WornApparel.of(new CustomApparel("Scrap arm", ApparelKind.ARMOR,
        Set.of(HitLocation.LEFT_ARM), 1, 1, 0, false, 0, ApparelModTable.RAIDER)), ApparelModSlot.MATERIAL,
        ApparelCatalog.modById("raider-buttressed").orElseThrow());
    ModdedApparel values = values(plate);
    assertEquals("3", values.weight());
    assertEquals(12, values.cost());
    assertEquals(5, values.values().physical());
    assertTrue(plate.values().isPresent());
  }

  /** WG p. 93: Shrouded weighs −1, doubled on the Wood Armor Chest Piece (4 lbs., 25 caps): 2 lbs., 45 caps. */
  @Test
  void shroudedMakesWoodArmorLighter() {
    ModdedApparel chest = values(modded("wood-armor-chest-piece", ApparelModSlot.MATERIAL, "wood-shrouded"));
    assertEquals("2", chest.weight());
    assertEquals(45, chest.cost());
    assertTrue(chest.shadowed());
  }

  /** WG p. 90: Marine Welded adds +1 to every resistance, radiation included (Marine Armor Arm 4/4/2). */
  @Test
  void marineMaterialAddsRadiation() {
    ModdedApparel arm = values(modded("marine-armor-arm", ApparelModSlot.MATERIAL, "marine-welded"));
    assertEquals(List.of(5, 5, 3), List.of(arm.values().physical(), arm.values().energy(), arm.values().radiation()));
  }

  /** WG p. 87: a Lining on the Underarmor Suit (30 caps) is not doubled although it covers the torso. */
  @Test
  void liningOnUnderarmorSuit() {
    ModdedApparel suit = values(modded("underarmor-suit", ApparelModSlot.LINING, "lining-protective-lining"));
    assertEquals(List.of(2, 2, 1), List.of(suit.values().physical(), suit.values().energy(),
        suit.values().radiation()));
    assertEquals(54, suit.cost());
    assertEquals("1", suit.weight());
    assertEquals(1, suit.specials().size());
  }
}
