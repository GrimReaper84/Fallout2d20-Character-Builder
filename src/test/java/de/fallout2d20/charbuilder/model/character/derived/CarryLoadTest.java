package de.fallout2d20.charbuilder.model.character.derived;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModSlot;
import de.fallout2d20.charbuilder.catalog.item.ItemCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.CustomItem;
import de.fallout2d20.charbuilder.model.character.item.Materials;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Carried weight and encumbrance (CRB p. 87). */
class CarryLoadTest {

  /** CRB p. 87: up to the carry weight no penalty; above +1; 50 or more over +2; twice the carry weight immobile. */
  @Test
  void thresholds() {
    assertEquals(new CarryLoad(200, 200, 0, false), CarryLoad.of(200, 200));
    assertEquals(new CarryLoad(201, 200, 1, false), CarryLoad.of(201, 200));
    assertEquals(1, CarryLoad.of(249, 200).penalty());
    assertEquals(2, CarryLoad.of(250, 200).penalty());
    assertEquals(3, CarryLoad.of(300, 200).penalty());
    assertTrue(CarryLoad.of(400, 200).immobile());
    assertFalse(CarryLoad.of(399, 200).immobile());
    assertFalse(CarryLoad.of(200, 200).isOverEncumbered());
  }

  @Test
  void readsPrintedWeights() {
    assertEquals(0, Weight.pounds(null));
    assertEquals(0, Weight.pounds("<1"));
    assertEquals(0, Weight.pounds("–"));
    assertEquals(16, Weight.pounds("16"));
    assertThrows(IllegalArgumentException.class, () -> Weight.pounds("1.5"));
  }

  /** Every printed weight of the catalogs is readable. */
  @Test
  void everyCatalogWeightIsReadable() {
    assertDoesNotThrow(() -> {
      WeaponCatalog.all().forEach(weapon -> Weight.pounds(weapon.weight()));
      WeaponCatalog.ammunition().forEach(ammunition -> Weight.pounds(ammunition.weight()));
      ApparelCatalog.all().forEach(item -> Weight.pounds(item.weight()));
      ItemCatalog.all().forEach(item -> Weight.pounds(item.weight()));
    });
  }

  /** Sledgehammer 12, Leather Chest Piece 3, 2 × Purified Water <1, Multi-Tool 1, custom 4 × 2, junk 2 × 2, 1 material. */
  @Test
  void sumsEverythingCarried() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("sledgehammer")));
    character.gear().addApparel(WornApparel.of(CatalogRef.of("leather-chest-piece")));
    character.gear().addItem(CarriedItem.of(CatalogRef.of("purified-water")).withQuantity(2));
    character.gear().addItem(CarriedItem.of(CatalogRef.of("multi-tool")));
    character.gear().addItem(CarriedItem.of(new CustomItem("Brick", 4, "")).withQuantity(2));
    character.gear().setMaterials(new Materials(2, 1, 0, 0));
    int sledgehammer = Weight.pounds(WeaponCatalog.byId("sledgehammer").orElseThrow().weight());
    int leather = Weight.pounds(ApparelCatalog.byId("leather-chest-piece").orElseThrow().weight());
    CarryLoad load = CarryLoad.of(character);
    assertEquals(sledgehammer + leather + 1 + 8 + 5, load.carried());
    assertEquals(200, load.carryWeight(), "150 + 10 × STR 5");
  }

  /** CRB p. 181: a backpack adds 5 or 10 × STR; only the best one counts. */
  @Test
  void backpackRaisesCarryWeight() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.gear().addItem(CarriedItem.of(CatalogRef.of("backpack-small")));
    assertEquals(225, DerivedStats.of(character).carryWeight());
    character.gear().addItem(CarriedItem.of(CatalogRef.of("backpack-large")));
    assertEquals(250, DerivedStats.of(character).carryWeight());
  }

  /** CRB p. 185: robot modules lower a robot's fixed carry weight (Mister Handy 150, CRB p. 55); backpacks do not count. */
  @Test
  void robotModulesLowerARobotsCarryWeight() {
    PlayerCharacter handy = PlayerCharacter.createNew();
    handy.chooseOrigin(CatalogRef.of("mister-handy"));
    handy.gear().addItem(CarriedItem.of(CatalogRef.of("sensor-array")));
    handy.gear().addItem(CarriedItem.of(CatalogRef.of("diagnosis-mod")));
    handy.gear().addItem(CarriedItem.of(CatalogRef.of("backpack-large")));
    assertEquals(150 - 15, DerivedStats.of(handy).carryWeight());
  }

  /** D4: a mod's weight counts (10mm Pistol 4 + Powerful receiver +1). */
  @Test
  void modsAddWeight() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    character.gear().setWeaponMod(0, ModSlot.RECEIVER, WeaponCatalog.modById("small-guns-powerful").orElseThrow());
    assertEquals(5, CarryLoad.of(character).carried());
  }

  /** Mods add their weight, doubled on torso armor (CRB pp. 133–134): Metal Chest Piece 6 + Polished Metal 2 × 4. */
  @Test
  void apparelModsAddWeight() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addApparel(WornApparel.of(CatalogRef.of("metal-chest-piece")));
    assertEquals(6, CarryLoad.of(character).carried());
    character.gear().setApparelMod(0, ApparelModSlot.MATERIAL, ApparelCatalog.modById("metal-polished-metal").orElseThrow());
    assertEquals(14, CarryLoad.of(character).carried());
  }

  private static PlayerCharacter withPerk(String perk, int rank) {
    return PlayerCharacter.builder().origin(CatalogRef.of("survivor"))
        .perks(List.of(new PerkSelection(CatalogRef.of(perk), rank, 0, List.of(), List.of()))).build();
  }

  /** WG p. 45 Sturdy Frame rank 2: worn armor counts half; carried armor and clothing count fully. */
  @Test
  void sturdyFrameLightensWornArmor() {
    PlayerCharacter character = withPerk("sturdy-frame", 2);
    character.gear().addApparel(WornApparel.of(CatalogRef.of("metal-chest-piece")));
    character.gear().addApparel(WornApparel.of(CatalogRef.of("metal-leg")).withLocation(HitLocation.LEFT_LEG));
    assertEquals(4, CarryLoad.of(character).carried(), "(6 + 3) / 2, rounded down");
    character.gear().addApparel(WornApparel.of(CatalogRef.of("tough-clothing")));
    character.gear().addApparel(WornApparel.of(CatalogRef.of("raider-chest-piece")));
    assertTrue(character.gear().getApparel().getLast().carried(), "a second chest piece starts carried");
    assertEquals(4 + 3 + 7, CarryLoad.of(character).carried());
  }

  /** Cage Armor is an outfit, but counts as armor for Sturdy Frame (user decision 2026-10-22). */
  @Test
  void sturdyFrameCountsArmorOutfits() {
    PlayerCharacter character = withPerk("sturdy-frame", 3);
    character.gear().addApparel(WornApparel.of(CatalogRef.of("cage-armor")));
    assertEquals(33 / 4, CarryLoad.of(character).carried(), "a quarter of 33 lbs., rounded down");
  }

  /** WG p. 45 Pack Rat: junk (2 lbs. each, CRB p. 208) counts half. */
  @Test
  void packRatHalvesJunk() {
    PlayerCharacter character = withPerk("pack-rat", 1);
    character.gear().setMaterials(new Materials(4, 1, 0, 0));
    assertEquals(4 + 1, CarryLoad.of(character).carried());
  }

  /** WG p. 45 Pack Rat also halves the junk items of WG pp. 120–125 (user decision), their half rounded down. */
  @Test
  void packRatHalvesJunkItems() {
    PlayerCharacter character = withPerk("pack-rat", 1);
    character.gear().addItem(CarriedItem.of(CatalogRef.of("bowling-ball")));
    character.gear().addItem(CarriedItem.of(CatalogRef.of("paint")));
    assertEquals((8 + 5) / 2, CarryLoad.of(character).carried(), "13 lbs. of junk items count 6");
    PlayerCharacter without = withPerk("toughness", 1);
    without.gear().addItem(CarriedItem.of(CatalogRef.of("bowling-ball")));
    assertEquals(8, CarryLoad.of(without).carried());
  }

  @Test
  void initiativeDropsByThePenaltyAndIsZeroWhenImmobile() {
    assertEquals(12, CarryLoad.of(200, 200).initiative(12));
    assertEquals(10, CarryLoad.of(260, 200).initiative(12));
    assertEquals(0, CarryLoad.of(300, 100).initiative(12));
    assertEquals(0, CarryLoad.of(190, 100).initiative(1), "never below 0");
  }
}
