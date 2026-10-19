package de.fallout2d20.charbuilder.model.character.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Which mods fit which weapon (CRB p. 87 and the weapon descriptions pp. 96–119). */
class WeaponModsTest {

  private static final Set<Book> ALL_BOOKS = EnumSet.allOf(Book.class);

  /** CRB p. 96: the 10mm Pistol's slots (its Magazine mods have no table row and are left out). */
  @Test
  void tenMillimeterPistolSlots() {
    assertEquals(List.of(ModSlot.RECEIVER, ModSlot.BARREL, ModSlot.GRIP, ModSlot.SIGHTS, ModSlot.MUZZLE),
        WeaponMods.slots(ModdedWeaponTest.catalog("10mm-pistol")));
    assertEquals(List.of("small-guns-long", "small-guns-ported"),
        WeaponMods.options(ModdedWeaponTest.catalog("10mm-pistol"), ModSlot.BARREL, ALL_BOOKS).stream()
            .map(WeaponModData::id).toList());
  }

  /** CRB p. 104–105: the Laser Musket takes a Muzzle mod only with a Bracketed barrel. */
  @Test
  void bracketedBarrelUnlocksTheMuzzle() {
    CarriedWeapon musket = ModdedWeaponTest.catalog("laser-musket");
    assertFalse(WeaponMods.slots(musket).contains(ModSlot.MUZZLE));
    musket = WeaponMods.install(musket, ModSlot.BARREL, mod("energy-weapons-bracketed-short-barrel"));
    assertTrue(WeaponMods.slots(musket).contains(ModSlot.MUZZLE));
    musket = WeaponMods.install(musket, ModSlot.MUZZLE, mod("energy-weapons-beam-focuser"));

    musket = WeaponMods.install(musket, ModSlot.BARREL, mod("energy-weapons-long-barrel"));

    assertFalse(musket.mods().containsKey(ModSlot.MUZZLE), "the Muzzle mod goes with its barrel");
  }

  /** The Laser Gun lists Muzzle mods without a Bracketed barrel: always on offer. */
  @Test
  void muzzleWithoutBracketedBarrelIsAlwaysOnOffer() {
    assertTrue(WeaponMods.slots(ModdedWeaponTest.catalog("laser-gun")).contains(ModSlot.MUZZLE));
  }

  /** CRB p. 98: the Pipe Bolt-Action takes a Grip or a Stock, not both. */
  @Test
  void gripOrStock() {
    CarriedWeapon rifle = WeaponMods.install(ModdedWeaponTest.catalog("pipe-bolt-action"), ModSlot.GRIP,
        mod("small-guns-sharpshooters-grip"));
    rifle = WeaponMods.install(rifle, ModSlot.STOCK, mod("small-guns-marksmans-stock"));
    assertEquals(Set.of(ModSlot.STOCK), rifle.mods().keySet());
  }

  @Test
  void rejectsModsTheWeaponDoesNotAccept() {
    CarriedWeapon pistol = ModdedWeaponTest.catalog("44-pistol");
    WeaponModData automatic = mod("small-guns-automatic");
    WeaponModData hardened = mod("small-guns-hardened");
    assertFalse(WeaponMods.canInstall(pistol, ModSlot.RECEIVER, automatic));
    assertThrows(IllegalArgumentException.class, () -> WeaponMods.install(pistol, ModSlot.RECEIVER, automatic));
    assertThrows(IllegalArgumentException.class, () -> WeaponMods.install(pistol, ModSlot.BARREL, hardened),
        "wrong slot");
  }

  /** A custom weapon takes any catalog mod of its chosen slots, its own weapon type's table first. */
  @Test
  void customWeaponOptions() {
    CarriedWeapon gun = ModdedWeaponTest.custom("", EnumSet.of(ModSlot.BARREL));
    assertEquals(List.of(ModSlot.BARREL), WeaponMods.slots(gun));
    List<WeaponModData> options = WeaponMods.options(gun, ModSlot.BARREL, ALL_BOOKS);
    assertEquals("small-guns", options.getFirst().table());
    assertTrue(options.stream().anyMatch(option -> option.table().equals("energy-weapons")));
    assertTrue(WeaponMods.canInstall(gun, ModSlot.BARREL, mod("energy-weapons-long-barrel")));
    assertFalse(WeaponMods.canInstall(gun, ModSlot.RECEIVER, mod("small-guns-hardened")), "slot not chosen");
    assertTrue(WeaponMods.options(gun, ModSlot.BARREL, EnumSet.of(Book.CRB)).stream()
        .noneMatch(option -> option.table().equals("cryojet")), "WOA mods need WOA");
  }

  /** CRB p. 87: perks are information only; Gun Nut 2 is missing for an Advanced receiver. */
  @Test
  void missingPerks() {
    PlayerCharacter character = PlayerCharacter.createNew();
    assertEquals(List.of("Gun Nut 2"), WeaponMods.missingPerks(character, mod("small-guns-advanced")).stream()
        .map(WeaponMods::perkText).toList());
    assertTrue(WeaponMods.missingPerks(character, mod("small-guns-hardened")).isEmpty());
  }

  @Test
  void customModSlotsDropTheirMods() {
    CarriedWeapon gun = WeaponMods.install(ModdedWeaponTest.custom("", EnumSet.of(ModSlot.BARREL, ModSlot.GRIP)),
        ModSlot.BARREL, mod("small-guns-long"));
    gun = gun.withCustomModSlots(EnumSet.of(ModSlot.GRIP));
    assertTrue(gun.mods().isEmpty());
    CarriedWeapon pistol = CarriedWeapon.of(CatalogRef.of("10mm-pistol"));
    assertThrows(IllegalStateException.class, () -> pistol.withCustomModSlots(Set.of()));
  }

  private static WeaponModData mod(String id) {
    return WeaponCatalog.modById(id).orElseThrow();
  }
}
