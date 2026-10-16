package de.fallout2d20.charbuilder.model.character.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Attack values per weapon (CRB p. 27, 13, 48). */
class WeaponAttackTest {

  /** A Survivor (all attributes 5) with AGI 6, STR 9, Small Guns tagged with 1 bought rank. */
  private static Character character() {
    Character character = Character.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.setAttributeAllocation(Attribute.AGILITY, 1);
    character.setAttributeAllocation(Attribute.STRENGTH, 4);
    character.setFreeTag(Skill.SMALL_GUNS, true);
    character.setCreationRanks(Skill.SMALL_GUNS, 1);
    return character;
  }

  /** AGI 6 + Small Guns 3 (Tag) = 9; every d20 up to 3 is a critical success. */
  @Test
  void taggedSmallGun() {
    WeaponAttack attack = WeaponAttack.of(character(), CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    assertEquals(Skill.SMALL_GUNS, attack.skill());
    assertEquals(9, attack.targetNumber());
    assertEquals(3, attack.criticalRange());
    assertEquals(4, attack.damage());
    assertEquals(0, attack.meleeBonus());
    assertEquals("Physical", attack.damageType());
    assertEquals(2, attack.fireRate());
    assertEquals(WeaponRange.CLOSE, attack.range());
    assertEquals("Close Quarters, Reliable", attack.qualities());
    assertEquals(List.of("10mm"), attack.ammunition());
  }

  /** STR 9 + untrained Melee Weapons = 9, critical only on a 1; STR 9–10 adds +2 CD (CRB p. 48). */
  @Test
  void untaggedMeleeWeaponAddsTheMeleeBonus() {
    WeaponAttack attack = WeaponAttack.of(character(), CarriedWeapon.of(CatalogRef.of("sledgehammer")));
    assertEquals(9, attack.targetNumber());
    assertEquals(1, attack.criticalRange());
    assertEquals(5, attack.damage());
    assertEquals(2, attack.meleeBonus());
    assertNull(attack.range());
  }

  @Test
  void unarmedStrikeAndPlasmaGunTypes() {
    Character character = character();
    WeaponAttack strike = WeaponAttack.of(character, WeaponCatalog.byId("unarmed-strike").orElseThrow());
    assertEquals(9, strike.targetNumber(), "STR 9 + Unarmed 0");
    assertEquals(2, strike.meleeBonus());
    assertEquals("Physical/Energy",
        WeaponAttack.of(character, WeaponCatalog.byId("plasma-gun").orElseThrow()).damageType());
  }

  @Test
  void customWeaponWithoutSkillHasNoTargetNumber() {
    CustomWeapon blaster = new CustomWeapon("Alien Blaster", null, 5, "", "Energy", 1, WeaponRange.MEDIUM, "",
        "fusion-cell");
    WeaponAttack attack = WeaponAttack.of(character(), CarriedWeapon.of(blaster));
    assertNull(attack.targetNumber());
    assertNull(attack.criticalRange());
    assertEquals(5, attack.damage());
    assertEquals(List.of("fusion-cell"), attack.ammunition());
  }

  @Test
  void carriedWeaponIsEitherCatalogOrCustom() {
    CustomWeapon custom = new CustomWeapon("Spear", Skill.MELEE_WEAPONS, 3, "", "Physical", null, null, "", null);
    assertThrows(IllegalArgumentException.class, () -> new CarriedWeapon(null, null, 1, "", false));
    assertThrows(IllegalArgumentException.class,
        () -> new CarriedWeapon(CatalogRef.of("sword"), custom, 1, "", false));
    assertEquals("Spear", CarriedWeapon.of(custom).displayName());
    assertEquals("Sword", CarriedWeapon.of(CatalogRef.of("sword")).displayName());
  }

  /** A Protectron with STR 8, PER 5, END 7 and no ranks in the attack skills. */
  private static Character protectron() {
    Character character = Character.createNew();
    character.chooseOrigin(CatalogRef.of("protectron"));
    character.setAttributeAllocation(Attribute.STRENGTH, 3);
    character.setAttributeAllocation(Attribute.ENDURANCE, 2);
    return character;
  }

  /** WOA p. 22: Self-Destruct rolls END + Explosives (7 + 0), not the governing PER. */
  @Test
  void selfDestructRollsEndurance() {
    WeaponAttack attack = WeaponAttack.of(protectron(), CarriedWeapon.of(CatalogRef.of("self-destruct")));
    assertEquals(Attribute.ENDURANCE, attack.attribute());
    assertEquals(7, attack.targetNumber());
    assertEquals(6, attack.damage());
    assertEquals(0, attack.meleeBonus());
  }

  /** WOA p. 22: the Cryojet rolls PER + Energy Weapons (5 + 0). */
  @Test
  void cryojetRollsPerception() {
    WeaponAttack attack = WeaponAttack.of(protectron(), CarriedWeapon.of(CatalogRef.of("cryojet")));
    assertEquals(Attribute.PERCEPTION, attack.attribute());
    assertEquals(5, attack.targetNumber());
    assertEquals(List.of("cryo-cell"), attack.ammunition());
  }

  /** WOA p. 22: Claws are unarmed attacks with the melee bonus (STR 8: +1 CD, CRB p. 48). */
  @Test
  void protectronClawsAddTheMeleeBonus() {
    WeaponAttack attack = WeaponAttack.of(protectron(), CarriedWeapon.of(CatalogRef.of("protectron-claws")));
    assertEquals(Skill.UNARMED, attack.skill());
    assertEquals(8, attack.targetNumber());
    assertEquals(3, attack.damage());
    assertEquals(1, attack.meleeBonus());
  }
}
