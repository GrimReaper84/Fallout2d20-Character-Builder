package de.fallout2d20.charbuilder.model.character.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Attack values per weapon (CRB p. 27, 13, 48). */
class WeaponAttackTest {

  /** A Survivor (all attributes 5) with AGI 6, STR 9, Small Guns tagged with 1 bought rank. */
  private static PlayerCharacter character() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.abilities().setAttributeAllocation(Attribute.AGILITY, 1);
    character.abilities().setAttributeAllocation(Attribute.STRENGTH, 4);
    character.abilities().setFreeTag(Skill.SMALL_GUNS, true);
    character.abilities().setCreationRanks(Skill.SMALL_GUNS, 1);
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
    PlayerCharacter character = character();
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
  private static PlayerCharacter protectron() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("protectron"));
    character.abilities().setAttributeAllocation(Attribute.STRENGTH, 3);
    character.abilities().setAttributeAllocation(Attribute.ENDURANCE, 2);
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

  /** CRB p. 90: the table range wins; range-less thrown weapons and grenades use their Thrown quality. */
  @Test
  void effectiveRangeFallsBackToTheThrownQuality() {
    PlayerCharacter character = character();
    assertEquals(WeaponRange.CLOSE, range(character, "10mm-pistol"));
    assertEquals(WeaponRange.CLOSE, range(character, "throwing-knives"));
    assertEquals(WeaponRange.MEDIUM, range(character, "frag-grenade"));
    assertNull(range(character, "bottlecap-mine"));
    assertNull(range(character, "combat-knife"));
  }

  @Test
  void effectiveRangeOfACustomWeaponReadsItsQualityText() {
    CustomWeapon spear = new CustomWeapon("Spear", Skill.THROWING, 3, "", "Physical", null, null, "Thrown (M)", null);
    assertEquals(WeaponRange.MEDIUM, WeaponAttack.of(character(), CarriedWeapon.of(spear)).effectiveRange());
  }

  @Test
  void rangeByCode() {
    assertEquals(Optional.of(WeaponRange.EXTREME), WeaponRange.byCode("X"));
    assertEquals(Optional.empty(), WeaponRange.byCode("Z"));
  }

  private static WeaponRange range(PlayerCharacter character, String id) {
    return WeaponAttack.of(character, CarriedWeapon.of(CatalogRef.of(id))).effectiveRange();
  }

  /** D4: the attack uses the modded values (Hardened +1 CD) and carries the modded name. */
  @Test
  void attackWithMods() {
    PlayerCharacter character = character();
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    character.gear().setWeaponMod(0, ModSlot.RECEIVER, WeaponCatalog.modById("small-guns-hardened").orElseThrow());
    WeaponAttack attack = WeaponAttack.of(character, character.gear().getWeapons().getFirst());
    assertEquals(5, attack.damage());
    assertEquals("10mm Pistol", attack.name());
    assertEquals("Hardened 10mm Pistol", attack.moddedName());
    assertEquals(9, attack.targetNumber());
  }
}
