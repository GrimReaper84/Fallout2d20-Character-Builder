package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.model.character.advancement.AdvancementRules;
import de.fallout2d20.charbuilder.model.character.creation.PerkRules;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** XP, level-up and staged advancement after creation (CRB p. 49). */
class CharacterAdvancementTest {

  private static final CatalogRef SKILLED = CatalogRef.of("skilled");
  private static final CatalogRef TAG = CatalogRef.of("tag");

  /** A complete Survivor at level 1 with 0 XP. */
  static Character complete(String origin) {
    Character character = Character.createNew();
    character.chooseOrigin(CatalogRef.of(origin));
    character.completeCreation();
    return character;
  }

  /** CRB p. 49 table: 300 XP reach level 3, so a level 1 character gains two levels. */
  @Test
  void addXpCrossingThresholdsGainsLevels() {
    Character character = complete("survivor");
    assertEquals(2, character.levelsGainedBy(300));
    assertEquals(1, character.getLevel(), "the preview changes nothing");
    assertEquals(2, character.addXp(300));
    assertEquals(3, character.getLevel());
    assertEquals(300, character.getXp());
    assertEquals(new Progression(2, 2), character.getProgression());
  }

  @Test
  void addXpBelowTheNextThresholdGainsNoLevel() {
    Character character = complete("survivor");
    assertEquals(0, character.addXp(99));
    assertEquals(1, character.getLevel());
    assertEquals(99, character.getXp());
    assertEquals(Progression.NONE, character.getProgression());
    assertEquals(1, character.addXp(1), "XP are added, not set");
  }

  @Test
  void addXpRejectsNonPositiveAmountsAndCreation() {
    Character character = complete("survivor");
    assertThrows(IllegalArgumentException.class, () -> character.addXp(0));
    assertThrows(IllegalArgumentException.class, () -> character.addXp(-5));
    assertThrows(IllegalStateException.class, () -> Character.createNew().addXp(100));
    assertThrows(IllegalStateException.class, () -> Character.createNew().gainMilestoneLevel());
  }

  /** CRB p. 49 milestone progression: XP are raised to the threshold, higher XP stay. */
  @Test
  void milestoneLevelRaisesXpToTheThresholdAndKeepsHigherXp() {
    Character character = complete("survivor");
    character.gainMilestoneLevel();
    assertEquals(2, character.getLevel());
    assertEquals(100, character.getXp());
    assertEquals(new Progression(1, 1), character.getProgression());

    // An imported character may have more XP than its level needs (legacy milestone levelling).
    Character ahead = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 2, 700, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(), Progression.NONE, Vitals.FULL,
        null, null);
    ahead.gainMilestoneLevel();
    assertEquals(3, ahead.getLevel());
    assertEquals(700, ahead.getXp(), "already above the level 3 threshold of 300");
  }

  /** CRB p. 49: +1 max HP per level; current HP rise as well (user decision), "at maximum" stays. */
  @Test
  void levelUpRaisesMaxAndCurrentHp() {
    Character atMaximum = complete("survivor");
    int maxHp = DerivedStats.of(atMaximum).maxHp();
    atMaximum.addXp(300);
    assertEquals(maxHp + 2, DerivedStats.of(atMaximum).maxHp());
    assertNull(atMaximum.getVitals().currentHp());

    Character hurt = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 1, 0, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(), Progression.NONE,
        new Vitals(4, 3, 0), null, null);
    hurt.gainMilestoneLevel();
    assertEquals(new Vitals(5, 3, 0), hurt.getVitals());
  }

  /** CRB p. 49: a skill rank per level, up to 6; staged ranks count in the rank. */
  @Test
  void stagedSkillRanksCountAndStopAtTheCap() {
    Character character = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 2, 100, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(Skill.SNEAK, new SkillState(true, 3, 0)), List.of(),
        List.of(), new Progression(2, 0), Vitals.FULL, null, null);
    assertEquals(5, character.skillRank(Skill.SNEAK));
    assertTrue(AdvancementRules.canRaise(character, Skill.SNEAK));
    character.stageSkillRank(Skill.SNEAK, 1);
    assertEquals(6, character.skillRank(Skill.SNEAK));
    assertEquals(1, character.advancementSkillRanksLeft());
    assertFalse(AdvancementRules.canRaise(character, Skill.SNEAK), "rank 6 is the maximum");
    assertThrows(IllegalArgumentException.class, () -> character.stageSkillRank(Skill.LOCKPICK, -1));
  }

  /** CRB p. 53: a Super Mutant's skills stop at rank 4, also after creation. */
  @Test
  void superMutantSkillsStopAtFour() {
    Character character = complete("super-mutant");
    character.addXp(1_000);
    for (int i = 0; i < 4; i++) {
      assertTrue(AdvancementRules.canRaise(character, Skill.MELEE_WEAPONS), "rank " + i);
      character.stageSkillRank(Skill.MELEE_WEAPONS, 1);
    }
    assertEquals(4, character.skillRank(Skill.MELEE_WEAPONS));
    assertFalse(AdvancementRules.canRaise(character, Skill.MELEE_WEAPONS));
  }

  /** CRB p. 71: a staged Skilled rank adds two skill ranks to spend. */
  @Test
  void stagedSkilledAddsTwoSkillRanks() {
    Character character = complete("survivor");
    character.addXp(300);
    character.addPerkRank(SKILLED, null, null);
    assertEquals(4, character.advancementSkillRanksLeft());
    assertEquals(1, character.advancementPerksLeft());
    for (int i = 0; i < 4; i++) {
      character.stageSkillRank(Skill.values()[i], 1);
    }
    assertEquals(0, character.advancementSkillRanksLeft());
    character.removePerkRank(SKILLED.id());
    assertEquals(-2, character.advancementSkillRanksLeft());
    assertThrows(IllegalStateException.class, character::applyAdvancement);
  }

  /** CRB p. 73: Tag! adds 2 ranks, but not above 6. */
  @Test
  void stagedTagAddsTwoRanksCappedAtSix() {
    Character character = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 5, 1_000, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(Skill.SCIENCE, new SkillState(false, 3, 2),
            Skill.REPAIR, new SkillState(false, 1, 0)), List.of(), List.of(), new Progression(0, 2),
        Vitals.FULL, null, null);
    character.addPerkRank(TAG, null, Skill.REPAIR);
    assertTrue(character.isTagged(Skill.REPAIR));
    assertEquals(3, character.skillRank(Skill.REPAIR));
    character.removePerkRank(TAG.id());
    character.addPerkRank(TAG, null, Skill.SCIENCE);
    assertEquals(6, character.skillRank(Skill.SCIENCE), "rank 5 + 2 is capped at 6");
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
    Character character = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 5, 1_000, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(), new Progression(1, 1),
        Vitals.FULL, new LegacyBaseline(attributes, ranks), null);
    character.addPerkRank(TAG, null, Skill.SNEAK);
    assertEquals(3, character.skillRank(Skill.SNEAK));
    character.stageSkillRank(Skill.SNEAK, 1);
    assertEquals(4, character.skillRank(Skill.SNEAK));
  }

  /**
   * An imported Intense Training without recorded choices: a staged rank counts its choice, and taking
   * it back removes that choice too.
   */
  @Test
  void stagedRankOnAnImportedPerkWithoutChoices() {
    Character character = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 7, 2_100, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(),
        List.of(new PerkSelection(CatalogRef.of("intense-training"), 2, 2, List.of(), List.of())),
        new Progression(0, 1), Vitals.FULL, null, null);
    character.addPerkRank(CatalogRef.of("intense-training"), Attribute.STRENGTH, null);
    assertEquals(6, character.attributeValue(Attribute.STRENGTH), "only the new rank adds +1");
    assertTrue(AdvancementRules.problems(character).isEmpty(), "imported ranks may lack their choices");
    character.removePerkRank("intense-training");
    assertEquals(new PerkSelection(CatalogRef.of("intense-training"), 2, 2, List.of(), List.of()),
        character.getPerks().get(0));
    assertEquals(5, character.attributeValue(Attribute.STRENGTH));
  }

  /** Imported perk ranks cannot be given back after creation. */
  @Test
  void importedPerkRanksStayAfterCreation() {
    PerkSelection imported = new PerkSelection(CatalogRef.of("jury-rigging"), 1, 1, List.of(), List.of());
    Character character = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 1, 0, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(imported), Progression.NONE,
        Vitals.FULL, null, null);
    character.removePerkRank("jury-rigging");
    assertEquals(List.of(imported), character.getPerks());
  }

  /** CRB p. 66: a staged Intense Training can meet the requirement of another staged perk. */
  @Test
  void stagedIntenseTrainingMeetsAnotherStagedRequirement() {
    Character character = complete("survivor");
    character.addXp(300);
    character.addPerkRank(CatalogRef.of("intense-training"), Attribute.INTELLIGENCE, null);
    assertEquals(6, character.attributeValue(Attribute.INTELLIGENCE));
    character.addPerkRank(CatalogRef.of("comprehension"), null, null);
    assertTrue(AdvancementRules.canApply(character));
    character.removePerkRank("intense-training");
    assertFalse(AdvancementRules.canApply(character), "Comprehension needs INT 6");
  }

  /** Applied ranks and perks stay; points left stay pending. */
  @Test
  void applyMakesStagedChoicesPermanentAndKeepsLeftovers() {
    Character character = complete("survivor");
    character.addXp(300);
    character.stageSkillRank(Skill.SNEAK, 1);
    character.addPerkRank(CatalogRef.of("jury-rigging"), null, null);
    character.applyAdvancement();
    assertEquals(new SkillState(false, 0, 1, 0), character.skillState(Skill.SNEAK));
    assertEquals(0, character.getPerks().get(0).stagedRanks());
    assertEquals(new Progression(1, 1), character.getProgression());
    assertFalse(character.hasStagedAdvancement());
    character.removePerkRank("jury-rigging");
    assertEquals(1, character.getPerks().size(), "applied perks cannot be given back");
  }

  @Test
  void discardTakesBackAllStagedChoices() {
    Character character = complete("survivor");
    character.addXp(300);
    character.stageSkillRank(Skill.SNEAK, 1);
    character.addPerkRank(CatalogRef.of("intense-training"), Attribute.AGILITY, null);
    character.discardAdvancement();
    assertFalse(character.hasStagedAdvancement());
    assertTrue(character.getPerks().isEmpty());
    assertEquals(0, character.skillRank(Skill.SNEAK));
    assertEquals(5, character.attributeValue(Attribute.AGILITY));
    assertEquals(new Progression(2, 2), character.getProgression());
  }

  /** CRB p. 73: Tag! needs level 5; the requirement uses the new level. */
  @Test
  void perkLevelRequirementUsesTheNewLevel() {
    Character character = complete("survivor");
    assertFalse(PerkRules.blockers(character, PerkCatalog.byId("tag").orElseThrow()).isEmpty());
    character.addXp(1_000);
    assertTrue(PerkRules.blockers(character, PerkCatalog.byId("tag").orElseThrow()).isEmpty());
  }
}
