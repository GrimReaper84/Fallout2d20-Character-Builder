package de.fallout2d20.charbuilder.model.character.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreationRulesTest {

  /** CRB p. 50: the name hint first, then the creation steps in book order. */
  @Test
  void collectsAllRulesInCreationOrder() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    List<String> topics = CreationRules.problems(character).stream().map(CreationProblem::topic).toList();
    List<String> steps = List.of("Name", "Traits", "S.P.E.C.I.A.L.", "Tag skills", "Skills", "Perks", "Equipment");
    List<Integer> firstIndex = steps.stream().map(topics::indexOf).toList();
    assertTrue(firstIndex.stream().allMatch(index -> index >= 0), "every step has a hint: " + topics);
    assertEquals(firstIndex.stream().sorted().toList(), firstIndex, "hints in creation order: " + topics);
  }

  /** Each hint names the part of the character it belongs to, so the view can show it in its tab. */
  @Test
  void hintsNameTheirArea() {
    PlayerCharacter character = PlayerCharacter.createNew();
    assertEquals(HintArea.HEADER, area(character, "Name"));
    assertEquals(HintArea.HEADER, area(character, "Origin"));
    character.chooseOrigin(CatalogRef.of("survivor"));
    assertEquals(HintArea.STATS, area(character, "Traits"));
    assertEquals(HintArea.STATS, area(character, "S.P.E.C.I.A.L."));
    assertEquals(HintArea.STATS, area(character, "Tag skills"));
    assertEquals(HintArea.STATS, area(character, "Skills"));
    assertEquals(HintArea.PERKS, area(character, "Perks"));
    assertEquals(HintArea.GEAR, area(character, "Equipment"));
    PlayerCharacter vaultDweller = PlayerCharacter.createNew();
    vaultDweller.chooseOrigin(CatalogRef.of("vault-dweller"));
    assertEquals(HintArea.STATS, area(vaultDweller, "Origin"), "the extra Tag skill is chosen with the skills");
  }

  private static HintArea area(PlayerCharacter character, String topic) {
    return CreationRules.problems(character).stream().filter(p -> p.topic().equals(topic)).findFirst()
        .orElseThrow(() -> new AssertionError("no hint for " + topic)).area();
  }

  @Test
  void nameHintOnlyForABlankName() {
    PlayerCharacter character = PlayerCharacter.createNew();
    assertEquals("Name", CreationRules.problems(character).getFirst().topic());
    character.setName("  ");
    assertEquals("Name", CreationRules.problems(character).getFirst().topic());
    character.setName("Nora");
    assertTrue(CreationRules.problems(character).stream().noneMatch(p -> p.topic().equals("Name")));
  }

  /**
   * A level-1 Survivor built by the book (CRB pp. 56–58, 66, 79, 81): two traits, 5 S.P.E.C.I.A.L.
   * points, three Tags, 9 + INT skill points, Jury Rigging (no requirements), the Settler pack – nothing
   * left to hint at; completing it leaves nothing either. The Tag skill items of Small Guns, Repair and
   * Sneak offer no choice (CRB p. 81), so no pick is needed.
   */
  @Test
  void aCharacterBuiltByTheBookHasNoHints() {
    PlayerCharacter nora = PlayerCharacter.createNew();
    nora.setName("Nora");
    nora.chooseOrigin(CatalogRef.of("survivor"));
    nora.chooseTrait(CatalogRef.of("fast-shot"));
    nora.chooseTrait(CatalogRef.of("heavy-handed"));
    nora.setAttributeAllocation(Attribute.STRENGTH, 2);
    nora.setAttributeAllocation(Attribute.PERCEPTION, 1);
    nora.setAttributeAllocation(Attribute.ENDURANCE, 1);
    nora.setAttributeAllocation(Attribute.AGILITY, 1);
    for (Skill tag : List.of(Skill.SMALL_GUNS, Skill.REPAIR, Skill.SNEAK)) {
      nora.setFreeTag(tag, true);
      nora.setCreationRanks(tag, 1);
    }
    nora.setCreationRanks(Skill.ATHLETICS, 3);
    nora.setCreationRanks(Skill.LOCKPICK, 3);
    nora.setCreationRanks(Skill.MEDICINE, 3);
    nora.setCreationRanks(Skill.BARTER, 2);
    nora.addPerkRank(CatalogRef.of("jury-rigging"), null, null);
    nora.chooseEquipmentPack(CatalogRef.of("settler"));
    nora.chooseEquipmentOption(0, "Pipe wrench");
    nora.setTrinket("Small wooden ocarina");
    assertEquals(List.of(), CreationRules.problems(nora));

    nora.completeCreation();
    assertEquals(List.of(), CreationRules.problems(nora));
  }

  @Test
  void completeCharactersHaveNoHints() {
    PlayerCharacter unnamed = PlayerCharacter.createNew();
    unnamed.completeCreation();
    assertEquals(List.of(), CreationRules.problems(unnamed));
  }
}
