package de.fallout2d20.charbuilder.model.character.advancement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.Vitals;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.model.character.creation.PerkRules;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdvancementRulesTest {

  private static Character complete() {
    Character character = Character.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.completeCreation();
    return character;
  }

  @Test
  void activeOnlyWithPointsOrStagedChoicesAfterCreation() {
    assertFalse(AdvancementRules.isActive(Character.createNew()));
    Character character = complete();
    assertFalse(AdvancementRules.isActive(character));
    character.gainMilestoneLevel();
    assertTrue(AdvancementRules.isActive(character));
    character.stageSkillRank(Skill.SNEAK, 1);
    character.addPerkRank(CatalogRef.of("jury-rigging"), null, null);
    character.applyAdvancement();
    assertFalse(AdvancementRules.isActive(character));
  }

  /** Only perks pending: the perk grid is needed, the skill buttons are not. */
  @Test
  void choosesSkillsAndPerksSeparately() {
    Character character = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 3, 300, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(), new Progression(0, 1), Vitals.FULL,
        null, null);
    assertTrue(AdvancementRules.choosesPerks(character));
    assertFalse(AdvancementRules.choosesSkills(character));
    character.addPerkRank(CatalogRef.of("skilled"), null, null);
    assertTrue(AdvancementRules.choosesSkills(character), "a staged Skilled gives two skill ranks");
  }

  @Test
  void canLowerOnlyStagedRanks() {
    Character character = complete();
    character.gainMilestoneLevel();
    assertFalse(AdvancementRules.canLower(character, Skill.SNEAK));
    character.stageSkillRank(Skill.SNEAK, 1);
    assertTrue(AdvancementRules.canLower(character, Skill.SNEAK));
    assertFalse(AdvancementRules.canRaise(character, Skill.REPAIR), "the only rank is staged");
  }

  /** Leftover points are no problem; too many staged choices and unmet requirements are. */
  @Test
  void problemsBlockApply() {
    Character character = complete();
    character.addXp(300);
    character.stageSkillRank(Skill.SNEAK, 1);
    assertTrue(AdvancementRules.problems(character).isEmpty());
    assertTrue(AdvancementRules.canApply(character));

    character.addPerkRank(CatalogRef.of("intense-training"), Attribute.INTELLIGENCE, null);
    character.addPerkRank(CatalogRef.of("comprehension"), null, null);
    character.addPerkRank(CatalogRef.of("jury-rigging"), null, null);
    character.removePerkRank("intense-training");
    character.stageSkillRank(Skill.SNEAK, 2);
    assertEquals(List.of("Too many skill ranks staged (1 too many).",
        "Comprehension: Requirement not met: INT 6."),
        AdvancementRules.problems(character).stream().map(CreationProblem::message).toList());
    assertFalse(AdvancementRules.canApply(character));
  }

  /** After creation the perk grid works with the pending perks and gives back staged ranks only. */
  @Test
  void perkRulesUseThePendingPerksAfterCreation() {
    Character character = complete();
    character.addXp(300);
    assertEquals(2, PerkRules.budget(character));
    assertEquals(2, PerkRules.perksLeft(character));
    character.addPerkRank(CatalogRef.of("jury-rigging"), null, null);
    assertEquals(1, PerkRules.perksLeft(character));
    assertTrue(PerkRules.canRemove(character, "jury-rigging"));
    character.applyAdvancement();
    assertFalse(PerkRules.canRemove(character, "jury-rigging"));
    assertEquals(1, PerkRules.budget(character));
  }
}
