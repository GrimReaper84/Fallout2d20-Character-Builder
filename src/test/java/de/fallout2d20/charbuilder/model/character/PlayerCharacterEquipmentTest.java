package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.equipment.ArmAttachment;
import de.fallout2d20.charbuilder.model.character.equipment.ArmSelection;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Starting equipment of a character in creation (CRB p. 50 step 6, pp. 76–81). */
class PlayerCharacterEquipmentTest {

  @Test
  void choosingAPackOpensItsChoices() {
    PlayerCharacter survivor = character("survivor");
    survivor.startingGear().choosePack(CatalogRef.of("raider"));
    assertEquals(Arrays.asList(null, null, null), survivor.startingGear().getSelection().choices());
    assertEquals(List.of(), survivor.startingGear().getSelection().arms());

    survivor.startingGear().chooseOption(0, "Pool cue");
    survivor.startingGear().chooseOption(1, "1 dose of Jet");
    assertEquals(Arrays.asList("Pool cue", "1 dose of Jet", null), survivor.startingGear().getSelection().choices());
    survivor.startingGear().chooseOption(1, null);
    assertNull(survivor.startingGear().getSelection().choices().get(1));

    StartingGear survivorStartingGear = survivor.startingGear();
    assertThrows(IllegalArgumentException.class, () -> survivorStartingGear.chooseOption(0, "Machete"));
    assertThrows(IllegalArgumentException.class, () -> survivorStartingGear.chooseOption(3, null));
    CatalogRef loneWanderer = CatalogRef.of("lone-wanderer");
    assertThrows(IllegalArgumentException.class, () -> survivorStartingGear.choosePack(loneWanderer));
  }

  @Test
  void optionsNeedAPack() {
    PlayerCharacter survivor = character("survivor");
    StartingGear survivorStartingGear = survivor.startingGear();
    assertThrows(IllegalStateException.class, () -> survivorStartingGear.chooseOption(0, null));
    assertThrows(IllegalStateException.class, () -> survivorStartingGear.setFreeArms(true));
    assertThrows(IllegalStateException.class, () -> survivorStartingGear.chooseArm(0, ArmSelection.OPEN));
  }

  /** CRB p. 80: only some packs include a personal trinket; the text survives a pack change. */
  @Test
  void trinketSurvivesPackChanges() {
    PlayerCharacter survivor = character("survivor");
    survivor.startingGear().choosePack(CatalogRef.of("settler"));
    survivor.startingGear().setTrinket("  small wooden ocarina ");
    assertEquals("small wooden ocarina", survivor.startingGear().getSelection().trinket());
    survivor.startingGear().choosePack(CatalogRef.of("wanderer"));
    assertEquals("small wooden ocarina", survivor.startingGear().getSelection().trinket());
    survivor.startingGear().choosePack(CatalogRef.of("raider"));
    survivor.startingGear().choosePack(CatalogRef.of("settler"));
    assertEquals("small wooden ocarina", survivor.startingGear().getSelection().trinket());
    survivor.startingGear().setTrinket(" ");
    assertNull(survivor.startingGear().getSelection().trinket());
  }

  /** CRB p. 81: Explosives, Melee Weapons and Throwing offer a choice; picks survive a pack change. */
  @Test
  void tagSkillItemPicks() {
    PlayerCharacter survivor = character("survivor");
    survivor.startingGear().chooseTagSkillItem(Skill.EXPLOSIVES, "2 baseball grenades");
    survivor.startingGear().choosePack(CatalogRef.of("settler"));
    assertEquals(Map.of(Skill.EXPLOSIVES, "2 baseball grenades"),
        survivor.startingGear().getSelection().tagSkillItems());
    survivor.startingGear().chooseTagSkillItem(Skill.EXPLOSIVES, null);
    assertEquals(Map.of(), survivor.startingGear().getSelection().tagSkillItems());
    StartingGear startingGear = survivor.startingGear();
    assertThrows(IllegalArgumentException.class, () -> startingGear.chooseTagSkillItem(Skill.REPAIR, "Multi-Tool"));
    assertThrows(IllegalArgumentException.class, () -> startingGear.chooseTagSkillItem(Skill.THROWING, "Spear"));
  }

  /** CRB p. 77: a pack sets the arms it fixes and leaves the choosable ones open. */
  @Test
  void misterHandyPackSetsItsArms() {
    PlayerCharacter handy = character("mister-handy");
    handy.startingGear().choosePack(CatalogRef.of("mister-gutsy"));
    assertEquals(List.of(ArmSelection.of(ArmAttachment.AUTO_PISTOL_10MM), ArmSelection.of(ArmAttachment.BUZZ_SAW),
        ArmSelection.of(ArmAttachment.LASER_EMITTER)), handy.startingGear().getSelection().arms());
    assertFalse(handy.startingGear().getSelection().hasPincer());

    handy.startingGear().choosePack(CatalogRef.of("nurse-handy"));
    assertEquals(List.of(ArmSelection.OPEN, ArmSelection.of(ArmAttachment.BUZZ_SAW), ArmSelection.OPEN),
        handy.startingGear().getSelection().arms());
    handy.startingGear().chooseArm(0, ArmSelection.of(ArmAttachment.PINCER));
    assertTrue(handy.startingGear().getSelection().hasPincer());
    StartingGear handyStartingGear = handy.startingGear();
    assertThrows(IllegalArgumentException.class, () -> handyStartingGear.chooseArm(3, ArmSelection.OPEN));
  }

  /** chooseArm is lenient: an arm the pack does not allow is stored and reported by the creation rules. */
  @Test
  void chooseArmAcceptsArmsThePackDoesNotAllow() {
    PlayerCharacter handy = character("mister-handy");
    handy.startingGear().choosePack(CatalogRef.of("mister-gutsy"));
    handy.startingGear().chooseArm(1, ArmSelection.of(ArmAttachment.PINCER));
    assertFalse(handy.startingGear().getSelection().freeArms());
    assertEquals(ArmSelection.of(ArmAttachment.PINCER), handy.startingGear().getSelection().arms().get(1));
  }

  /** Free arms (house-rule option): any attachment or a weapon; switching back restores the pack's arms. */
  @Test
  void freeArmsAndBackToThePack() {
    PlayerCharacter handy = character("mister-handy");
    handy.startingGear().choosePack(CatalogRef.of("nurse-handy"));
    handy.startingGear().chooseArm(0, ArmSelection.of(ArmAttachment.PINCER));
    handy.startingGear().setFreeArms(true);
    assertTrue(handy.startingGear().getSelection().freeArms());
    assertEquals(ArmSelection.of(ArmAttachment.PINCER), handy.startingGear().getSelection().arms().getFirst());
    handy.startingGear().chooseArm(1, ArmSelection.of(ArmAttachment.PINCER));
    handy.startingGear().chooseArm(2, ArmSelection.ofWeapon("Hunting rifle"));

    handy.startingGear().setFreeArms(false);
    assertFalse(handy.startingGear().getSelection().freeArms());
    assertEquals(List.of(ArmSelection.of(ArmAttachment.PINCER), ArmSelection.of(ArmAttachment.BUZZ_SAW),
        ArmSelection.OPEN), handy.startingGear().getSelection().arms());

    handy.startingGear().setFreeArms(true);
    handy.startingGear().choosePack(CatalogRef.of("mister-handy"));
    assertFalse(handy.startingGear().getSelection().freeArms(), "a new pack starts with its own arms");

    PlayerCharacter survivor = character("survivor");
    survivor.startingGear().choosePack(CatalogRef.of("settler"));
    StartingGear survivorStartingGear = survivor.startingGear();
    assertThrows(IllegalStateException.class, () -> survivorStartingGear.setFreeArms(true));
  }

  /** Ghoul and Survivor share the Wastelander packs (CRB p. 79); other origins do not. */
  @Test
  void originChangeKeepsOnlyAPackStillOffered() {
    PlayerCharacter character = character("survivor");
    character.startingGear().choosePack(CatalogRef.of("settler"));
    character.startingGear().chooseOption(0, "Pipe wrench");
    character.chooseOrigin(CatalogRef.of("ghoul"));
    assertEquals(CatalogRef.of("settler"), character.startingGear().getSelection().pack());
    assertEquals(List.of("Pipe wrench"), character.startingGear().getSelection().choices());
    character.chooseOrigin(CatalogRef.of("vault-dweller"));
    assertEquals(StartingEquipment.NONE, character.startingGear().getSelection());

    character.chooseOrigin(CatalogRef.of("ghoul"));
    character.startingGear().choosePack(CatalogRef.of("vault-tec-security"));
    character.chooseOrigin(CatalogRef.of("vault-dweller"));
    assertEquals(CatalogRef.of("vault-tec-security"), character.startingGear().getSelection().pack());
  }

  @Test
  void equipmentIsFixedAfterCreationButNotesStayEditable() {
    StartingEquipment settler = new StartingEquipment(CatalogRef.of("settler"), Arrays.asList("Knuckles"),
        Map.of(), "Medal", false, List.of());
    PlayerCharacter complete = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor"))
        .creationComplete(true).startingEquipment(settler).inventoryNotes("Notes").build();
    assertEquals(settler, complete.startingGear().getSelection());
    StartingGear completeStartingGear = complete.startingGear();
    CatalogRef raider = CatalogRef.of("raider");
    assertThrows(IllegalStateException.class, () -> completeStartingGear.choosePack(raider));
    assertThrows(IllegalStateException.class, () -> completeStartingGear.chooseOption(0, "Rolling pin"));
    assertThrows(IllegalStateException.class, () -> completeStartingGear.chooseTagSkillItem(Skill.THROWING, null));
    assertThrows(IllegalStateException.class, () -> completeStartingGear.setTrinket("Wedding ring"));
    assertThrows(IllegalStateException.class, () -> completeStartingGear.setFreeArms(true));
    assertThrows(IllegalStateException.class, () -> completeStartingGear.chooseArm(0, ArmSelection.OPEN));
    assertEquals("Notes", complete.gear().getNotes());
    complete.gear().setNotes("42 caps, 3 bobby pins");
    assertEquals("42 caps, 3 bobby pins", complete.gear().getNotes());
    complete.gear().setNotes(null);
    assertEquals("", complete.gear().getNotes());
    assertEquals("", PlayerCharacter.createNew().gear().getNotes());
    assertEquals(StartingEquipment.NONE, PlayerCharacter.createNew().startingGear().getSelection());
  }

  private static PlayerCharacter character(String origin) {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of(origin));
    return character;
  }
}
