package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlayerCharacterBuilderTest {

  @Test
  void defaultsAreANewEmptyCharacterInCreation() {
    PlayerCharacter character = PlayerCharacter.builder().build();
    assertEquals("", character.getName());
    assertNull(character.getOrigin());
    assertEquals(1, character.advancement().getLevel());
    assertEquals(0, character.advancement().getXp());
    assertFalse(character.isCreationComplete());
    assertEquals(EnumSet.allOf(Book.class), character.getEnabledBooks());
    for (Attribute attribute : Attribute.values()) {
      assertEquals(0, character.abilities().attributeAllocation(attribute));
    }
    for (Skill skill : Skill.values()) {
      assertEquals(SkillState.UNTRAINED, character.abilities().skillState(skill));
    }
    assertTrue(character.getTraits().isEmpty());
    assertTrue(character.getPerks().isEmpty());
    assertEquals(Vitals.FULL, character.session().getVitals());
    assertTrue(character.gear().getWeapons().isEmpty());
    assertTrue(character.gear().getItems().isEmpty());
    assertTrue(character.getCreatedWith().isEmpty());
  }

  @Test
  void coreRulebookIsAlwaysEnabled() {
    PlayerCharacter character = PlayerCharacter.builder().enabledBooks(Set.of(Book.WOA)).build();
    assertEquals(EnumSet.of(Book.CRB, Book.WOA), character.getEnabledBooks());
  }

  @Test
  void ammunitionWithoutShotsIsDropped() {
    PlayerCharacter character = PlayerCharacter.builder().ammunition(Map.of("10mm", 12, "fusion-cell", 0)).build();
    assertEquals(Map.of("10mm", 12), character.gear().getAmmunition());
  }

  @Test
  void rejectsInvalidParts() {
    PlayerCharacterBuilder noLevel = PlayerCharacter.builder().level(0);
    assertThrows(IllegalArgumentException.class, noLevel::build);
    PlayerCharacterBuilder negativeXp = PlayerCharacter.builder().xp(-1);
    assertThrows(IllegalArgumentException.class, negativeXp::build);
    PlayerCharacterBuilder startingLevelInCreation = PlayerCharacter.builder().level(2).startingLevel(1);
    assertThrows(IllegalArgumentException.class, startingLevelInCreation::build);
    PlayerCharacterBuilder tagsInCreation = PlayerCharacter.builder().startingTagSkills(Set.of(Skill.SCIENCE));
    assertThrows(IllegalArgumentException.class, tagsInCreation::build);
    PlayerCharacterBuilder startingLevelAboveLevel =
        PlayerCharacter.builder().creationComplete(true).level(2).startingLevel(3);
    assertThrows(IllegalArgumentException.class, startingLevelAboveLevel::build);
    PlayerCharacterBuilder blankAmmunition = PlayerCharacter.builder().ammunition(Map.of(" ", 1));
    assertThrows(IllegalArgumentException.class, blankAmmunition::build);
    PlayerCharacterBuilder negativeShots = PlayerCharacter.builder().ammunition(Map.of("10mm", -1));
    assertThrows(IllegalArgumentException.class, negativeShots::build);
  }

  @Test
  void completeCharacterKeepsStartingLevelAndTags() {
    PlayerCharacter character = PlayerCharacter.builder().creationComplete(true).level(3).startingLevel(2)
        .startingTagSkills(Set.of(Skill.SCIENCE)).build();
    assertEquals(2, character.advancement().getStartingLevel().orElseThrow());
    assertEquals(Set.of(Skill.SCIENCE), character.getStartingTagSkills());
  }
}
