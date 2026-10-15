package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CharacterTest {

  @Test
  void newCharacterStartsWithBaseValues() {
    Character character = Character.createNew();
    for (Attribute attribute : Attribute.values()) {
      assertEquals(5, character.attributeValue(attribute));
    }
    for (Skill skill : Skill.values()) {
      assertEquals(0, character.skillRank(skill));
    }
    assertFalse(character.isLegacyImport());
    assertFalse(character.isCreationComplete());
  }

  /** The Core Rulebook can never be switched off (user decision). */
  @Test
  void coreRulebookIsAlwaysEnabled() {
    assertEquals(EnumSet.allOf(Book.class), Character.createNew().getEnabledBooks());
    Character character = new Character("Test", null, OriginChoices.NONE, 1, 0, false, EnumSet.of(Book.WOA), Map.of(),
        Map.of(), List.of(), List.of(), Progression.NONE, Vitals.FULL, null, null);
    assertEquals(EnumSet.of(Book.CRB, Book.WOA), character.getEnabledBooks());
  }

  /** CRB p. 58: base 5 plus spent points; an attribute may be lowered to 4. */
  @Test
  void attributeIsBasePlusAllocation() {
    Character character = create(Map.of(Attribute.STRENGTH, 2, Attribute.CHARISMA, -1), Map.of(), null);
    assertEquals(7, character.attributeValue(Attribute.STRENGTH));
    assertEquals(4, character.attributeValue(Attribute.CHARISMA));
  }

  /** CRB p. 58: a Tag skill starts at rank 2. */
  @Test
  void tagSkillAddsTwoRanks() {
    Character character = create(Map.of(),
        Map.of(Skill.REPAIR, new SkillState(true, 1, 0), Skill.SNEAK, new SkillState(false, 2, 1)), null);
    assertEquals(3, character.skillRank(Skill.REPAIR));
    assertEquals(3, character.skillRank(Skill.SNEAK));
  }

  @Test
  void legacyBaselineReplacesBaseAndTagBonus() {
    Map<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
    for (Attribute attribute : Attribute.values()) {
      attributes.put(attribute, 6);
    }
    attributes.put(Attribute.STRENGTH, 4);
    Map<Skill, Integer> ranks = new EnumMap<>(Skill.class);
    for (Skill skill : Skill.values()) {
      ranks.put(skill, 0);
    }
    ranks.put(Skill.LOCKPICK, 3);
    Character character = create(Map.of(),
        Map.of(Skill.LOCKPICK, new SkillState(true, 0, 1)), new LegacyBaseline(attributes, ranks));

    assertTrue(character.isLegacyImport());
    assertEquals(4, character.attributeValue(Attribute.STRENGTH));
    assertEquals(4, character.skillRank(Skill.LOCKPICK));
    assertTrue(character.skillState(Skill.LOCKPICK).tagged());
  }

  /** CRB p. 53: a new Super Mutant starts with STR and END 7; an imported one keeps its baseline. */
  @Test
  void superMutantGetsAttributeModifiers() {
    Character mutant = Character.createNew();
    mutant.chooseOrigin(CatalogRef.of("super-mutant"));
    assertEquals(7, mutant.attributeValue(Attribute.STRENGTH));
    assertEquals(7, mutant.attributeValue(Attribute.ENDURANCE));
    assertEquals(5, mutant.attributeValue(Attribute.INTELLIGENCE));

    Character imported = create(Map.of(), Map.of(), baseline(8));
    assertEquals(8, new Character("Imported", CatalogRef.of("super-mutant"), OriginChoices.NONE, 1, 0,
        true, EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(), Progression.NONE,
        Vitals.FULL, imported.getLegacyBaseline(), null).attributeValue(Attribute.STRENGTH));
  }

  /** CRB p. 52 (user reading): Survival is tagged in addition; a free Survival Tag is removed. */
  @Test
  void ghoulSurvivalIsAnAdditionalTag() {
    Character ghoul = create(Map.of(), Map.of(Skill.SURVIVAL, new SkillState(true, 1, 0)), null);
    ghoul.chooseOrigin(CatalogRef.of("ghoul"));
    assertFalse(ghoul.skillState(Skill.SURVIVAL).tagged());
    assertTrue(ghoul.isTagged(Skill.SURVIVAL));
    assertEquals(3, ghoul.skillRank(Skill.SURVIVAL));
  }

  /** CRB p. 51: the extra Tag skill of the Brotherhood counts as a Tag skill. */
  @Test
  void originTagSkillCountsAsTag() {
    Character initiate = Character.createNew();
    initiate.chooseOrigin(CatalogRef.of("brotherhood-initiate"));
    initiate.chooseOriginTagSkill(Skill.SCIENCE);
    assertTrue(initiate.isTagged(Skill.SCIENCE));
    assertEquals(2, initiate.skillRank(Skill.SCIENCE));
    assertFalse(initiate.isTagged(Skill.REPAIR));
  }

  @Test
  void chooseOriginResetsChoicesAndTraitsButKeepsAllocation() {
    Character character = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 1, 0,
        false, EnumSet.allOf(Book.class), Map.of(Attribute.AGILITY, 2), Map.of(),
        List.of(new TraitSelection(CatalogRef.of("small-frame"), false, List.of())), List.of(),
        Progression.NONE, Vitals.FULL, null, null);
    character.chooseOrigin(CatalogRef.of("protectron"));
    character.setPurpose("  Medical Responder ");
    assertEquals("Medical Responder", character.getOriginChoices().purpose());

    character.chooseOrigin(CatalogRef.of("vault-dweller"));
    assertEquals(OriginChoices.NONE, character.getOriginChoices());
    assertTrue(character.getTraits().isEmpty());
    assertEquals(7, character.attributeValue(Attribute.AGILITY));
  }

  @Test
  void completeCharactersCannotChangeTheirOrigin() {
    Character complete = create(Map.of(), Map.of(), baseline(6));
    assertThrows(IllegalStateException.class, () -> complete.chooseOrigin(CatalogRef.of("ghoul")));
    assertThrows(IllegalStateException.class, () -> complete.setNoPincer(true));
  }

  private static LegacyBaseline baseline(int value) {
    Map<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
    for (Attribute attribute : Attribute.values()) {
      attributes.put(attribute, value);
    }
    Map<Skill, Integer> ranks = new EnumMap<>(Skill.class);
    for (Skill skill : Skill.values()) {
      ranks.put(skill, 0);
    }
    return new LegacyBaseline(attributes, ranks);
  }

  private static Character create(Map<Attribute, Integer> allocation, Map<Skill, SkillState> skills,
      LegacyBaseline baseline) {
    return new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 1, 0, baseline != null,
        EnumSet.allOf(Book.class), allocation, skills,
        List.of(), List.of(), Progression.NONE, Vitals.FULL, baseline, null);
  }
}
