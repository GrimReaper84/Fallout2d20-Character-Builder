package de.fallout2d20.charbuilder.model.character.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.equipment.ArmAttachment;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.equipment.ArmSelection;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EquipmentRulesTest {

  @Test
  void asksForAPackOnlyWhenTheOriginHasPacks() {
    assertEquals(List.of(), messages(PlayerCharacter.createNew()), "the origin comes first");
    assertEquals(List.of("Choose an equipment pack."), messages(character("survivor")));
    assertEquals(List.of("Choose an equipment pack."), messages(character("protectron")));
  }

  /** WOA p. 22: a Protectron pack needs Winter of Atom enabled (B7 follow-up). */
  @Test
  void reportsAPackOfADisabledBook() {
    StartingEquipment medic = new StartingEquipment(CatalogRef.of("protectron-medic"), List.of(), Map.of(), null,
        false, List.of());
    PlayerCharacter protectron = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("protectron"))
        .enabledBooks(EnumSet.of(Book.CRB)).startingEquipment(medic).build();
    assertEquals(List.of("Protectron Medic needs a book that is not enabled."), messages(protectron));
  }

  /** WOA p. 26: the Zealot leaves the clothing to choose. */
  @Test
  void woaPackChoices() {
    PlayerCharacter zealot = character("child-of-atom");
    zealot.chooseEquipmentPack(CatalogRef.of("zealot"));
    assertEquals(List.of("Choose: Clothing (Zealot pack)."), messages(zealot));
    zealot.chooseEquipmentOption(0, "Drifter outfit");
    assertEquals(List.of(), messages(zealot));
  }

  /** CRB p. 79: Settler with its melee weapon and trinket, Tags without a choice. */
  @Test
  void completeSelectionHasNoProblems() {
    PlayerCharacter settler = character("survivor");
    settler.chooseEquipmentPack(CatalogRef.of("settler"));
    settler.chooseEquipmentOption(0, "Pipe wrench");
    settler.setTrinket("Medal");
    settler.setFreeTag(Skill.LOCKPICK, true);
    assertEquals(List.of(), messages(settler));
  }

  @Test
  void reportsOpenChoicesTrinketAndTagSkillItems() {
    PlayerCharacter settler = character("survivor");
    settler.chooseEquipmentPack(CatalogRef.of("settler"));
    settler.setFreeTag(Skill.MELEE_WEAPONS, true);
    assertEquals(List.of("Choose: Melee weapon (Settler pack).", "Describe or roll the personal trinket.",
        "Choose the Tag skill items for Melee Weapons."), messages(settler));
    settler.chooseTagSkillItem(Skill.MELEE_WEAPONS, "Machete");
    assertEquals(List.of("Choose: Melee weapon (Settler pack).", "Describe or roll the personal trinket."),
        messages(settler));
  }

  /** CRB p. 57: a ghoul may take a Vault Dweller pack with GM permission; a survivor may not. */
  @Test
  void packMustBeOfferedToTheOrigin() {
    PlayerCharacter ghoul = character("ghoul");
    ghoul.chooseEquipmentPack(CatalogRef.of("vault-tec-security"));
    assertEquals(List.of(), messages(ghoul));
    PlayerCharacter survivor = character("survivor");
    survivor.chooseEquipmentPack(CatalogRef.of("vault-tec-security"));
    assertEquals(List.of("Vault-Tec Security is not an equipment pack for this origin."), messages(survivor));
  }

  @Test
  void reportsAPackOutsideTheCatalog() {
    StartingEquipment unknown = new StartingEquipment(new CatalogRef("lone-wanderer", "Lone Wanderer"),
        List.of(), Map.of(), null, false, List.of());
    PlayerCharacter synth = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor"))
        .startingEquipment(unknown).build();
    assertEquals(List.of("\"Lone Wanderer\" is not an equipment pack of the supported books."),
        messages(synth));
  }

  /** CRB p. 77: Nurse Handy leaves two arms to choose; Mister Gutsy fixes all three. */
  @Test
  void misterHandyArms() {
    PlayerCharacter nurse = character("mister-handy");
    nurse.chooseEquipmentPack(CatalogRef.of("nurse-handy"));
    assertEquals(List.of("Choose arm 1.", "Choose arm 3."), messages(nurse));

    PlayerCharacter gutsy = character("mister-handy");
    gutsy.chooseEquipmentPack(CatalogRef.of("mister-gutsy"));
    assertEquals(List.of(), messages(gutsy));
    gutsy.chooseArm(1, ArmSelection.of(ArmAttachment.PINCER));
    assertEquals(List.of("The Mister Gutsy pack does not allow this attachment on arm 2; switch on freely "
        + "configurable arms to keep it."), messages(gutsy));
    gutsy.setFreeArms(true);
    gutsy.chooseArm(2, ArmSelection.ofWeapon("Hunting rifle"));
    assertEquals(List.of(), messages(gutsy), "free arms allow any attachment or weapon");
    gutsy.chooseArm(0, ArmSelection.OPEN);
    assertEquals(List.of("Choose arm 1."), messages(gutsy));
  }

  @Test
  void completeCharactersAreNotChecked() {
    PlayerCharacter complete = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor"))
        .creationComplete(true).build();
    assertEquals(List.of(), messages(complete));
  }

  private static List<String> messages(PlayerCharacter character) {
    return EquipmentRules.problems(character).stream().map(CreationProblem::message).toList();
  }

  private static PlayerCharacter character(String origin) {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of(origin));
    return character;
  }
}
