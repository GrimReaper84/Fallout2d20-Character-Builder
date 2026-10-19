package de.fallout2d20.charbuilder.model.character.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Mod effects on the weapon values (CRB pp. 87–88, mod tables pp. 100–119). */
class ModdedWeaponTest {

  /** CRB p. 100: Hardened +1 CD, +20 caps, no weight; the name prefix goes before the name. */
  @Test
  void hardenedReceiver() {
    ModdedWeapon pistol = modded(catalog("10mm-pistol"), ModSlot.RECEIVER, "small-guns-hardened");
    assertEquals(5, pistol.damage());
    assertEquals("Hardened 10mm Pistol", pistol.moddedName());
    assertEquals("10mm Pistol", pistol.name());
    assertEquals(70, pistol.cost());
    assertEquals("4", pistol.weight());
  }

  /** CRB p. 100: Automatic −1 CD, +2 Fire Rate, gains Burst and Inaccurate; +1 lbs. */
  @Test
  void automaticReceiver() {
    ModdedWeapon pistol = modded(catalog("10mm-pistol"), ModSlot.RECEIVER, "small-guns-automatic");
    assertEquals(3, pistol.damage());
    assertEquals(4, pistol.fireRate());
    assertEquals(List.of("Burst"), pistol.damageEffects());
    assertEquals(List.of("Close Quarters", "Reliable", "Inaccurate"), pistol.qualities());
    assertEquals("5", pistol.weight());
  }

  /** CRB p. 88: a gained Accurate cancels Inaccurate instead of adding Accurate. */
  @Test
  void accurateCancelsInaccurate() {
    CarriedWeapon gun = custom("Inaccurate", EnumSet.of(ModSlot.SIGHTS));
    assertEquals(List.of(), modded(gun, ModSlot.SIGHTS, "small-guns-short-scope").qualities());
  }

  /**
   * House rule (user decision 2026-10-19, the book does not say): a removed quality stays removed whichever slot
   * gains it – Automatic receiver (gains Inaccurate) with a Comfort Grip (removes it).
   */
  @Test
  void removalWinsOverAnEarlierGain() {
    CarriedWeapon pistol = install(catalog("10mm-pistol"), ModSlot.RECEIVER, "small-guns-automatic");
    ModdedWeapon modded = modded(pistol, ModSlot.GRIP, "small-guns-comfort-grip");
    assertEquals(List.of("Close Quarters", "Reliable"), modded.qualities());
    assertEquals(List.of("Inaccurate (removed by Comfort Grip)"), modded.removedQualities());
  }

  /** Sharpshooter's Grip removes Inaccurate, a later slot (Beam Splitter muzzle) gaining it does not bring it back. */
  @Test
  void removalWinsOverALaterGain() {
    CarriedWeapon laser = install(catalog("laser-gun"), ModSlot.GRIP, "energy-weapons-sharpshooters-grip");
    ModdedWeapon modded = modded(laser, ModSlot.MUZZLE, "energy-weapons-beam-splitter");
    assertFalse(modded.qualities().contains("Inaccurate"));
    assertEquals(List.of("Inaccurate (removed by Sharpshooter’s Grip)"), modded.removedQualities());
  }

  /** A Sawed-Off barrel keeps Two-Handed away although a Full Stock gains it; a removal never blocks other gains. */
  @Test
  void removalBlocksOnlyItsOwnQuality() {
    CarriedWeapon shotgun = CarriedWeapon.of(new CustomWeapon("Shotgun", Skill.SMALL_GUNS, 5, "", "Physical", 0,
        WeaponRange.CLOSE, "Two-Handed, Inaccurate", null, EnumSet.of(ModSlot.BARREL, ModSlot.STOCK)));
    CarriedWeapon sawed = install(shotgun, ModSlot.BARREL, "small-guns-sawed-off");
    ModdedWeapon modded = modded(sawed, ModSlot.STOCK, "small-guns-full-stock");
    assertEquals(List.of("Close Quarters"), modded.qualities());
    assertEquals(List.of("Two-Handed (removed by Sawed-Off)", "Inaccurate (removed by Full Stock)"),
        modded.removedQualities());
  }

  /** Without a removal nothing is noted. */
  @Test
  void nothingRemoved() {
    assertTrue(modded(catalog("10mm-pistol"), ModSlot.RECEIVER, "small-guns-automatic").removedQualities().isEmpty());
  }

  /** CRB p. 90: a weapon may not be both Reliable and Unreliable – they cancel like Accurate and Inaccurate. */
  @Test
  void reliableCancelsUnreliable() {
    CarriedWeapon gun = custom("Unreliable", EnumSet.of(ModSlot.BARREL));
    assertEquals(List.of(), modded(gun, ModSlot.BARREL, "small-guns-bull-barrel").qualities());
  }

  /** CRB p. 88: a quality is never gained twice. */
  @Test
  void qualityNotGainedTwice() {
    CarriedWeapon gun = custom("Accurate", EnumSet.of(ModSlot.SIGHTS));
    assertEquals(List.of("Accurate"), modded(gun, ModSlot.SIGHTS, "small-guns-short-scope").qualities());
  }

  /** CRB p. 88: each Piercing from a mod raises the rating by 1 (Laser Gun Piercing 1 → 2). */
  @Test
  void piercingStacks() {
    ModdedWeapon laser = modded(catalog("laser-gun"), ModSlot.GRIP, "energy-weapons-sharpshooters-grip");
    assertEquals(List.of("Piercing 2"), laser.damageEffects());
  }

  /** CRB p. 88: range moves in steps and never beyond Extreme or below Close. */
  @Test
  void rangeStepsAreClamped() {
    assertEquals(WeaponRange.EXTREME, modded(catalog("gauss-rifle"), ModSlot.SIGHTS, "small-guns-long-scope").range());
    assertEquals(WeaponRange.CLOSE,
        modded(catalog("railway-rifle"), ModSlot.RECEIVER, "small-guns-automatic-piston").range());
    CarriedWeapon far = CarriedWeapon.of(new CustomWeapon("Far", Skill.SMALL_GUNS, 3, "", "Physical", 1,
        WeaponRange.EXTREME, "", null, EnumSet.of(ModSlot.SIGHTS)));
    assertEquals(WeaponRange.EXTREME, modded(far, ModSlot.SIGHTS, "small-guns-long-scope").range());
  }

  /** CRB p. 89: fire rate stays at 6 or less (Minigun 5 + Accelerated Barrel +1, then a custom 6). */
  @Test
  void fireRateIsClamped() {
    assertEquals(6, modded(catalog("minigun"), ModSlot.BARREL, "minigun-accelerated-barrel").fireRate());
    CarriedWeapon fast = CarriedWeapon.of(new CustomWeapon("Fast", Skill.SMALL_GUNS, 3, "", "Physical", 6,
        WeaponRange.CLOSE, "", null, EnumSet.of(ModSlot.RECEIVER)));
    assertEquals(6, modded(fast, ModSlot.RECEIVER, "small-guns-hair-trigger").fireRate());
  }

  /** CRB p. 100: the .308 Receiver sets 7 CD and .308 ammo first; the Finned barrel's +1 CD adds to it. */
  @Test
  void replacingValuesComeFirst() {
    CarriedWeapon revolver = WeaponMods.install(catalog("pipe-revolver"), ModSlot.BARREL,
        WeaponCatalog.modById("small-guns-finned").orElseThrow());
    ModdedWeapon modded = modded(revolver, ModSlot.RECEIVER, "small-guns-308-receiver");
    assertEquals(8, modded.damage());
    assertEquals(List.of("308"), modded.ammunition());
    assertEquals(".308 Finned Pipe Revolver", modded.moddedName());
  }

  /** CRB p. 104: the Electric Signal Carrier Antennae change the Gamma Gun to 7 CD Energy, Radioactive. */
  @Test
  void damageTypeChange() {
    ModdedWeapon gamma = modded(catalog("gamma-gun"), ModSlot.MUZZLE, "gamma-gun-electric-signal-carrier-antennae");
    assertEquals(7, gamma.damage());
    assertEquals("Energy", gamma.damageType());
    assertEquals(List.of("Piercing 1", "Stun", "Radioactive"), gamma.damageEffects());
  }

  /** CRB p. 103: a Stock turns the Laser Gun into a Laser Rifle; prefixes still go first. */
  @Test
  void stockNameReplacesTheName() {
    assertEquals("Laser Rifle", modded(catalog("laser-gun"), ModSlot.STOCK, "energy-weapons-standard-stock").moddedName());
    assertEquals("Marksman’s Laser Rifle",
        modded(catalog("laser-gun"), ModSlot.STOCK, "energy-weapons-marksmans-stock").moddedName());
  }

  /** A custom weapon has no weight of its own; its mods' weight counts (user decision 2026-10-17). */
  @Test
  void customWeaponWeighsItsMods() {
    CarriedWeapon gun = custom("", EnumSet.of(ModSlot.RECEIVER));
    assertEquals(null, ModdedWeapon.of(gun).orElseThrow().weight());
    assertEquals("1", modded(gun, ModSlot.RECEIVER, "small-guns-powerful").weight());
  }

  @Test
  void specialRulesAreListedWithTheModName() {
    assertEquals(List.of("Reflex Sight: May re-roll the hit location die"),
        modded(catalog("10mm-pistol"), ModSlot.SIGHTS, "small-guns-reflex-sight").specials());
  }

  @Test
  void unmoddedKeepsTheBaseValues() {
    CarriedWeapon pistol = WeaponMods.install(catalog("10mm-pistol"), ModSlot.RECEIVER,
        WeaponCatalog.modById("small-guns-hardened").orElseThrow());
    assertEquals(4, ModdedWeapon.unmodded(pistol).orElseThrow().damage());
    assertEquals("10mm Pistol", ModdedWeapon.unmodded(pistol).orElseThrow().moddedName());
  }

  @Test
  void unknownModIdHasNoEffect() {
    CarriedWeapon pistol = catalog("10mm-pistol").withMod(ModSlot.RECEIVER, CatalogRef.of("gone"));
    assertEquals(4, ModdedWeapon.of(pistol).orElseThrow().damage());
  }

  /** A mod stored under another slot than its own (hand-edited file) has no effect, so no slot counts twice. */
  @Test
  void modUnderAnotherSlotHasNoEffect() {
    CarriedWeapon pistol = catalog("10mm-pistol").withMod(ModSlot.RECEIVER, CatalogRef.of("small-guns-long"));
    assertEquals(WeaponRange.CLOSE, ModdedWeapon.of(pistol).orElseThrow().range());
  }

  /** Free-text Piercing of a custom weapon stacks regardless of case. */
  @Test
  void customPiercingStacksIgnoringCase() {
    CarriedWeapon gun = CarriedWeapon.of(new CustomWeapon("Homebrew", Skill.SMALL_GUNS, 3, "piercing 2", "Physical", 1,
        WeaponRange.CLOSE, "", null, EnumSet.of(ModSlot.GRIP)));
    assertEquals(List.of("Piercing 3"), modded(gun, ModSlot.GRIP, "small-guns-sharpshooters-grip").damageEffects());
  }

  static CarriedWeapon catalog(String id) {
    return CarriedWeapon.of(CatalogRef.of(id));
  }

  static CarriedWeapon custom(String qualities, Set<ModSlot> slots) {
    return CarriedWeapon.of(new CustomWeapon("Homebrew", Skill.SMALL_GUNS, 3, "", "Physical", 1, WeaponRange.CLOSE,
        qualities, null, slots));
  }

  private static CarriedWeapon install(CarriedWeapon weapon, ModSlot slot, String modId) {
    return WeaponMods.install(weapon, slot, WeaponCatalog.modById(modId).orElseThrow());
  }

  private static ModdedWeapon modded(CarriedWeapon weapon, ModSlot slot, String modId) {
    return ModdedWeapon.of(WeaponMods.install(weapon, slot, WeaponCatalog.modById(modId).orElseThrow())).orElseThrow();
  }
}
