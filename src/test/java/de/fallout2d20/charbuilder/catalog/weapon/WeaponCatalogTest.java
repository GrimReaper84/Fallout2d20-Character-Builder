package de.fallout2d20.charbuilder.catalog.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Spot checks of the weapon and ammunition catalogs against CRB p. 91–120 and WG. */
class WeaponCatalogTest {

  @Test
  void containsAllCoreWeaponsPerSkill() {
    Map<Skill, Long> perSkill = WeaponCatalog.available(EnumSet.of(Book.CRB)).stream()
        .collect(Collectors.groupingBy(WeaponData::skill, Collectors.counting()));
    assertEquals(Map.of(Skill.SMALL_GUNS, 15L, Skill.ENERGY_WEAPONS, 5L, Skill.BIG_GUNS, 7L,
        Skill.MELEE_WEAPONS, 20L, Skill.UNARMED, 6L, Skill.THROWING, 3L, Skill.EXPLOSIVES, 11L), perSkill);
    // 20 ammunition types (CRB p. 91) and the 9 syringer types (CRB p. 93).
    assertEquals(29, WeaponCatalog.ammunition().stream().filter(ammo -> ammo.isAvailableWith(EnumSet.of(Book.CRB)))
        .count());
    WeaponData syringer = WeaponCatalog.byId("syringer").orElseThrow();
    assertEquals(11, syringer.ammunition().size());
    assertTrue(syringer.ammunition().stream().allMatch(id -> WeaponCatalog.ammunitionById(id).isPresent()));
    assertEquals(75, WeaponCatalog.ammunitionById("syringer-cazadores-sting").orElseThrow().cost());
    assertEquals(67, WeaponCatalog.available(EnumSet.of(Book.CRB)).size());
  }

  /** CRB p. 95 and the 10mm Pistol description. */
  @Test
  void tenMillimeterPistol() {
    WeaponData pistol = WeaponCatalog.byId("10mm-pistol").orElseThrow();
    assertEquals(new WeaponData("10mm-pistol", "10mm Pistol", List.of(new Source(Book.CRB, 95)), Skill.SMALL_GUNS,
        4, List.of(), List.of(DamageType.PHYSICAL), 2, WeaponRange.CLOSE, List.of("Close Quarters", "Reliable"),
        "4", 50, 1, List.of("10mm"), List.of(), null, List.of(),
        List.of("10mm automatic pistol", "10mm Auto Pistol"), List.of("small-guns-calibrated", "small-guns-hardened",
            "small-guns-automatic", "small-guns-hair-trigger", "small-guns-powerful", "small-guns-advanced",
            "small-guns-long", "small-guns-ported", "small-guns-comfort-grip", "small-guns-sharpshooters-grip",
            "small-guns-reflex-sight", "small-guns-recon-scope", "small-guns-compensator", "small-guns-suppressor",
            "small-guns-armor-piercing-receiver", "small-guns-armor-piercing-automatic-receiver",
            "small-guns-hardened-automatic-receiver", "small-guns-rapid-automatic-receiver",
            "small-guns-calibrated-powerful-receiver", "small-guns-powerful-automatic-receiver",
            "small-guns-hardened-piercing-auto-receiver"),
        null, false, null, false, false, false), pistol);
    assertTrue(WeaponCatalog.byName("Pipe rifle").isEmpty(), "pack wordings are not legacy names");
    assertEquals("pipe-gun", WeaponCatalog.byNameOrAlias("Pipe rifle").orElseThrow().id());
  }

  /** CRB p. 111: Unarmed Strike has no weight, cost or rarity. */
  @Test
  void unarmedStrike() {
    WeaponData strike = WeaponCatalog.byId("unarmed-strike").orElseThrow();
    assertEquals(Skill.UNARMED, strike.skill());
    assertEquals(2, strike.damage());
    assertNull(strike.weight());
    assertNull(strike.cost());
    assertNull(strike.range());
    assertTrue(strike.ammunition().isEmpty());
  }

  /** CRB p. 106–107: Gatling Laser takes fusion cells or a fusion core; CRB p. 91 fusion core values. */
  @Test
  void gatlingLaserAndFusionCore() {
    assertEquals(List.of("fusion-cell", "fusion-core"), WeaponCatalog.byId("gatling-laser").orElseThrow().ammunition());
    AmmunitionData core = WeaponCatalog.ammunitionById("fusion-core").orElseThrow();
    assertEquals("1", core.quantityFound());
    assertEquals(200, core.cost());
    assertEquals(3, core.rarity());
    assertNull(WeaponCatalog.ammunitionById("syringer-ammo").orElseThrow().cost(), "cost varies");
  }

  @Test
  void findsWeaponsByNameAndLegacyName() {
    assertEquals("laser-gun", WeaponCatalog.byName(" laser gun ").orElseThrow().id());
    assertEquals("molotov-cocktail", WeaponCatalog.byName("Molotov").orElseThrow().id());
    assertTrue(WeaponCatalog.byName("Alien Blaster").isEmpty());
  }

  /**
   * Every gun of the Small Guns, Energy Weapons, Big Guns and Bows tables (bows: Athletics, WG p. 73) names its
   * ammunition, except the Junk Jet; the Ballistic Fist is the only other weapon that uses ammunition (WG p. 77).
   */
  @Test
  void gunsHaveAmmunitionAndRange() {
    for (WeaponData weapon : WeaponCatalog.all()) {
      boolean gun = EnumSet.of(Skill.SMALL_GUNS, Skill.ENERGY_WEAPONS, Skill.BIG_GUNS, Skill.ATHLETICS)
          .contains(weapon.skill());
      assertEquals((gun && !weapon.id().equals("junk-jet")) || weapon.id().equals("ballistic-fist"),
          !weapon.ammunition().isEmpty(), weapon.id());
      assertEquals(gun || weapon.id().equals("self-destruct"), weapon.range() != null, weapon.id());
      assertEquals(gun, weapon.fireRate() != null, weapon.id());
    }
  }

  /** WOA pp. 22–23: the Protectron weapons, built into a Protectron only. */
  @Test
  void protectronWeapons() {
    List<WeaponData> woa = WeaponCatalog.available(EnumSet.of(Book.CRB, Book.WOA)).stream()
        .filter(weapon -> !weapon.isAvailableWith(EnumSet.of(Book.CRB))).toList();
    assertEquals(List.of("protectron-claws", "cryojet", "self-destruct", "shock-hands"), woa.stream().map(WeaponData::id).toList());
    for (WeaponData weapon : woa) {
      assertTrue(weapon.isOfferedTo("protectron"), weapon.id());
      assertFalse(weapon.isOfferedTo("survivor"), weapon.id());
      assertFalse(weapon.isOfferedTo(null), weapon.id());
    }
    assertTrue(WeaponCatalog.byId("10mm-pistol").orElseThrow().isOfferedTo(null));
    WeaponData cryojet = WeaponCatalog.byId("cryojet").orElseThrow();
    assertEquals(new WeaponData("cryojet", "Cryojet", List.of(new Source(Book.WOA, 22), new Source(Book.WOA, 23)), Skill.ENERGY_WEAPONS, 3,
        List.of("Stun", "Persistent"), List.of(DamageType.ENERGY), 4, WeaponRange.CLOSE,
        List.of("Debilitating", "Inaccurate"), "6", 156, 4, List.of("cryo-cell"), List.of(), null,
        List.of("protectron"), List.of(), List.of("cryojet-liquid-nitrogen", "cryojet-compressed-tubing",
            "cryojet-large-tank", "cryojet-huge-tank", "cryojet-wide-diffusion", "cryojet-aerosolizer",
            "cryojet-bladed-exhaust"), null, false, null, false, false, false), cryojet);
    AmmunitionData cell = WeaponCatalog.ammunitionById("cryo-cell").orElseThrow();
    assertNull(cell.quantityFound());
    assertNull(cell.rarity());
    WeaponData selfDestruct = WeaponCatalog.byId("self-destruct").orElseThrow();
    assertEquals(Attribute.ENDURANCE, selfDestruct.attackAttribute());
    assertEquals(Attribute.PERCEPTION, cryojet.attackAttribute(), "governing attribute of Energy Weapons");
    assertEquals("shock-hands", WeaponCatalog.byName("Protectron Shock Hands").orElseThrow().id());
    assertEquals("protectron-claws", WeaponCatalog.byName("Protectron Claws").orElseThrow().id());
    assertEquals("protectron-claws", WeaponCatalog.byName("Claws").orElseThrow().id());
    assertEquals(WeaponCatalog.available(EnumSet.of(Book.CRB, Book.WOA)).size() - 4,
        WeaponCatalog.offered("survivor", EnumSet.of(Book.CRB, Book.WOA)).size());
  }

  /**
   * WG p. 11: the Assaultron's trait weapons; range and fire rate of the Head Laser from the hand-held Assaultron
   * Head (WG p. 65).
   */
  @Test
  void assaultronWeapons() {
    WeaponData claws = WeaponCatalog.byId("assaultron-claws").orElseThrow();
    assertEquals(List.of(Skill.UNARMED, 4), List.of(claws.skill(), claws.damage()));
    WeaponData laser = WeaponCatalog.byId("assaultron-head-laser").orElseThrow();
    assertEquals(List.of(5, 0), List.of(laser.damage(), laser.fireRate()));
    assertEquals(List.of("Piercing 1"), laser.damageEffects());
    assertEquals(WeaponRange.CLOSE, laser.range());
    assertEquals(List.of("fusion-cell"), laser.ammunition());
    assertTrue(laser.special() != null && laser.special().contains("2 shots"));
    WeaponData selfDestruct = WeaponCatalog.byId("self-destruct").orElseThrow();
    assertTrue(selfDestruct.isOfferedTo("protectron") && selfDestruct.isOfferedTo("assaultron"));
    assertFalse(laser.isOfferedTo("protectron"));
  }

  /** WG pp. 57–80, 112: the Wanderer's Guide weapons (with the Lightweight Mini-Nuke) and ammunition. */
  @Test
  void wandererWeapons() {
    assertEquals(57, WeaponCatalog.all().stream()
        .filter(weapon -> weapon.sources().getFirst().book() == Book.WG && weapon.origins().isEmpty()).count());
    WeaponData revolver = WeaponCatalog.byId("357-magnum-revolver").orElseThrow();
    assertEquals(List.of(5, 1), List.of(revolver.damage(), revolver.fireRate()));
    assertEquals(List.of("Vicious"), revolver.damageEffects());
    assertEquals(List.of("357-magnum"), revolver.ammunition());
    assertEquals(List.of("Inaccurate", "Recoil (7)", "Two-Handed"),
        WeaponCatalog.byId("12-7mm-smg").orElseThrow().qualities());
    WeaponData gatlingPlasma = WeaponCatalog.byId("gatling-plasma").orElseThrow();
    assertEquals(List.of(DamageType.PHYSICAL, DamageType.ENERGY), gatlingPlasma.damageTypes());
    assertEquals(List.of("plasma-cartridge", "plasma-core"), gatlingPlasma.ammunition());
    WeaponData bow = WeaponCatalog.byId("bow").orElseThrow();
    assertEquals(Skill.ATHLETICS, bow.skill());
    assertEquals(Attribute.AGILITY, bow.attackAttribute());
    assertEquals(7, bow.ammunition().size(), "arrows and the six arrow types");
    assertTrue(WeaponCatalog.byId("chainsaw").orElseThrow().noMeleeBonus());
    WeaponData flashBang = WeaponCatalog.byId("flash-bang").orElseThrow();
    assertTrue(flashBang.specialDamage());
    assertTrue(flashBang.damageTypes().isEmpty());
    AmmunitionData cryoArrow = WeaponCatalog.ammunitionById("cryo-arrow").orElseThrow();
    assertEquals(List.of(10, 4), List.of(cryoArrow.cost(), cryoArrow.rarity()), "Arrow 2/1 + Cryo +8/+3 (WG p. 59)");
    assertTrue(cryoArrow.special() != null && cryoArrow.special().contains("Freeze"));
    assertNull(WeaponCatalog.ammunitionById("electron-charge-pack").orElseThrow().cost(), "named only (WG p. 65)");
  }
}
