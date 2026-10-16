package de.fallout2d20.charbuilder.model.character.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.SkillState;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import de.fallout2d20.charbuilder.model.character.Vitals;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SkillRulesTest {

  private static final Map<Skill, SkillState> THREE_TAGS = Map.of(
      Skill.SMALL_GUNS, new SkillState(true, 0, 0),
      Skill.SNEAK, new SkillState(true, 0, 0),
      Skill.LOCKPICK, new SkillState(true, 0, 0));

  /** CRB p. 58: three Tag skills, all of them must be chosen (user decision). */
  @Test
  void threeFreeTagsRequired() {
    Character fresh = character("survivor", Map.of(), List.of());
    assertEquals(3, SkillRules.freeTagsLeft(fresh));
    assertEquals(List.of("Choose 3 more Tag skills.", "Spend all skill points (14 left)."), messages(fresh));
    assertEquals(0, SkillRules.freeTagsLeft(character("survivor", THREE_TAGS, List.of())));
    Character four = character("survivor", tags(Skill.SMALL_GUNS, Skill.SNEAK, Skill.LOCKPICK, Skill.REPAIR),
        List.of());
    assertEquals(List.of("Too many Tag skills chosen (1 too many).", "Spend all skill points (14 left)."),
        messages(four));
  }

  /** CRB p. 56: Educated gives one more Tag skill. */
  @Test
  void educatedAddsAFreeTag() {
    Character educated = character("survivor", THREE_TAGS, List.of("educated"));
    assertEquals(4, SkillRules.freeTagBudget(educated));
    assertEquals("Choose 1 more Tag skill.", messages(educated).getFirst());
  }

  /** CRB p. 52 (user reading): Ghoul Survival comes in addition to the three free Tags. */
  @Test
  void ghoulSurvivalIsNotAFreeTag() {
    Character ghoul = character("ghoul", THREE_TAGS, List.of());
    assertEquals(0, SkillRules.freeTagsLeft(ghoul));
    assertTrue(ghoul.isTagged(Skill.SURVIVAL));
    assertFalse(SkillRules.canToggleTag(ghoul, Skill.SURVIVAL));
  }

  /** CRB p. 51: the Brotherhood's extra Tag is not one of the free ones. */
  @Test
  void brotherhoodExtraTagIsNotAFreeTag() {
    Character initiate = character("brotherhood-initiate", THREE_TAGS, List.of());
    initiate.chooseOriginTagSkill(Skill.SCIENCE);
    assertEquals(0, SkillRules.freeTagsLeft(initiate));
    assertFalse(SkillRules.canToggleTag(initiate, Skill.SCIENCE));
  }

  /** CRB p. 58: 9 + Intelligence skill points. */
  @Test
  void skillPointsAreNinePlusIntelligence() {
    assertEquals(14, SkillRules.skillPointBudget(character("survivor", Map.of(), List.of())));
    Character smart = character("survivor", Map.of(), List.of());
    smart.setAttributeAllocation(Attribute.INTELLIGENCE, 3);
    assertEquals(17, SkillRules.skillPointBudget(smart));
  }

  /** CRB p. 71: Skilled adds 2 skill points per rank (level 3: 9 + INT 5 + 2 levels + 2). */
  @Test
  void skilledAddsTwoPointsPerRank() {
    Character skilled = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 3, 0, false,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(),
        List.of(new PerkSelection(CatalogRef.of("skilled"), 1, 0, List.of(), List.of())),
        Progression.NONE, Vitals.FULL, null, null);
    assertEquals(18, SkillRules.skillPointBudget(skilled));
  }

  /** After lowering the level, the lower creation cap blocks buying further ranks (CRB p. 58). */
  @Test
  void loweringTheLevelBlocksRanksAboveTheNewCap() {
    Character character = character("survivor", Map.of(), List.of());
    character.setStartingLevel(3);
    character.setCreationRanks(Skill.SNEAK, 5);
    character.setStartingLevel(1);
    assertEquals(3, character.skillRank(Skill.SNEAK));
    assertFalse(SkillRules.canRaise(character, Skill.SNEAK));
  }

  /** CRB p. 49: one skill rank per level above 1 (level 3, INT 5 → 16). */
  @Test
  void skillPointsGrowWithLevel() {
    Character character = character("survivor", Map.of(), List.of());
    character.setStartingLevel(3);
    assertEquals(16, SkillRules.skillPointBudget(character));
  }

  /** User decision: Gifted on INT raises the skill points. */
  @Test
  void giftedIntelligenceRaisesSkillPoints() {
    Character gifted = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 1, 0, false,
        EnumSet.allOf(Book.class), Map.of(), Map.of(),
        List.of(new TraitSelection(CatalogRef.of("gifted"), false,
            List.of(Attribute.INTELLIGENCE, Attribute.LUCK))),
        List.of(), Progression.NONE, Vitals.FULL, null, null);
    assertEquals(15, SkillRules.skillPointBudget(gifted));
  }

  /** Tag ranks are free; bought ranks cost one point each; all points must be spent (user decision). */
  @Test
  void unspentAndOverspentSkillPoints() {
    Character character = character("survivor", Map.of(
        Skill.SMALL_GUNS, new SkillState(true, 1, 0),
        Skill.SNEAK, new SkillState(true, 1, 0),
        Skill.LOCKPICK, new SkillState(true, 1, 0),
        Skill.ATHLETICS, new SkillState(false, 3, 0)), List.of());
    assertEquals(8, SkillRules.skillPointsLeft(character));
    assertEquals(List.of("Spend all skill points (8 left)."), messages(character));
    character.setCreationRanks(Skill.REPAIR, 3);
    character.setCreationRanks(Skill.SCIENCE, 3);
    character.setCreationRanks(Skill.MEDICINE, 3);
    assertEquals(List.of("Too many skill points spent (1 too many)."), messages(character));
  }

  /** CRB p. 58: no skill above rank 3 during creation at level 1. */
  @Test
  void rankCapThreeAtLevelOne() {
    Character character = character("survivor", Map.of(Skill.SNEAK, new SkillState(true, 1, 0)), List.of());
    assertFalse(SkillRules.canRaise(character, Skill.SNEAK));
    assertTrue(SkillRules.canRaise(character, Skill.REPAIR));
    character.setCreationRanks(Skill.SNEAK, 2);
    assertTrue(messages(character).contains("Sneak may not exceed rank 3 during creation."));
  }

  /** CRB p. 53: Super Mutant skills at most rank 4, even from level 3 on. */
  @Test
  void superMutantCapFour() {
    Character mutant = new Character("Test", CatalogRef.of("super-mutant"), OriginChoices.NONE, 3, 0,
        false, EnumSet.allOf(Book.class), Map.of(),
        Map.of(Skill.MELEE_WEAPONS, new SkillState(true, 3, 0)), List.of(), List.of(), Progression.NONE,
        Vitals.FULL, null, null);
    assertTrue(messages(mutant).contains("Melee Weapons may not exceed rank 4 (Super Mutant)."));
    Character atCap = new Character("Test", CatalogRef.of("super-mutant"), OriginChoices.NONE, 3, 0,
        false, EnumSet.allOf(Book.class), Map.of(),
        Map.of(Skill.MELEE_WEAPONS, new SkillState(true, 2, 0)), List.of(), List.of(), Progression.NONE,
        Vitals.FULL, null, null);
    assertFalse(SkillRules.canRaise(atCap, Skill.MELEE_WEAPONS));
  }

  /** At level 1 the creation cap 3 is lower than the Super Mutant cap 4 (CRB p. 58, 53). */
  @Test
  void superMutantAtLevelOneUsesTheCreationCap() {
    Character mutant = character("super-mutant", Map.of(Skill.MELEE_WEAPONS, new SkillState(true, 2, 0)),
        List.of());
    assertTrue(messages(mutant).contains("Melee Weapons may not exceed rank 3 during creation."));
  }

  /** WG p. 15: Nomad forbids Science as a Tag skill; a full budget blocks new Tags. */
  @Test
  void canToggleTag() {
    Character tribal = character("tribal", Map.of(), List.of("nomad"));
    assertFalse(SkillRules.canToggleTag(tribal, Skill.SCIENCE));
    assertEquals("Nomad", tribal.tagForbiddenBy(Skill.SCIENCE).orElseThrow().name());
    assertTrue(SkillRules.canToggleTag(tribal, Skill.SURVIVAL));
    Character full = character("survivor", THREE_TAGS, List.of());
    assertFalse(SkillRules.canToggleTag(full, Skill.REPAIR));
    assertTrue(SkillRules.canToggleTag(full, Skill.SNEAK));
  }

  /** A free Tag can always be removed, even where a trait or the origin no longer allows it. */
  @Test
  void aFreeTagCanAlwaysBeRemoved() {
    Character loaded = character("tribal", Map.of(Skill.SCIENCE, new SkillState(true, 0, 0)), List.of("nomad"));
    assertTrue(SkillRules.canToggleTag(loaded, Skill.SCIENCE));
    loaded.setFreeTag(Skill.SCIENCE, false);
    assertFalse(loaded.isTagged(Skill.SCIENCE));
    Character ghoul = character("ghoul", Map.of(Skill.SURVIVAL, new SkillState(true, 0, 0)), List.of());
    assertTrue(SkillRules.canToggleTag(ghoul, Skill.SURVIVAL));
    ghoul.setFreeTag(Skill.SURVIVAL, false);
    assertEquals(3, SkillRules.freeTagsLeft(ghoul));
    assertTrue(ghoul.isTagged(Skill.SURVIVAL));
  }

  @Test
  void completeCharactersHaveNoProblems() {
    Character complete = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 1, 0, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(), Progression.NONE,
        Vitals.FULL, null, null);
    assertEquals(List.of(), messages(complete));
  }

  private static Map<Skill, SkillState> tags(Skill... skills) {
    Map<Skill, SkillState> map = new EnumMap<>(Skill.class);
    for (Skill skill : skills) {
      map.put(skill, new SkillState(true, 0, 0));
    }
    return map;
  }

  private static Character character(String origin, Map<Skill, SkillState> skills, List<String> traits) {
    return new Character("Test", CatalogRef.of(origin), OriginChoices.NONE, 1, 0, false,
        EnumSet.allOf(Book.class), Map.of(), skills,
        traits.stream().map(id -> new TraitSelection(CatalogRef.of(id), false, List.of())).toList(),
        List.of(), Progression.NONE, Vitals.FULL, null, null);
  }

  private static List<String> messages(Character character) {
    return SkillRules.problems(character).stream().map(CreationProblem::message).toList();
  }
}
