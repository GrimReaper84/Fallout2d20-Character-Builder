package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.model.character.creation.AttributeRules;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.model.character.creation.CreationRules;
import de.fallout2d20.charbuilder.model.character.creation.PerkRules;
import de.fallout2d20.charbuilder.model.character.creation.SkillRules;
import de.fallout2d20.charbuilder.model.character.session.SessionValues;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/** GM free edit (D2, user decisions 2026-10-16). */
class GmModeTest {

  @Test
  void creationEditsOfACompleteCharacterNeedGmMode() {
    Character character = complete();
    assertThrows(IllegalStateException.class, () -> character.setAttributeAllocation(Attribute.LUCK, 2));
    character.setGmMode(true);
    character.chooseOrigin(CatalogRef.of("ghoul"));
    character.setAttributeAllocation(Attribute.LUCK, 3);
    character.setFreeTag(Skill.SCIENCE, true);
    assertEquals(8, character.attributeValue(Attribute.LUCK));
    assertTrue(character.isTagged(Skill.SCIENCE));
    character.setGmMode(false);
    assertThrows(IllegalStateException.class, () -> character.setFreeTag(Skill.SCIENCE, false));
  }

  /** No budgets in GM mode; the attribute maximum and the skill rank cap stay. */
  @Test
  void budgetsDoNotApplyButCapsDo() {
    Character character = complete();
    character.setGmMode(true);
    character.setAttributeAllocation(Attribute.STRENGTH, 4);
    character.setAttributeAllocation(Attribute.AGILITY, 1);
    assertEquals(0, AttributeRules.pointsLeft(character));
    assertTrue(AttributeRules.canRaise(character, Attribute.STRENGTH), "no budget in GM mode");
    character.setAttributeAllocation(Attribute.STRENGTH, 5);
    assertEquals(10, character.attributeValue(Attribute.STRENGTH));
    assertFalse(AttributeRules.canRaise(character, Attribute.STRENGTH), "maximum 10 (CRB p. 50)");
    character.setAttributeAllocation(Attribute.AGILITY, -4);
    assertFalse(AttributeRules.canLower(character, Attribute.AGILITY), "technical floor 1");

    for (int i = 0; i < 6; i++) {
      SkillRules.step(character, Skill.REPAIR, 1);
    }
    assertEquals(6, character.skillRank(Skill.REPAIR));
    assertFalse(SkillRules.canRaise(character, Skill.REPAIR), "rank cap 6 (CRB p. 49)");
    assertTrue(SkillRules.canToggleTag(character, Skill.SPEECH));
  }

  /** Perks: added as fixed ranks, any rank removable (imported too); requirements only hints. */
  @Test
  void perksInGmMode() {
    Character character = complete();
    character.setGmMode(true);
    assertTrue(PerkRules.canAdd(character, PerkCatalog.byId("adamantium-skeleton").orElseThrow()),
        "requirements are hints in GM mode");
    character.addPerkRank(CatalogRef.of("adamantium-skeleton"), null, null);
    PerkSelection perk = character.getPerks().getLast();
    assertEquals(0, perk.stagedRanks());
    assertTrue(PerkRules.canRemove(character, "adamantium-skeleton"));
    character.removePerkRank("adamantium-skeleton");
    assertTrue(character.getPerks().stream().noneMatch(p -> p.perk().id().equals("adamantium-skeleton")));
  }

  /** B8 follow-up: lowering the level lowers a starting level above it. */
  @Test
  void levelAndXpGoDown() {
    Character character = Character.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.setStartingLevel(4);
    character.completeCreation();
    assertThrows(IllegalStateException.class, () -> character.setLevelAsGm(2, 300));
    character.setGmMode(true);
    character.setLevelAsGm(2, 300);
    assertEquals(2, character.getLevel());
    assertEquals(300, character.getXp());
    assertEquals(OptionalInt.of(2), character.getStartingLevel());
    assertThrows(IllegalArgumentException.class, () -> character.setLevelAsGm(0, 0));
  }

  /** A Tag! bonus hides GM corrections below it; the step is then not offered and changes nothing. */
  @Test
  void stepsThatChangeNothingAreRefused() {
    Character character = complete();
    character.setGmMode(true);
    character.addPerkRank(CatalogRef.of("tag"), null, Skill.SURVIVAL);
    assertEquals(2, character.skillRank(Skill.SURVIVAL));
    assertFalse(SkillRules.canLower(character, Skill.SURVIVAL));
    SkillRules.step(character, Skill.SURVIVAL, -1);
    assertEquals(0, character.skillState(Skill.SURVIVAL).gmRanks());
    SkillRules.step(character, Skill.SURVIVAL, 1);
    assertEquals(3, character.skillRank(Skill.SURVIVAL));
  }

  /** Ending creation and the starting level belong to creation itself, even in GM mode. */
  @Test
  void creationItselfIsNotRedone() {
    Character character = complete();
    character.setGmMode(true);
    assertThrows(IllegalStateException.class, character::completeCreation);
    assertThrows(IllegalStateException.class, () -> character.setStartingLevel(3));
  }

  /** User decision 2026-10-16: lowering the level cuts pending points and caps the current HP. */
  @Test
  void loweringTheLevelCutsPendingPointsAndHp() {
    Character character = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 5, 1_000, true,
        EnumSet.allOf(Book.class), Map.of(),
        Map.of(), List.of(), List.of(), new Progression(3, 1), new Vitals(14, 0, 0),
        null, null);
    character.setGmMode(true);
    character.setLevelAsGm(3, 300);
    assertEquals(new Progression(1, 0), character.getProgression());
    assertEquals(12, SessionValues.of(character).currentHp(), "END 5 + LCK 5 + 2 levels (CRB p. 49)");
    assertNull(character.getVitals().currentHp(), "capped at the maximum = stored as full");
    character.setLevelAsGm(6, 1_500);
    assertEquals(new Progression(1, 0), character.getProgression(), "raising grants no points");
  }

  /** In GM mode a complete character gets content hints (requirements), but no budget hints. */
  @Test
  void contentHintsWithoutBudgets() {
    Character character = complete();
    character.setGmMode(true);
    character.addPerkRank(CatalogRef.of("adamantium-skeleton"), null, null);
    List<String> hints = CreationRules.problems(character).stream().map(CreationProblem::message).toList();
    assertTrue(hints.stream().anyMatch(hint -> hint.startsWith("Adamantium Skeleton:")), hints.toString());
    assertTrue(hints.stream().noneMatch(hint -> hint.contains("points") || hint.contains("more perk")), hints.toString());
    character.setGmMode(false);
    assertEquals(List.of(), CreationRules.problems(character));
  }

  private static Character complete() {
    Character character = Character.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.completeCreation();
    return character;
  }
}
