package de.fallout2d20.charbuilder.model.character.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.SkillState;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import java.util.EnumMap;
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
    PlayerCharacter fresh = character("survivor", Map.of(), List.of());
    assertEquals(3, SkillRules.freeTagsLeft(fresh));
    assertEquals(List.of("Choose 3 more Tag skills.", "Spend all skill points (14 left)."), messages(fresh));
    assertEquals(0, SkillRules.freeTagsLeft(character("survivor", THREE_TAGS, List.of())));
    PlayerCharacter four = character("survivor", tags(Skill.SMALL_GUNS, Skill.SNEAK, Skill.LOCKPICK, Skill.REPAIR),
        List.of());
    assertEquals(List.of("Too many Tag skills chosen (1 too many).", "Spend all skill points (14 left)."),
        messages(four));
  }

  /** CRB p. 56: Educated gives one more Tag skill. */
  @Test
  void educatedAddsAFreeTag() {
    PlayerCharacter educated = character("survivor", THREE_TAGS, List.of("educated"));
    assertEquals(4, SkillRules.freeTagBudget(educated));
    assertEquals("Choose 1 more Tag skill.", messages(educated).getFirst());
  }

  /** CRB p. 52 (user reading): Ghoul Survival comes in addition to the three free Tags. */
  @Test
  void ghoulSurvivalIsNotAFreeTag() {
    PlayerCharacter ghoul = character("ghoul", THREE_TAGS, List.of());
    assertEquals(0, SkillRules.freeTagsLeft(ghoul));
    assertTrue(ghoul.abilities().isTagged(Skill.SURVIVAL));
    assertFalse(SkillRules.canToggleTag(ghoul, Skill.SURVIVAL));
  }

  /** CRB p. 51: the Brotherhood's extra Tag is not one of the free ones. */
  @Test
  void brotherhoodExtraTagIsNotAFreeTag() {
    PlayerCharacter initiate = character("brotherhood-initiate", THREE_TAGS, List.of());
    initiate.chooseOriginTagSkill(Skill.SCIENCE);
    assertEquals(0, SkillRules.freeTagsLeft(initiate));
    assertFalse(SkillRules.canToggleTag(initiate, Skill.SCIENCE));
  }

  /** CRB p. 58: 9 + Intelligence skill points. */
  @Test
  void skillPointsAreNinePlusIntelligence() {
    assertEquals(14, SkillRules.skillPointBudget(character("survivor", Map.of(), List.of())));
    PlayerCharacter smart = character("survivor", Map.of(), List.of());
    smart.abilities().setAttributeAllocation(Attribute.INTELLIGENCE, 3);
    assertEquals(17, SkillRules.skillPointBudget(smart));
  }

  /** CRB p. 71: Skilled adds 2 skill points per rank (level 3: 9 + INT 5 + 2 levels + 2). */
  @Test
  void skilledAddsTwoPointsPerRank() {
    PlayerCharacter skilled = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor")).level(3)
        .perks(List.of(new PerkSelection(CatalogRef.of("skilled"), 1, 0, List.of(), List.of()))).build();
    assertEquals(18, SkillRules.skillPointBudget(skilled));
  }

  /** After lowering the level, the lower creation cap blocks buying further ranks (CRB p. 58). */
  @Test
  void loweringTheLevelBlocksRanksAboveTheNewCap() {
    PlayerCharacter character = character("survivor", Map.of(), List.of());
    character.advancement().setStartingLevel(3);
    character.abilities().setCreationRanks(Skill.SNEAK, 5);
    character.advancement().setStartingLevel(1);
    assertEquals(3, character.abilities().skillRank(Skill.SNEAK));
    assertFalse(SkillRules.canRaise(character, Skill.SNEAK));
  }

  /** CRB p. 49: one skill rank per level above 1 (level 3, INT 5 → 16). */
  @Test
  void skillPointsGrowWithLevel() {
    PlayerCharacter character = character("survivor", Map.of(), List.of());
    character.advancement().setStartingLevel(3);
    assertEquals(16, SkillRules.skillPointBudget(character));
  }

  /** User decision: Gifted on INT raises the skill points. */
  @Test
  void giftedIntelligenceRaisesSkillPoints() {
    PlayerCharacter gifted = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor"))
        .traits(List.of(new TraitSelection(CatalogRef.of("gifted"), false,
            List.of(Attribute.INTELLIGENCE, Attribute.LUCK))))
        .build();
    assertEquals(15, SkillRules.skillPointBudget(gifted));
  }

  /** Tag ranks are free; bought ranks cost one point each; all points must be spent (user decision). */
  @Test
  void unspentAndOverspentSkillPoints() {
    PlayerCharacter character = character("survivor", Map.of(
        Skill.SMALL_GUNS, new SkillState(true, 1, 0),
        Skill.SNEAK, new SkillState(true, 1, 0),
        Skill.LOCKPICK, new SkillState(true, 1, 0),
        Skill.ATHLETICS, new SkillState(false, 3, 0)), List.of());
    assertEquals(8, SkillRules.skillPointsLeft(character));
    assertEquals(List.of("Spend all skill points (8 left)."), messages(character));
    character.abilities().setCreationRanks(Skill.REPAIR, 3);
    character.abilities().setCreationRanks(Skill.SCIENCE, 3);
    character.abilities().setCreationRanks(Skill.MEDICINE, 3);
    assertEquals(List.of("Too many skill points spent (1 too many)."), messages(character));
  }

  /** CRB p. 58: no skill above rank 3 during creation at level 1. */
  @Test
  void rankCapThreeAtLevelOne() {
    PlayerCharacter character = character("survivor", Map.of(Skill.SNEAK, new SkillState(true, 1, 0)), List.of());
    assertFalse(SkillRules.canRaise(character, Skill.SNEAK));
    assertTrue(SkillRules.canRaise(character, Skill.REPAIR));
    character.abilities().setCreationRanks(Skill.SNEAK, 2);
    assertTrue(messages(character).contains("Sneak may not exceed rank 3 during creation."));
  }

  /** CRB p. 53: Super Mutant skills at most rank 4, even from level 3 on. */
  @Test
  void superMutantCapFour() {
    PlayerCharacter mutant = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("super-mutant")).level(3)
        .skills(Map.of(Skill.MELEE_WEAPONS, new SkillState(true, 3, 0))).build();
    assertTrue(messages(mutant).contains("Melee Weapons may not exceed rank 4 (Super Mutant)."));
    PlayerCharacter atCap = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("super-mutant")).level(3)
        .skills(Map.of(Skill.MELEE_WEAPONS, new SkillState(true, 2, 0))).build();
    assertFalse(SkillRules.canRaise(atCap, Skill.MELEE_WEAPONS));
  }

  /** At level 1 the creation cap 3 is lower than the Super Mutant cap 4 (CRB p. 58, 53). */
  @Test
  void superMutantAtLevelOneUsesTheCreationCap() {
    PlayerCharacter mutant = character("super-mutant", Map.of(Skill.MELEE_WEAPONS, new SkillState(true, 2, 0)),
        List.of());
    assertTrue(messages(mutant).contains("Melee Weapons may not exceed rank 3 during creation."));
  }

  /** WG p. 15: Nomad forbids Science as a Tag skill; a full budget blocks new Tags. */
  @Test
  void canToggleTag() {
    PlayerCharacter tribal = character("tribal", Map.of(), List.of("nomad"));
    assertFalse(SkillRules.canToggleTag(tribal, Skill.SCIENCE));
    assertEquals("Nomad", tribal.abilities().tagForbiddenBy(Skill.SCIENCE).orElseThrow().name());
    assertTrue(SkillRules.canToggleTag(tribal, Skill.SURVIVAL));
    PlayerCharacter full = character("survivor", THREE_TAGS, List.of());
    assertFalse(SkillRules.canToggleTag(full, Skill.REPAIR));
    assertTrue(SkillRules.canToggleTag(full, Skill.SNEAK));
  }

  /** A free Tag can always be removed, even where a trait or the origin no longer allows it. */
  @Test
  void aFreeTagCanAlwaysBeRemoved() {
    PlayerCharacter loaded = character("tribal", Map.of(Skill.SCIENCE, new SkillState(true, 0, 0)), List.of("nomad"));
    assertTrue(SkillRules.canToggleTag(loaded, Skill.SCIENCE));
    loaded.abilities().setFreeTag(Skill.SCIENCE, false);
    assertFalse(loaded.abilities().isTagged(Skill.SCIENCE));
    PlayerCharacter ghoul = character("ghoul", Map.of(Skill.SURVIVAL, new SkillState(true, 0, 0)), List.of());
    assertTrue(SkillRules.canToggleTag(ghoul, Skill.SURVIVAL));
    ghoul.abilities().setFreeTag(Skill.SURVIVAL, false);
    assertEquals(3, SkillRules.freeTagsLeft(ghoul));
    assertTrue(ghoul.abilities().isTagged(Skill.SURVIVAL));
  }

  @Test
  void completeCharactersHaveNoProblems() {
    PlayerCharacter complete = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor"))
        .creationComplete(true).build();
    assertEquals(List.of(), messages(complete));
  }

  private static Map<Skill, SkillState> tags(Skill... skills) {
    Map<Skill, SkillState> map = new EnumMap<>(Skill.class);
    for (Skill skill : skills) {
      map.put(skill, new SkillState(true, 0, 0));
    }
    return map;
  }

  private static PlayerCharacter character(String origin, Map<Skill, SkillState> skills, List<String> traits) {
    return PlayerCharacter.builder().name("Test").origin(CatalogRef.of(origin)).skills(skills)
        .traits(traits.stream().map(id -> new TraitSelection(CatalogRef.of(id), false, List.of())).toList()).build();
  }

  private static List<String> messages(PlayerCharacter character) {
    return SkillRules.problems(character).stream().map(CreationProblem::message).toList();
  }
}
