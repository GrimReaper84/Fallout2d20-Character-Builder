package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.model.character.advancement.AdvancementRules;
import de.fallout2d20.charbuilder.model.character.creation.PerkRules;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** XP, level-up and staged advancement after creation (CRB p. 49). */
class PlayerCharacterAdvancementTest {

  private static final CatalogRef SKILLED = CatalogRef.of("skilled");
  private static final CatalogRef TAG = CatalogRef.of("tag");

  /** A complete Survivor at level 1 with 0 XP. */
  static PlayerCharacter complete(String origin) {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of(origin));
    character.completeCreation();
    return character;
  }

  /** CRB p. 49 table: 300 XP reach level 3, so a level 1 character gains two levels. */
  @Test
  void addXpCrossingThresholdsGainsLevels() {
    PlayerCharacter character = complete("survivor");
    assertEquals(2, character.advancement().levelsGainedBy(300));
    assertEquals(1, character.advancement().getLevel(), "the preview changes nothing");
    assertEquals(2, character.advancement().addXp(300));
    assertEquals(3, character.advancement().getLevel());
    assertEquals(300, character.advancement().getXp());
    assertEquals(new Progression(2, 2), character.advancement().getProgression());
  }

  @Test
  void addXpBelowTheNextThresholdGainsNoLevel() {
    PlayerCharacter character = complete("survivor");
    assertEquals(0, character.advancement().addXp(99));
    assertEquals(1, character.advancement().getLevel());
    assertEquals(99, character.advancement().getXp());
    assertEquals(Progression.NONE, character.advancement().getProgression());
    assertEquals(1, character.advancement().addXp(1), "XP are added, not set");
  }

  @Test
  void addXpRejectsNonPositiveAmountsAndCreation() {
    PlayerCharacter character = complete("survivor");
    assertThrows(IllegalArgumentException.class, () -> character.advancement().addXp(0));
    assertThrows(IllegalArgumentException.class, () -> character.advancement().addXp(-5));
    assertThrows(IllegalStateException.class, () -> PlayerCharacter.createNew().advancement().addXp(100));
    assertThrows(IllegalStateException.class, () -> PlayerCharacter.createNew().advancement().gainMilestoneLevel());
  }

  /** CRB p. 49 milestone progression: XP are raised to the threshold, higher XP stay. */
  @Test
  void milestoneLevelRaisesXpToTheThresholdAndKeepsHigherXp() {
    PlayerCharacter character = complete("survivor");
    character.advancement().gainMilestoneLevel();
    assertEquals(2, character.advancement().getLevel());
    assertEquals(100, character.advancement().getXp());
    assertEquals(new Progression(1, 1), character.advancement().getProgression());

    // An imported character may have more XP than its level needs (legacy milestone levelling).
    PlayerCharacter ahead = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor")).level(2).xp(700)
        .creationComplete(true).build();
    ahead.advancement().gainMilestoneLevel();
    assertEquals(3, ahead.advancement().getLevel());
    assertEquals(700, ahead.advancement().getXp(), "already above the level 3 threshold of 300");
  }

  /** CRB p. 49: +1 max HP per level; current HP rise as well (user decision), "at maximum" stays. */
  @Test
  void levelUpRaisesMaxAndCurrentHp() {
    PlayerCharacter atMaximum = complete("survivor");
    int maxHp = DerivedStats.of(atMaximum).maxHp();
    atMaximum.advancement().addXp(300);
    assertEquals(maxHp + 2, DerivedStats.of(atMaximum).maxHp());
    assertNull(atMaximum.session().getVitals().currentHp());

    PlayerCharacter hurt = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor"))
        .creationComplete(true).vitals(new Vitals(4, 3, 0)).build();
    hurt.advancement().gainMilestoneLevel();
    assertEquals(new Vitals(5, 3, 0), hurt.session().getVitals());
  }

  /** CRB p. 49: a skill rank per level, up to 6; staged ranks count in the rank. */
  @Test
  void stagedSkillRanksCountAndStopAtTheCap() {
    PlayerCharacter character = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor")).level(2)
        .xp(100).creationComplete(true).skills(Map.of(Skill.SNEAK, new SkillState(true, 3, 0)))
        .progression(new Progression(2, 0)).build();
    assertEquals(5, character.abilities().skillRank(Skill.SNEAK));
    assertTrue(AdvancementRules.canRaise(character, Skill.SNEAK));
    character.advancement().stageSkillRank(Skill.SNEAK, 1);
    assertEquals(6, character.abilities().skillRank(Skill.SNEAK));
    assertEquals(1, character.advancement().skillRanksLeft());
    assertFalse(AdvancementRules.canRaise(character, Skill.SNEAK), "rank 6 is the maximum");
    assertThrows(IllegalArgumentException.class, () -> character.advancement().stageSkillRank(Skill.LOCKPICK, -1));
  }

  /** CRB p. 53: a Super Mutant's skills stop at rank 4, also after creation. */
  @Test
  void superMutantSkillsStopAtFour() {
    PlayerCharacter character = complete("super-mutant");
    character.advancement().addXp(1_000);
    for (int i = 0; i < 4; i++) {
      assertTrue(AdvancementRules.canRaise(character, Skill.MELEE_WEAPONS), "rank " + i);
      character.advancement().stageSkillRank(Skill.MELEE_WEAPONS, 1);
    }
    assertEquals(4, character.abilities().skillRank(Skill.MELEE_WEAPONS));
    assertFalse(AdvancementRules.canRaise(character, Skill.MELEE_WEAPONS));
  }

  /** CRB p. 71: a staged Skilled rank adds two skill ranks to spend. */
  @Test
  void stagedSkilledAddsTwoSkillRanks() {
    PlayerCharacter character = complete("survivor");
    character.advancement().addXp(300);
    character.addPerkRank(SKILLED, null, null);
    assertEquals(4, character.advancement().skillRanksLeft());
    assertEquals(1, character.advancement().perksLeft());
    for (Skill skill : List.of(Skill.values()).subList(0, 4)) {
      character.advancement().stageSkillRank(skill, 1);
    }
    assertEquals(0, character.advancement().skillRanksLeft());
    character.removePerkRank(SKILLED.id());
    assertEquals(-2, character.advancement().skillRanksLeft());
    assertThrows(IllegalStateException.class, character.advancement()::apply);
  }

  /** CRB p. 73: Tag! adds 2 ranks, but not above 6. */
  @Test
  void stagedTagAddsTwoRanksCappedAtSix() {
    PlayerCharacter character = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor")).level(5)
        .xp(1_000).creationComplete(true)
        .skills(Map.of(Skill.SCIENCE, new SkillState(false, 3, 2), Skill.REPAIR, new SkillState(false, 1, 0)))
        .progression(new Progression(0, 2)).build();
    character.addPerkRank(TAG, null, Skill.REPAIR);
    assertTrue(character.abilities().isTagged(Skill.REPAIR));
    assertEquals(3, character.abilities().skillRank(Skill.REPAIR));
    character.removePerkRank(TAG.id());
    character.addPerkRank(TAG, null, Skill.SCIENCE);
    assertEquals(6, character.abilities().skillRank(Skill.SCIENCE), "rank 5 + 2 is capped at 6");
  }

  /** An imported character's baseline already holds its old Tags; a new Tag! adds its 2 ranks on top. */
  @Test
  void legacyCharacterGetsTheTagBonusOfANewTagPerk() {
    Map<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
    for (Attribute attribute : Attribute.values()) {
      attributes.put(attribute, 5);
    }
    Map<Skill, Integer> ranks = new EnumMap<>(Skill.class);
    for (Skill skill : Skill.values()) {
      ranks.put(skill, skill == Skill.SNEAK ? 1 : 0);
    }
    PlayerCharacter character = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor")).level(5)
        .xp(1_000).creationComplete(true).progression(new Progression(1, 1))
        .legacyBaseline(new LegacyBaseline(attributes, ranks)).build();
    character.addPerkRank(TAG, null, Skill.SNEAK);
    assertEquals(3, character.abilities().skillRank(Skill.SNEAK));
    character.advancement().stageSkillRank(Skill.SNEAK, 1);
    assertEquals(4, character.abilities().skillRank(Skill.SNEAK));
  }

  /**
   * An imported Intense Training without recorded choices: a staged rank counts its choice, and taking
   * it back removes that choice too.
   */
  @Test
  void stagedRankOnAnImportedPerkWithoutChoices() {
    PlayerCharacter character = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor")).level(7)
        .xp(2_100).creationComplete(true)
        .perks(List.of(new PerkSelection(CatalogRef.of("intense-training"), 2, 2, List.of(), List.of())))
        .progression(new Progression(0, 1)).build();
    character.addPerkRank(CatalogRef.of("intense-training"), Attribute.STRENGTH, null);
    assertEquals(6, character.abilities().attributeValue(Attribute.STRENGTH), "only the new rank adds +1");
    assertTrue(AdvancementRules.problems(character).isEmpty(), "imported ranks may lack their choices");
    character.removePerkRank("intense-training");
    assertEquals(new PerkSelection(CatalogRef.of("intense-training"), 2, 2, List.of(), List.of()),
        character.getPerks().getFirst());
    assertEquals(5, character.abilities().attributeValue(Attribute.STRENGTH));
  }

  /** Imported perk ranks cannot be given back after creation. */
  @Test
  void importedPerkRanksStayAfterCreation() {
    PerkSelection imported = new PerkSelection(CatalogRef.of("jury-rigging"), 1, 1, List.of(), List.of());
    PlayerCharacter character = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor"))
        .creationComplete(true).perks(List.of(imported)).build();
    character.removePerkRank("jury-rigging");
    assertEquals(List.of(imported), character.getPerks());
  }

  /** CRB p. 66: a staged Intense Training can meet the requirement of another staged perk. */
  @Test
  void stagedIntenseTrainingMeetsAnotherStagedRequirement() {
    PlayerCharacter character = complete("survivor");
    character.advancement().addXp(300);
    character.addPerkRank(CatalogRef.of("intense-training"), Attribute.INTELLIGENCE, null);
    assertEquals(6, character.abilities().attributeValue(Attribute.INTELLIGENCE));
    character.addPerkRank(CatalogRef.of("comprehension"), null, null);
    assertTrue(AdvancementRules.canApply(character));
    character.removePerkRank("intense-training");
    assertFalse(AdvancementRules.canApply(character), "Comprehension needs INT 6");
  }

  /** Applied ranks and perks stay; points left stay pending. */
  @Test
  void applyMakesStagedChoicesPermanentAndKeepsLeftovers() {
    PlayerCharacter character = complete("survivor");
    character.advancement().addXp(300);
    character.advancement().stageSkillRank(Skill.SNEAK, 1);
    character.addPerkRank(CatalogRef.of("jury-rigging"), null, null);
    character.advancement().apply();
    assertEquals(new SkillState(false, 0, 1, 0), character.abilities().skillState(Skill.SNEAK));
    assertEquals(0, character.getPerks().getFirst().stagedRanks());
    assertEquals(new Progression(1, 1), character.advancement().getProgression());
    assertFalse(character.advancement().hasStaged());
    character.removePerkRank("jury-rigging");
    assertEquals(1, character.getPerks().size(), "applied perks cannot be given back");
  }

  @Test
  void discardTakesBackAllStagedChoices() {
    PlayerCharacter character = complete("survivor");
    character.advancement().addXp(300);
    character.advancement().stageSkillRank(Skill.SNEAK, 1);
    character.addPerkRank(CatalogRef.of("intense-training"), Attribute.AGILITY, null);
    character.advancement().discard();
    assertFalse(character.advancement().hasStaged());
    assertTrue(character.getPerks().isEmpty());
    assertEquals(0, character.abilities().skillRank(Skill.SNEAK));
    assertEquals(5, character.abilities().attributeValue(Attribute.AGILITY));
    assertEquals(new Progression(2, 2), character.advancement().getProgression());
  }

  /** CRB p. 73: Tag! needs level 5; the requirement uses the new level. */
  @Test
  void perkLevelRequirementUsesTheNewLevel() {
    PlayerCharacter character = complete("survivor");
    assertFalse(PerkRules.blockers(character, PerkCatalog.byId("tag").orElseThrow()).isEmpty());
    character.advancement().addXp(1_000);
    assertTrue(PerkRules.blockers(character, PerkCatalog.byId("tag").orElseThrow()).isEmpty());
  }
}
