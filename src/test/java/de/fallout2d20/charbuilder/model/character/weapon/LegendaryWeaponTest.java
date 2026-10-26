package de.fallout2d20.charbuilder.model.character.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Legendary weapon properties on the weapon values (WG pp. 96–103), applied after the mods. */
class LegendaryWeaponTest {

  /** WG p. 103: Two-Shot +2 CD, Vicious, Ammo-Hungry (2); WG p. 96: cost ×5; the name gets the mark. */
  @Test
  void twoShot() {
    ModdedWeapon rifle = legendary("hunting-rifle", "two-shot");
    assertEquals(8, rifle.damage());
    assertEquals(List.of("Piercing 1", "Vicious"), rifle.damageEffects());
    assertEquals(List.of("Two-Handed", "Ammo-Hungry (2)"), rifle.qualities());
    assertEquals(275, rifle.cost());
    assertEquals("★ Two-Shot Hunting Rifle", rifle.moddedName());
    assertEquals("Hunting Rifle", rifle.name());
    assertEquals("legendary-weapon-two-shot", rifle.legendary().id());
  }

  /** The mark and property go before the mods' prefixes; the base cost ×5, the mod's cost added once. */
  @Test
  void withMods() {
    CarriedWeapon pistol = CarriedWeapon.of(CatalogRef.of("10mm-pistol"))
        .withMod(ModSlot.RECEIVER, CatalogRef.of("small-guns-hardened"))
        .withLegendary(CatalogRef.of("legendary-weapon-powerful"));
    ModdedWeapon modded = ModdedWeapon.of(pistol).orElseThrow();
    assertEquals("★ Powerful Hardened 10mm Pistol", modded.moddedName());
    assertEquals(7, modded.damage());
    assertEquals(50 * 5 + 20, modded.cost());
  }

  /** WG p. 102: Rapid +3 Fire Rate, also above 6. */
  @Test
  void rapid() {
    assertEquals(9, legendary("gatling-laser", "rapid").fireRate());
    assertEquals(5, legendary("10mm-pistol", "rapid").fireRate());
  }

  /** WG p. 102: Penetrating gives Piercing 2, or +2 to an existing rating. */
  @Test
  void penetrating() {
    assertEquals(List.of("Piercing 3"), legendary("hunting-rifle", "penetrating").damageEffects());
    assertEquals(List.of("Piercing 2"), legendary("10mm-pistol", "penetrating").damageEffects());
  }

  /** WG p. 103: Violent +2 CD and Recoil (7), or +1 to an existing Recoil rating. */
  @Test
  void violent() {
    ModdedWeapon pistol = legendary("12-7mm-pistol", "violent");
    assertEquals(8, pistol.damage());
    assertEquals(List.of("Close Quarters", "Recoil (8)"), pistol.qualities());
    assertEquals(List.of("Close Quarters", "Reliable", "Recoil (7)"), legendary("10mm-pistol", "violent").qualities());
  }

  /** WG p. 102: Plasma Infused +2 CD, Physical and Energy damage. */
  @Test
  void plasmaInfused() {
    ModdedWeapon pistol = legendary("10mm-pistol", "plasma-infused");
    assertEquals(6, pistol.damage());
    assertEquals("Physical/Energy", pistol.damageType());
  }

  /** WG pp. 101–103: the damage effects gained, with their damage type. */
  @Test
  void gainedEffects() {
    assertEquals(List.of("Piercing 1", "Persistent (Energy)"), legendary("machete", "incendiary").damageEffects());
    assertEquals(List.of("Persistent (Poison)"), legendary("10mm-pistol", "poisoners").damageEffects());
    assertEquals(List.of("Radioactive"), legendary("10mm-pistol", "irradiated").damageEffects());
  }

  /** A property that changes no value keeps the values but marks the name and multiplies the cost. */
  @Test
  void ruleOnlyProperty() {
    ModdedWeapon pistol = legendary("10mm-pistol", "assassins");
    assertEquals(4, pistol.damage());
    assertEquals("★ Assassin's 10mm Pistol", pistol.moddedName());
    assertEquals(250, pistol.cost());
  }

  /** An id the catalog does not know has no effect. */
  @Test
  void unknownProperty() {
    ModdedWeapon pistol = ModdedWeapon.of(CarriedWeapon.of(CatalogRef.of("10mm-pistol"))
        .withLegendary(CatalogRef.of("gone"))).orElseThrow();
    assertEquals("10mm Pistol", pistol.moddedName());
    assertEquals(50, pistol.cost());
    assertNull(pistol.legendary());
  }

  private static ModdedWeapon legendary(String weapon, String property) {
    return ModdedWeapon.of(CarriedWeapon.of(CatalogRef.of(weapon))
        .withLegendary(CatalogRef.of("legendary-weapon-" + property))).orElseThrow();
  }
}
