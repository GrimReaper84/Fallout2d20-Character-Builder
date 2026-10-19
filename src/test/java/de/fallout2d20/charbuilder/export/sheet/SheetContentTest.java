package de.fallout2d20.charbuilder.export.sheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.CustomItem;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Texts of the printed character sheet (layout modelled on CRB pp. 430–431). */
class SheetContentTest {

  private static PlayerCharacter survivor() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.setName("Nora");
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.abilities().setFreeTag(Skill.SMALL_GUNS, true);
    return character;
  }

  @Test
  void headerAttributesAndSkills() {
    SheetContent sheet = SheetContent.of(survivor());
    assertEquals("Nora", sheet.name());
    assertEquals("Survivor", sheet.origin());
    assertEquals("1", sheet.level());
    assertEquals("0", sheet.xpEarned());
    assertEquals("100", sheet.xpNextLevel(), "CRB p. 49: level 2 at 100 XP");
    assertEquals(7, sheet.attributes().size());
    assertEquals(5, sheet.attributes().get(Attribute.AGILITY));
    assertEquals(17, sheet.skills().size());
    SkillRow smallGuns = sheet.skills().stream().filter(row -> row.name().equals("Small Guns")).findFirst()
        .orElseThrow();
    assertEquals(new SkillRow("Small Guns", "AGI", true, 2), smallGuns, "Tag skills start at rank 2");
  }

  /** CRB p. 28: humanoid hit locations with their d20 ranges, innate DR 0. */
  @Test
  void humanoidHitLocations() {
    SheetContent sheet = SheetContent.of(survivor());
    assertEquals(List.of("Head", "Torso", "Left Arm", "Right Arm", "Left Leg", "Right Leg"),
        sheet.hitLocations().stream().map(HitLocationRow::name).toList());
    assertEquals(new HitLocationRow("Left Leg", "15–17", "0", "0", "0"), sheet.hitLocations().get(4));
    assertEquals("0", sheet.poisonDr());
  }

  /** CRB p. 54: Mister Handy locations and ranges; robots are immune to radiation and poison. */
  @Test
  void misterHandyHitLocations() {
    PlayerCharacter robot = PlayerCharacter.createNew();
    robot.chooseOrigin(CatalogRef.of("mister-handy"));
    robot.startingGear().choosePack(CatalogRef.of("mister-handy"));
    SheetContent sheet = SheetContent.of(robot);
    assertEquals(List.of("Optics", "Main Body", "Arm 1", "Arm 2", "Arm 3", "Thruster"),
        sheet.hitLocations().stream().map(HitLocationRow::name).toList());
    assertEquals("18–20", sheet.hitLocations().get(5).dieRange());
    assertEquals("Immune", sheet.hitLocations().getFirst().radiation());
    assertEquals("Immune", sheet.poisonDr());
    assertEquals("Mister Handy Robot", sheet.perks().getFirst().name(), "origin trait first");
    assertEquals("150", sheet.carryWeightMax());
    assertEquals(3, sheet.notes().stream().filter(note -> note.startsWith("Arm ")).count());
  }

  @Test
  void weaponRowsWithTargetNumberTagAndAmmunition() {
    PlayerCharacter character = survivor();
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("baseball-bat")).withNotes("Spiked"));
    SheetContent sheet = SheetContent.of(character);
    WeaponRow pistol = sheet.weapons().getFirst();
    assertEquals("10mm Pistol", pistol.name());
    assertEquals("Small Guns", pistol.skill());
    assertEquals("7", pistol.targetNumber(), "AGI 5 + Small Guns 2");
    assertTrue(pistol.tagged());
    assertEquals("4 CD", pistol.damage());
    assertEquals(List.of("10mm"), sheet.ammunition());
    WeaponRow bat = sheet.weapons().get(1);
    assertFalse(bat.tagged());
    assertEquals("Melee Weapons", bat.skill());
    assertTrue(sheet.notes().contains("Baseball Bat: Spiked"));
  }

  @Test
  void gearWithQuantitiesWeightsAndCarryLoad() {
    PlayerCharacter character = survivor();
    character.gear().addItem(CarriedItem.of(CatalogRef.of("stimpak")).withQuantity(3));
    character.gear().addItem(CarriedItem.of(new CustomItem("Toolbox", 2, "")).withQuantity(3));
    character.gear().addApparel(WornApparel.of(CatalogRef.of("vault-jumpsuit")));
    character.gear().setApparelCarried(0, true);
    SheetContent sheet = SheetContent.of(character);
    assertEquals(new GearRow("Stimpak ×3", "<1"), sheet.gear().getFirst(), "light items keep the printed weight");
    assertEquals(new GearRow("Toolbox ×3", "6"), sheet.gear().get(1));
    assertEquals("Vault Jumpsuit (carried)", sheet.gear().get(2).item());
    assertTrue(Integer.parseInt(sheet.carryWeightCurrent()) >= 6, "the toolboxes alone weigh 6 lbs.");
    assertEquals("200", sheet.carryWeightMax(), "CRB p. 47: 150 + 10 × STR 5");
  }

  @Test
  void perksAndTraitsWithRankAndSummary() {
    PlayerCharacter character = survivor();
    character.chooseTrait(CatalogRef.of("educated"));
    character.addPerkRank(CatalogRef.of("awareness"), null, null);
    SheetContent sheet = SheetContent.of(character);
    assertEquals(2, sheet.perks().size(), "Survivor has no named origin trait");
    PerkRow trait = sheet.perks().getFirst();
    assertEquals("Educated", trait.name());
    assertEquals("", trait.rank());
    assertTrue(trait.effect().contains("Penalty: "));
    PerkRow perk = sheet.perks().get(1);
    assertEquals("Awareness", perk.name());
    assertEquals("1", perk.rank());
    assertTrue(perk.effect().startsWith("After aiming"));
  }
}
