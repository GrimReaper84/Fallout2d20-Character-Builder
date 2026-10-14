package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SkillTest {

  /** CRB p. 44, skills summary table. */
  @Test
  void governingAttributesMatchTheCoreRulebook() {
    Map<String, Attribute> book = new LinkedHashMap<>();
    book.put("Athletics", Attribute.STRENGTH);
    book.put("Barter", Attribute.CHARISMA);
    book.put("Big Guns", Attribute.ENDURANCE);
    book.put("Energy Weapons", Attribute.PERCEPTION);
    book.put("Explosives", Attribute.PERCEPTION);
    book.put("Lockpick", Attribute.PERCEPTION);
    book.put("Medicine", Attribute.INTELLIGENCE);
    book.put("Melee Weapons", Attribute.STRENGTH);
    book.put("Pilot", Attribute.PERCEPTION);
    book.put("Repair", Attribute.INTELLIGENCE);
    book.put("Science", Attribute.INTELLIGENCE);
    book.put("Small Guns", Attribute.AGILITY);
    book.put("Sneak", Attribute.AGILITY);
    book.put("Speech", Attribute.CHARISMA);
    book.put("Survival", Attribute.ENDURANCE);
    book.put("Throwing", Attribute.AGILITY);
    book.put("Unarmed", Attribute.STRENGTH);

    assertEquals(17, Skill.values().length);
    book.forEach((name, attribute) ->
        assertEquals(attribute, Skill.fromDisplayName(name).orElseThrow().governingAttribute(), name));
  }

  @Test
  void idsAreUniqueAndResolvable() {
    assertEquals(Skill.values().length, Arrays.stream(Skill.values()).map(Skill::id).distinct().count());
    for (Skill skill : Skill.values()) {
      assertEquals(skill, Skill.fromId(skill.id()).orElseThrow());
    }
  }

  @Test
  void unknownDisplayNameIsEmpty() {
    assertTrue(Skill.fromDisplayName("Hacking").isEmpty());
  }
}
