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
    survivor.chooseEquipmentPack(CatalogRef.of("raider"));
    assertEquals(Arrays.asList(null, null, null), survivor.getStartingEquipment().choices());
    assertEquals(List.of(), survivor.getStartingEquipment().arms());

    survivor.chooseEquipmentOption(0, "Pool cue");
    survivor.chooseEquipmentOption(1, "1 dose of Jet");
    assertEquals(Arrays.asList("Pool cue", "1 dose of Jet", null), survivor.getStartingEquipment().choices());
    survivor.chooseEquipmentOption(1, null);
    assertNull(survivor.getStartingEquipment().choices().get(1));

    assertThrows(IllegalArgumentException.class, () -> survivor.chooseEquipmentOption(0, "Machete"));
    assertThrows(IllegalArgumentException.class, () -> survivor.chooseEquipmentOption(3, null));
    assertThrows(IllegalArgumentException.class,
        () -> survivor.chooseEquipmentPack(CatalogRef.of("lone-wanderer")));
  }

  @Test
  void optionsNeedAPack() {
    PlayerCharacter survivor = character("survivor");
    assertThrows(IllegalStateException.class, () -> survivor.chooseEquipmentOption(0, null));
    assertThrows(IllegalStateException.class, () -> survivor.setFreeArms(true));
    assertThrows(IllegalStateException.class, () -> survivor.chooseArm(0, ArmSelection.OPEN));
  }

  /** CRB p. 80: only some packs include a personal trinket; the text survives a pack change. */
  @Test
  void trinketSurvivesPackChanges() {
    PlayerCharacter survivor = character("survivor");
    survivor.chooseEquipmentPack(CatalogRef.of("settler"));
    survivor.setTrinket("  small wooden ocarina ");
    assertEquals("small wooden ocarina", survivor.getStartingEquipment().trinket());
    survivor.chooseEquipmentPack(CatalogRef.of("wanderer"));
    assertEquals("small wooden ocarina", survivor.getStartingEquipment().trinket());
    survivor.chooseEquipmentPack(CatalogRef.of("raider"));
    survivor.chooseEquipmentPack(CatalogRef.of("settler"));
    assertEquals("small wooden ocarina", survivor.getStartingEquipment().trinket());
    survivor.setTrinket(" ");
    assertNull(survivor.getStartingEquipment().trinket());
  }

  /** CRB p. 81: Explosives, Melee Weapons and Throwing offer a choice; picks survive a pack change. */
  @Test
  void tagSkillItemPicks() {
    PlayerCharacter survivor = character("survivor");
    survivor.chooseTagSkillItem(Skill.EXPLOSIVES, "2 baseball grenades");
    survivor.chooseEquipmentPack(CatalogRef.of("settler"));
    assertEquals(Map.of(Skill.EXPLOSIVES, "2 baseball grenades"),
        survivor.getStartingEquipment().tagSkillItems());
    survivor.chooseTagSkillItem(Skill.EXPLOSIVES, null);
    assertEquals(Map.of(), survivor.getStartingEquipment().tagSkillItems());
    assertThrows(IllegalArgumentException.class, () -> survivor.chooseTagSkillItem(Skill.REPAIR, "Multi-Tool"));
    assertThrows(IllegalArgumentException.class, () -> survivor.chooseTagSkillItem(Skill.THROWING, "Spear"));
  }

  /** CRB p. 77: a pack sets the arms it fixes and leaves the choosable ones open. */
  @Test
  void misterHandyPackSetsItsArms() {
    PlayerCharacter handy = character("mister-handy");
    handy.chooseEquipmentPack(CatalogRef.of("mister-gutsy"));
    assertEquals(List.of(ArmSelection.of(ArmAttachment.AUTO_PISTOL_10MM), ArmSelection.of(ArmAttachment.BUZZ_SAW),
        ArmSelection.of(ArmAttachment.LASER_EMITTER)), handy.getStartingEquipment().arms());
    assertFalse(handy.getStartingEquipment().hasPincer());

    handy.chooseEquipmentPack(CatalogRef.of("nurse-handy"));
    assertEquals(List.of(ArmSelection.OPEN, ArmSelection.of(ArmAttachment.BUZZ_SAW), ArmSelection.OPEN),
        handy.getStartingEquipment().arms());
    handy.chooseArm(0, ArmSelection.of(ArmAttachment.PINCER));
    assertTrue(handy.getStartingEquipment().hasPincer());
    assertThrows(IllegalArgumentException.class, () -> handy.chooseArm(3, ArmSelection.OPEN));
  }

  /** chooseArm is lenient: an arm the pack does not allow is stored and reported by the creation rules. */
  @Test
  void chooseArmAcceptsArmsThePackDoesNotAllow() {
    PlayerCharacter handy = character("mister-handy");
    handy.chooseEquipmentPack(CatalogRef.of("mister-gutsy"));
    handy.chooseArm(1, ArmSelection.of(ArmAttachment.PINCER));
    assertFalse(handy.getStartingEquipment().freeArms());
    assertEquals(ArmSelection.of(ArmAttachment.PINCER), handy.getStartingEquipment().arms().get(1));
  }

  /** Free arms (house-rule option): any attachment or a weapon; switching back restores the pack's arms. */
  @Test
  void freeArmsAndBackToThePack() {
    PlayerCharacter handy = character("mister-handy");
    handy.chooseEquipmentPack(CatalogRef.of("nurse-handy"));
    handy.chooseArm(0, ArmSelection.of(ArmAttachment.PINCER));
    handy.setFreeArms(true);
    assertTrue(handy.getStartingEquipment().freeArms());
    assertEquals(ArmSelection.of(ArmAttachment.PINCER), handy.getStartingEquipment().arms().getFirst());
    handy.chooseArm(1, ArmSelection.of(ArmAttachment.PINCER));
    handy.chooseArm(2, ArmSelection.ofWeapon("Hunting rifle"));

    handy.setFreeArms(false);
    assertFalse(handy.getStartingEquipment().freeArms());
    assertEquals(List.of(ArmSelection.of(ArmAttachment.PINCER), ArmSelection.of(ArmAttachment.BUZZ_SAW),
        ArmSelection.OPEN), handy.getStartingEquipment().arms());

    handy.setFreeArms(true);
    handy.chooseEquipmentPack(CatalogRef.of("mister-handy"));
    assertFalse(handy.getStartingEquipment().freeArms(), "a new pack starts with its own arms");

    PlayerCharacter survivor = character("survivor");
    survivor.chooseEquipmentPack(CatalogRef.of("settler"));
    assertThrows(IllegalStateException.class, () -> survivor.setFreeArms(true));
  }

  /** Ghoul and Survivor share the Wastelander packs (CRB p. 79); other origins do not. */
  @Test
  void originChangeKeepsOnlyAPackStillOffered() {
    PlayerCharacter character = character("survivor");
    character.chooseEquipmentPack(CatalogRef.of("settler"));
    character.chooseEquipmentOption(0, "Pipe wrench");
    character.chooseOrigin(CatalogRef.of("ghoul"));
    assertEquals(CatalogRef.of("settler"), character.getStartingEquipment().pack());
    assertEquals(List.of("Pipe wrench"), character.getStartingEquipment().choices());
    character.chooseOrigin(CatalogRef.of("vault-dweller"));
    assertEquals(StartingEquipment.NONE, character.getStartingEquipment());

    character.chooseOrigin(CatalogRef.of("ghoul"));
    character.chooseEquipmentPack(CatalogRef.of("vault-tec-security"));
    character.chooseOrigin(CatalogRef.of("vault-dweller"));
    assertEquals(CatalogRef.of("vault-tec-security"), character.getStartingEquipment().pack());
  }

  @Test
  void equipmentIsFixedAfterCreationButNotesStayEditable() {
    StartingEquipment settler = new StartingEquipment(CatalogRef.of("settler"), Arrays.asList("Knuckles"),
        Map.of(), "Medal", false, List.of());
    PlayerCharacter complete = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor"))
        .creationComplete(true).startingEquipment(settler).inventoryNotes("Notes").build();
    assertEquals(settler, complete.getStartingEquipment());
    assertThrows(IllegalStateException.class, () -> complete.chooseEquipmentPack(CatalogRef.of("raider")));
    assertThrows(IllegalStateException.class, () -> complete.chooseEquipmentOption(0, "Rolling pin"));
    assertThrows(IllegalStateException.class, () -> complete.chooseTagSkillItem(Skill.THROWING, null));
    assertThrows(IllegalStateException.class, () -> complete.setTrinket("Wedding ring"));
    assertThrows(IllegalStateException.class, () -> complete.setFreeArms(true));
    assertThrows(IllegalStateException.class, () -> complete.chooseArm(0, ArmSelection.OPEN));
    assertEquals("Notes", complete.getInventoryNotes());
    complete.setInventoryNotes("42 caps, 3 bobby pins");
    assertEquals("42 caps, 3 bobby pins", complete.getInventoryNotes());
    complete.setInventoryNotes(null);
    assertEquals("", complete.getInventoryNotes());
    assertEquals("", PlayerCharacter.createNew().getInventoryNotes());
    assertEquals(StartingEquipment.NONE, PlayerCharacter.createNew().getStartingEquipment());
  }

  private static PlayerCharacter character(String origin) {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of(origin));
    return character;
  }
}
