package de.fallout2d20.charbuilder.model.character.advancement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.model.character.creation.HintArea;
import de.fallout2d20.charbuilder.model.character.creation.PerkRules;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdvancementRulesTest {

  private static PlayerCharacter complete() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.completeCreation();
    return character;
  }

  @Test
  void activeOnlyWithPointsOrStagedChoicesAfterCreation() {
    assertFalse(AdvancementRules.isActive(PlayerCharacter.createNew()));
    PlayerCharacter character = complete();
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
    PlayerCharacter character = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor")).level(3)
        .xp(300).creationComplete(true).progression(new Progression(0, 1)).build();
    assertTrue(AdvancementRules.choosesPerks(character));
    assertFalse(AdvancementRules.choosesSkills(character));
    character.addPerkRank(CatalogRef.of("skilled"), null, null);
    assertTrue(AdvancementRules.choosesSkills(character), "a staged Skilled gives two skill ranks");
  }

  @Test
  void canLowerOnlyStagedRanks() {
    PlayerCharacter character = complete();
    character.gainMilestoneLevel();
    assertFalse(AdvancementRules.canLower(character, Skill.SNEAK));
    character.stageSkillRank(Skill.SNEAK, 1);
    assertTrue(AdvancementRules.canLower(character, Skill.SNEAK));
    assertFalse(AdvancementRules.canRaise(character, Skill.REPAIR), "the only rank is staged");
  }

  /** Leftover points are no problem; too many staged choices and unmet requirements are. */
  @Test
  void problemsBlockApply() {
    PlayerCharacter character = complete();
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
    assertEquals(List.of(HintArea.STATS, HintArea.PERKS),
        AdvancementRules.problems(character).stream().map(CreationProblem::area).toList());
    assertFalse(AdvancementRules.canApply(character));
  }

  /** After creation the perk grid works with the pending perks and gives back staged ranks only. */
  @Test
  void perkRulesUseThePendingPerksAfterCreation() {
    PlayerCharacter character = complete();
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
