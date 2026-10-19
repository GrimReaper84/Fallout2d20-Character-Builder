package de.fallout2d20.charbuilder.model.character.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Weapon and ammunition edits of a character (CRB p. 91: shots per ammunition type). */
class PlayerCharacterWeaponsTest {

  private static final CustomWeapon BLASTER = new CustomWeapon("Alien Blaster", null, 5, "", "Energy", 1,
      WeaponRange.MEDIUM, "", null);

  @Test
  void weaponsCanBeEditedInEveryMode() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.completeCreation();
    character.addWeapon(CarriedWeapon.of(CatalogRef.of("frag-grenade")));
    character.addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    character.setWeaponQuantity(0, 3);
    character.setWeaponNotes(1, "Long Barrel");
    assertEquals(List.of(new CarriedWeapon(CatalogRef.of("frag-grenade"), null, 3, "", false),
        new CarriedWeapon(CatalogRef.of("10mm-pistol"), null, 1, "Long Barrel", false)), character.getWeapons());
    assertThrows(IllegalArgumentException.class, () -> character.setWeaponQuantity(0, 0));
    character.removeWeapon(0);
    assertEquals("10mm Pistol", character.getWeapons().getFirst().displayName());
    assertThrows(IndexOutOfBoundsException.class, () -> character.removeWeapon(5));
  }

  @Test
  void ammunitionIsCountedPerTypeAndZeroRemovesIt() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.setAmmunition("10mm", 34);
    assertEquals(34, character.ammunition("10mm"));
    assertEquals(0, character.ammunition("38"));
    character.setAmmunition("10mm", 0);
    assertEquals(Map.of(), character.getAmmunition());
    assertThrows(IllegalArgumentException.class, () -> character.setAmmunition("10mm", -1));
    assertThrows(IllegalArgumentException.class, () -> character.setAmmunition(" ", 3));
  }

  @Test
  void constructorDropsEmptyAmmunitionAndRejectsNegativeShots() {
    PlayerCharacter character = create(Map.of("10mm", 0, "38", 12));
    assertEquals(Map.of("38", 12), character.getAmmunition());
    assertThrows(IllegalArgumentException.class, () -> create(Map.of("38", -1)));
  }

  /** A custom weapon imported without skill gets one later; a catalog weapon's skill is fixed. */
  @Test
  void customWeaponSkillCanBeChosen() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.addWeapon(CarriedWeapon.of(BLASTER));
    character.addWeapon(CarriedWeapon.of(CatalogRef.of("laser-gun")));
    character.setCustomWeaponSkill(0, Skill.ENERGY_WEAPONS);
    assertEquals(Skill.ENERGY_WEAPONS, character.getWeapons().getFirst().skill().orElseThrow());
    assertThrows(IllegalStateException.class, () -> character.setCustomWeaponSkill(1, Skill.SMALL_GUNS));
  }

  /** CRB p. 27, 120: explosives and thrown weapons are used up, so their quantity matters. */
  @Test
  void explosivesAndThrownWeaponsAreCounted() {
    assertTrue(CarriedWeapon.of(CatalogRef.of("frag-grenade")).isCounted());
    assertTrue(CarriedWeapon.of(CatalogRef.of("javelin")).isCounted());
    assertFalse(CarriedWeapon.of(CatalogRef.of("10mm-pistol")).isCounted());
    assertTrue(CarriedWeapon.of(BLASTER.withSkill(Skill.EXPLOSIVES)).isCounted(), "homebrew grenade");
    assertFalse(CarriedWeapon.of(BLASTER).isCounted());
    assertTrue(CarriedWeapon.of(BLASTER).withQuantity(2).isCounted());
  }

  /** CRB p. 89: fire rate 0–6. */
  @Test
  void customFireRateStaysWithinTheBook() {
    assertThrows(IllegalArgumentException.class,
        () -> new CustomWeapon("Gun", Skill.SMALL_GUNS, 3, "", "Physical", 7, WeaponRange.CLOSE, "", null));
  }

  private static PlayerCharacter create(Map<String, Integer> ammunition) {
    return PlayerCharacter.builder().name("Test").ammunition(ammunition).build();
  }

  /** D4: mods are installed, replaced and removed per slot in any mode. */
  @Test
  void installsReplacesAndRemovesMods() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    character.setWeaponMod(0, ModSlot.RECEIVER, WeaponCatalog.modById("small-guns-hardened").orElseThrow());
    character.setWeaponMod(0, ModSlot.RECEIVER, WeaponCatalog.modById("small-guns-powerful").orElseThrow());
    assertEquals(Map.of(ModSlot.RECEIVER, CatalogRef.of("small-guns-powerful")), character.getWeapons().getFirst().mods());
    character.setWeaponMod(0, ModSlot.RECEIVER, null);
    assertEquals(Map.of(), character.getWeapons().getFirst().mods());
    assertThrows(IllegalArgumentException.class,
        () -> character.setWeaponMod(0, ModSlot.STOCK, WeaponCatalog.modById("small-guns-full-stock").orElseThrow()));
  }

  @Test
  void customWeaponModSlots() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.addWeapon(CarriedWeapon.of(BLASTER));
    character.setCustomWeaponModSlots(0, EnumSet.of(ModSlot.BARREL));
    character.setWeaponMod(0, ModSlot.BARREL, WeaponCatalog.modById("energy-weapons-long-barrel").orElseThrow());
    character.setCustomWeaponModSlots(0, EnumSet.of(ModSlot.SIGHTS));
    assertEquals(Set.of(ModSlot.SIGHTS), character.getWeapons().getFirst().custom().modSlots());
    assertTrue(character.getWeapons().getFirst().mods().isEmpty());
    character.addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    assertThrows(IllegalStateException.class, () -> character.setCustomWeaponModSlots(1, Set.of()));
  }
}
