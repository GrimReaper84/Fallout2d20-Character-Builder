package de.fallout2d20.charbuilder.model.character.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.Vitals;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import de.fallout2d20.charbuilder.model.character.item.Inventory;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Weapon and ammunition edits of a character (CRB p. 91: shots per ammunition type). */
class CharacterWeaponsTest {

  private static final CustomWeapon BLASTER = new CustomWeapon("Alien Blaster", null, 5, "", "Energy", 1,
      WeaponRange.MEDIUM, "", null);

  @Test
  void weaponsCanBeEditedInEveryMode() {
    Character character = Character.createNew();
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
    Character character = Character.createNew();
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
    Character character = create(Map.of("10mm", 0, "38", 12));
    assertEquals(Map.of("38", 12), character.getAmmunition());
    assertThrows(IllegalArgumentException.class, () -> create(Map.of("38", -1)));
  }

  /** A custom weapon imported without skill gets one later; a catalog weapon's skill is fixed. */
  @Test
  void customWeaponSkillCanBeChosen() {
    Character character = Character.createNew();
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

  private static Character create(Map<String, Integer> ammunition) {
    return new Character("Test", null, OriginChoices.NONE, 1, 0, false, EnumSet.allOf(Book.class), Map.of(),
        Map.of(), List.of(), List.of(), Progression.NONE, Vitals.FULL, null, null, StartingEquipment.NONE, "",
        null, null, List.of(), ammunition, List.of(), Inventory.EMPTY, null);
  }

  /** D4: mods are installed, replaced and removed per slot in any mode. */
  @Test
  void installsReplacesAndRemovesMods() {
    Character character = Character.createNew();
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
    Character character = Character.createNew();
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
