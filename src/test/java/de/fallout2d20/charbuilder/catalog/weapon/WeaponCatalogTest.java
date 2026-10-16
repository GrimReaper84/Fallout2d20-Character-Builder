package de.fallout2d20.charbuilder.catalog.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Spot checks of the weapon and ammunition catalogs against CRB p. 91–120. */
class WeaponCatalogTest {

  @Test
  void containsAllCoreWeaponsPerSkill() {
    Map<Skill, Long> perSkill = WeaponCatalog.all().stream()
        .collect(Collectors.groupingBy(WeaponData::skill, Collectors.counting()));
    assertEquals(Map.of(Skill.SMALL_GUNS, 15L, Skill.ENERGY_WEAPONS, 5L, Skill.BIG_GUNS, 7L,
        Skill.MELEE_WEAPONS, 20L, Skill.UNARMED, 6L, Skill.THROWING, 3L, Skill.EXPLOSIVES, 11L), perSkill);
    assertEquals(20, WeaponCatalog.ammunition().size());
    assertEquals(67, WeaponCatalog.available(EnumSet.of(Book.CRB)).size());
  }

  /** CRB p. 95 and the 10mm Pistol description. */
  @Test
  void tenMillimeterPistol() {
    WeaponData pistol = WeaponCatalog.byId("10mm-pistol").orElseThrow();
    assertEquals(new WeaponData("10mm-pistol", "10mm Pistol", List.of(new Source(Book.CRB, 95)), Skill.SMALL_GUNS,
        4, List.of(), List.of(DamageType.PHYSICAL), 2, WeaponRange.CLOSE, List.of("Close Quarters", "Reliable"),
        "4", 50, 1, List.of("10mm"), List.of()), pistol);
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

  /** Every gun of the Small Guns, Energy Weapons and Big Guns tables names its ammunition, except the Junk Jet. */
  @Test
  void gunsHaveAmmunitionAndRange() {
    for (WeaponData weapon : WeaponCatalog.all()) {
      boolean gun = EnumSet.of(Skill.SMALL_GUNS, Skill.ENERGY_WEAPONS, Skill.BIG_GUNS).contains(weapon.skill());
      assertEquals(gun && !weapon.id().equals("junk-jet"), !weapon.ammunition().isEmpty(), weapon.id());
      assertEquals(gun, weapon.range() != null, weapon.id());
      assertEquals(gun, weapon.fireRate() != null, weapon.id());
    }
  }
}
