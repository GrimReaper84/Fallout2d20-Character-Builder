package de.fallout2d20.charbuilder.catalog.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The weapon mod catalog (CRB pp. 100–119, WOA p. 23). */
class WeaponModCatalogTest {

  @Test
  void loadsEveryTable() {
    assertEquals(150, WeaponCatalog.mods().size());
    assertEquals(47, WeaponCatalog.all().stream().filter(weapon -> !weapon.mods().isEmpty()).count());
  }

  /** CRB p. 100: Hardened receiver. */
  @Test
  void hardenedReceiver() {
    assertEquals(new WeaponModData("small-guns-hardened", "Hardened", "small-guns", "Small Guns mods",
        List.of(new Source(Book.CRB, 100)), ModSlot.RECEIVER, Skill.REPAIR, "Hardened",
        List.of(new ModEffect(ModEffectType.DAMAGE, 1, null)), null, 20, List.of()),
        WeaponCatalog.modById("small-guns-hardened").orElseThrow());
  }

  /** CRB p. 104 vs. p. 108: the Gatling Laser's Photon Exciter differs from the shared one. */
  @Test
  void sameNameOtherTable() {
    WeaponModData shared = WeaponCatalog.modById("energy-weapons-photon-exciter").orElseThrow();
    WeaponModData gatling = WeaponCatalog.modById("gatling-laser-photon-exciter").orElseThrow();
    assertNull(shared.weight());
    assertEquals(30, shared.cost());
    assertEquals(1, gatling.weight());
    assertEquals(19, gatling.cost());
    assertEquals(List.of(new ModPerk("science", 3)), gatling.perks());
  }

  /** WOA p. 23: the Cryojet's own mods. */
  @Test
  void cryojetMods() {
    WeaponModData nitrogen = WeaponCatalog.modById("cryojet-liquid-nitrogen").orElseThrow();
    assertEquals(new Source(Book.WOA, 23), nitrogen.sources().getFirst());
    assertEquals(ModSlot.FUEL, nitrogen.slot());
    assertEquals(7, WeaponCatalog.byId("cryojet").orElseThrow().mods().size());
  }

  @Test
  void everyModIsConsistent() {
    for (WeaponModData mod : WeaponCatalog.mods()) {
      assertTrue(mod.id().startsWith(mod.table() + "-"), mod.id());
      for (ModPerk perk : mod.perks()) {
        assertTrue(PerkCatalog.byId(perk.perk()).isPresent(), mod.id() + " " + perk.perk());
      }
      boolean melee = mod.slot() == ModSlot.UPGRADE;
      assertTrue(WeaponCatalog.all().stream().filter(weapon -> weapon.mods().contains(mod.id()))
          .allMatch(weapon -> melee == (weapon.skill() == Skill.MELEE_WEAPONS || weapon.skill() == Skill.UNARMED)),
          mod.id());
    }
  }

  @Test
  void effectDescriptions() {
    assertEquals("+1 CD damage", new ModEffect(ModEffectType.DAMAGE, 1, null).description());
    assertEquals("Range -1 step", new ModEffect(ModEffectType.RANGE, -1, null).description());
    assertEquals("Damage type Energy", new ModEffect(ModEffectType.SET_DAMAGE_TYPE, null, "ENERGY").description());
    assertEquals("Ammunition .308", new ModEffect(ModEffectType.SET_AMMUNITION, null, "308").description());
  }
}
