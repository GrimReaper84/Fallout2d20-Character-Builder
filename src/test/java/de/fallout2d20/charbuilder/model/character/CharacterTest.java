package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
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

  private static Character create(Map<Attribute, Integer> allocation, Map<Skill, SkillState> skills,
      LegacyBaseline baseline) {
    return new Character("Test", CatalogRef.of("survivor"), 1, 0, baseline != null, allocation, skills,
        List.of(), List.of(), Progression.NONE, Vitals.FULL, baseline, null);
  }
}
