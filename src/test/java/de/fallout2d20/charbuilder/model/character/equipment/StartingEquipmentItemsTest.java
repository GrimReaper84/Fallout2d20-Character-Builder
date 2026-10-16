package de.fallout2d20.charbuilder.model.character.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.equipment.ArmAttachment;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.SkillState;
import de.fallout2d20.charbuilder.model.character.Vitals;
import de.fallout2d20.charbuilder.model.character.item.Inventory;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StartingEquipmentItemsTest {

  /** CRB p. 81: only the Tags of creation grant items; a Tag! taken after a level-up does not. */
  @Test
  void tagPerkAfterCreationGrantsNoItems() {
    Character settler = character("survivor");
    settler.chooseEquipmentPack(CatalogRef.of("settler"));
    settler.setFreeTag(Skill.LOCKPICK, true);
    settler.completeCreation();
    settler.addXp(1_000);
    settler.addPerkRank(CatalogRef.of("tag"), null, Skill.SCIENCE);
    settler.applyAdvancement();
    assertTrue(settler.isTagged(Skill.SCIENCE));
    assertEquals(Set.of(Skill.LOCKPICK), settler.getStartingTagSkills());
    assertEquals(List.of(Skill.LOCKPICK),
        StartingEquipmentItems.taggedSkillItems(settler).map(items -> items.skill()).toList());
  }

  /** Like the legacy save JC.json: Survivor with the Settler pack (CRB p. 79) and three Tags (CRB p. 81). */
  @Test
  void settlerWithTagSkillItems() {
    Character settler = character("survivor");
    settler.chooseEquipmentPack(CatalogRef.of("settler"));
    settler.chooseEquipmentOption(0, "Pipe wrench");
    settler.setTrinket("Small wooden ocarina");
    settler.setFreeTag(Skill.LOCKPICK, true);
    settler.setFreeTag(Skill.REPAIR, true);
    settler.setFreeTag(Skill.SCIENCE, true);
    assertEquals(List.of("Tough clothing", "Pipe gun", "6 + 3 CD rounds of .38 ammunition",
        "2 rolls on the random food table (CRB p. 202)", "Pipe wrench", "Personal trinket: Small wooden ocarina",
        "Lockpick: 4 + 2 CD bobby pins", "Repair: Multi-Tool", "Science: Lab coat, 1 dose of Mentats"),
        StartingEquipmentItems.lines(settler));
    assertEquals(new StartingCaps(45, 0, 0), StartingEquipmentItems.caps(settler));
  }

  @Test
  void openChoicesSayWhatIsMissing() {
    Character raider = character("survivor");
    raider.chooseEquipmentPack(CatalogRef.of("raider"));
    raider.setFreeTag(Skill.THROWING, true);
    List<String> lines = StartingEquipmentItems.lines(raider);
    assertTrue(lines.contains("Melee weapon: not chosen yet"), lines.toString());
    assertTrue(lines.contains("Throwing: 4 + 2 CD throwing knives or 2 + 1 CD tomahawks (not chosen yet)"),
        lines.toString());
    raider.chooseTagSkillItem(Skill.THROWING, "2 + 1 CD tomahawks");
    assertTrue(StartingEquipmentItems.lines(raider).contains("Throwing: 2 + 1 CD tomahawks"));

    Character settler = character("survivor");
    settler.chooseEquipmentPack(CatalogRef.of("settler"));
    assertTrue(StartingEquipmentItems.lines(settler).contains("Personal trinket: not described yet"));
  }

  /** CRB p. 52: Survival is a Tag skill of every Ghoul, so it grants its items too (user decision). */
  @Test
  void originTagSkillsGrantItems() {
    Character ghoul = character("ghoul");
    assertEquals(List.of("Survival: 2 purified water, 1 iguana on a stick"), StartingEquipmentItems.lines(ghoul));
  }

  /** CRB pp. 55, 77: weapon arms come with 20 shots; Mister Gutsy has no pincer. */
  @Test
  void misterGutsyArmsComeWithAmmo() {
    Character gutsy = character("mister-handy");
    gutsy.chooseEquipmentPack(CatalogRef.of("mister-gutsy"));
    assertEquals(List.of("Mister Gutsy plating", "Recon sensors mod",
        "Arm 1: 10mm Auto Pistol, with 20 shots of 10mm", "Arm 2: Buzz-Saw",
        "Arm 3: Laser Emitter, with 20 shots of Fusion Cell"), StartingEquipmentItems.lines(gutsy));
    assertEquals(new StartingCaps(10, 0, 0), StartingEquipmentItems.caps(gutsy));
  }

  /** Nurse Handy with Buzz-Saw and Flamer has no pincer; a free weapon arm gets 20 shots too (user decision). */
  @Test
  void nurseHandyAndFreeArms() {
    Character nurse = character("mister-handy");
    nurse.chooseEquipmentPack(CatalogRef.of("nurse-handy"));
    nurse.chooseArm(0, ArmSelection.of(ArmAttachment.BUZZ_SAW));
    List<String> lines = StartingEquipmentItems.lines(nurse);
    assertTrue(lines.contains("Arm 3: not chosen yet"), lines.toString());
    nurse.chooseArm(2, ArmSelection.of(ArmAttachment.FLAMER));
    assertTrue(StartingEquipmentItems.lines(nurse).contains("Arm 3: Flamer, with 20 shots of Flamer Fuel"));

    nurse.setFreeArms(true);
    nurse.chooseArm(2, ArmSelection.ofWeapon("Hunting rifle"));
    assertTrue(StartingEquipmentItems.lines(nurse)
        .contains("Arm 3: Hunting rifle, with 20 shots of its ammunition"));
  }

  /** CRB p. 81: Barter grants 2d20 caps, a starting level above 1 extra caps (level 2: 100). */
  @Test
  void capsAddPackLevelAndBarter() {
    Character trader = character("survivor");
    trader.chooseEquipmentPack(CatalogRef.of("trader"));
    trader.setFreeTag(Skill.BARTER, true);
    trader.setStartingLevel(2);
    StartingCaps caps = StartingEquipmentItems.caps(trader);
    assertEquals(new StartingCaps(50, 100, 2), caps);
    assertEquals(150, caps.fixedTotal());
    assertEquals("150 caps + 2d20", caps.toString());
    assertTrue(StartingEquipmentItems.lines(trader).stream().noneMatch(line -> line.startsWith("Barter")));

    Character noPack = character("survivor");
    noPack.setFreeTag(Skill.BARTER, true);
    assertEquals("2d20 caps", StartingEquipmentItems.caps(noPack).toString());
    assertEquals("0 caps", StartingEquipmentItems.caps(Character.createNew()).toString());
    assertThrows(IllegalArgumentException.class, () -> new StartingCaps(-1, 0, 0));
  }

  /** CRB p. 81: a completed character keeps the caps of its stored starting level. */
  @Test
  void completedCharactersKeepTheLevelCaps() {
    Character trader = character("survivor");
    trader.chooseEquipmentPack(CatalogRef.of("trader"));
    trader.setStartingLevel(2);
    trader.completeCreation();
    assertEquals(new StartingCaps(50, 100, 0), StartingEquipmentItems.caps(trader));
  }

  /** An imported character's starting level is unknown (its level may have grown): no level caps then. */
  @Test
  void unknownStartingLevelGivesNoLevelCaps() {
    StartingEquipment wanderer = new StartingEquipment(CatalogRef.of("wanderer"), List.of(), Map.of(), null,
        false, List.of());
    Character complete = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 5, 0, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(Skill.BARTER, new SkillState(true, 0, 0)), List.of(),
        List.of(), Progression.NONE, Vitals.FULL, null, null, wanderer, "", null, null, List.of(), Map.of(), List.of(), Inventory.EMPTY);
    assertEquals(new StartingCaps(30, 0, 2), StartingEquipmentItems.caps(complete));
    assertTrue(StartingEquipmentItems.lines(complete).contains("Melee weapon: not chosen yet"));
  }

  private static Character character(String origin) {
    Character character = Character.createNew();
    character.chooseOrigin(CatalogRef.of(origin));
    return character;
  }
}
