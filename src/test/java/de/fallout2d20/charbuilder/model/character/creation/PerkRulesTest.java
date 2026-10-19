package de.fallout2d20.charbuilder.model.character.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.SkillState;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PerkRulesTest {

  /** CRB p. 49: one perk per level; CRB p. 56: one trait gives an extra perk; WOA p. 25: Child of Atom. */
  @Test
  void budgetIsLevelPlusExtras() {
    assertEquals(1, PerkRules.budget(character("survivor", 1, Map.of(), List.of(), traits("educated", "gifted"))));
    assertEquals(2, PerkRules.budget(character("survivor", 1, Map.of(), List.of(), traits("educated"))));
    assertEquals(2, PerkRules.budget(character("child-of-atom", 1, Map.of(), List.of(), List.of())));
    assertEquals(3, PerkRules.budget(character("vault-dweller", 3, Map.of(), List.of(), List.of())));
  }

  /** User decision: all perks must be chosen. */
  @Test
  void allPerksMustBeChosen() {
    PlayerCharacter none = character("vault-dweller", 1, Map.of(), List.of(), List.of());
    assertEquals(List.of("Choose 1 more perk."), messages(none));
    PlayerCharacter two = character("vault-dweller", 1, Map.of(),
        List.of(perk("action-boy-girl", 1), perk("jury-rigging", 1)), List.of());
    assertEquals(List.of("Too many perks chosen (1 too many)."), messages(two));
    PlayerCharacter one = character("vault-dweller", 1, Map.of(), List.of(perk("action-boy-girl", 1)), List.of());
    assertEquals(List.of(), messages(one));
  }

  /** CRB p. 59: Adrenalin Rush needs STR 7. */
  @Test
  void attributeRequirement() {
    PlayerCharacter weak = character("vault-dweller", 1, Map.of(Attribute.STRENGTH, 1), List.of(), List.of());
    assertEquals(List.of("Requirement not met: STR 7."), PerkRules.blockers(weak, def("adrenalin-rush")));
    assertFalse(PerkRules.canAdd(weak, def("adrenalin-rush")));
    PlayerCharacter strong = character("vault-dweller", 1, Map.of(Attribute.STRENGTH, 2), List.of(), List.of());
    assertTrue(PerkRules.canAdd(strong, def("adrenalin-rush")));
  }

  /** CRB p. 59: Life Giver taken at level 5 needs level 10 for rank 2. */
  @Test
  void levelStepPerRank() {
    PlayerCharacter nine = character("vault-dweller", 9, Map.of(), List.of(perk("life-giver", 1)), List.of());
    assertEquals(List.of("Requirement not met: level 10."), PerkRules.blockers(nine, def("life-giver")));
    PlayerCharacter ten = character("vault-dweller", 10, Map.of(), List.of(perk("life-giver", 1)), List.of());
    assertEquals(List.of(), PerkRules.blockers(ten, def("life-giver")));
  }

  /** CRB p. 60: Barbarian needs level 4 and is not for robots. */
  @Test
  void robotsCannotTakeBarbarian() {
    PlayerCharacter handy = character("mister-handy", 4, Map.of(Attribute.STRENGTH, 2), List.of(), List.of());
    assertEquals(List.of("Requirement not met: no robot."), PerkRules.blockers(handy, def("barbarian")));
  }

  /** CRB p. 62: Cautious Nature and Daring Nature exclude each other. */
  @Test
  void cautiousAndDaringExclusive() {
    Map<Attribute, Integer> perceptive = Map.of(Attribute.PERCEPTION, 2, Attribute.LUCK, 2);
    PlayerCharacter cautious =
        character("vault-dweller", 2, perceptive, List.of(perk("cautious-nature", 1)), List.of());
    assertEquals(List.of("Requirement not met: not with Cautious Nature."),
        PerkRules.blockers(cautious, def("daring-nature")));
    PlayerCharacter both = character("vault-dweller", 2, perceptive,
        List.of(perk("cautious-nature", 1), perk("daring-nature", 1)), List.of());
    assertEquals(List.of("Cautious Nature: Requirement not met: not with Daring Nature.",
        "Daring Nature: Requirement not met: not with Cautious Nature."), messages(both));
  }

  /** User requests 2026-10-19: perks excluded by a chosen perk or for a robot are "not possible". */
  @Test
  void excludedByChosenPerk() {
    PlayerCharacter cautious = character("vault-dweller", 1, Map.of(), List.of(perk("cautious-nature", 1)), List.of());
    assertTrue(PerkRules.isImpossible(cautious, def("daring-nature")), "even with requirements unmet");
    assertFalse(PerkRules.isImpossible(cautious, def("action-boy-girl")));
    PlayerCharacter none = character("vault-dweller", 1, Map.of(), List.of(), List.of());
    assertFalse(PerkRules.isImpossible(none, def("daring-nature")));
    PlayerCharacter robot = character("mister-handy", 1, Map.of(), List.of(), List.of());
    PerkDefinition notForRobots = PerkCatalog.all().stream().filter(perk -> perk.requirements().notRobot())
        .findFirst().orElseThrow();
    assertTrue(PerkRules.isImpossible(robot, notForRobots), "robot origin");
    assertFalse(PerkRules.isImpossible(none, notForRobots));
  }

  @Test
  void maxRank() {
    PlayerCharacter character = character("vault-dweller", 1, Map.of(), List.of(perk("action-boy-girl", 1)), List.of());
    assertEquals(List.of("Maximum rank reached."), PerkRules.blockers(character, def("action-boy-girl")));
  }

  /** User decision: Intense Training from the same creation counts for other requirements. */
  @Test
  void intenseTrainingCountsForRequirements() {
    PlayerCharacter character = character("vault-dweller", 2, Map.of(Attribute.STRENGTH, 1),
        List.of(new PerkSelection(CatalogRef.of("intense-training"), 1, 0, List.of(Attribute.STRENGTH), List.of())),
        List.of());
    assertEquals(7, character.attributeValue(Attribute.STRENGTH));
    assertEquals(List.of(), PerkRules.blockers(character, def("adrenalin-rush")));
  }

  /** User decision: a perk that becomes invalid stays and gets a hint. */
  @Test
  void invalidatedPerkStaysWithHint() {
    PlayerCharacter character = character("vault-dweller", 4, Map.of(Attribute.STRENGTH, 2),
        List.of(perk("barbarian", 1), perk("jury-rigging", 1), perk("action-boy-girl", 1),
            perk("light-step", 1)), List.of());
    assertEquals(List.of(), messages(character));
    character.chooseOrigin(CatalogRef.of("mister-handy"));
    assertTrue(messages(character).contains("Barbarian: Requirement not met: no robot."));
    assertEquals(4, character.getPerks().size());
  }

  /** CRB p. 66: every rank of Intense Training needs its attribute, and none above 10. */
  @Test
  void choicesRequired() {
    PlayerCharacter missing = character("vault-dweller", 2, Map.of(),
        List.of(perk("intense-training", 1), perk("jury-rigging", 1)), List.of());
    assertEquals(List.of("Choose an attribute for every rank of Intense Training."), messages(missing));
    PlayerCharacter tooHigh = character("vault-dweller", 2, Map.of(Attribute.LUCK, 5),
        List.of(new PerkSelection(CatalogRef.of("intense-training"), 1, 0, List.of(Attribute.LUCK), List.of()),
            perk("jury-rigging", 1)), List.of());
    assertTrue(messages(tooHigh).contains("Intense Training cannot raise Luck above 10."));
    assertFalse(PerkRules.attributeOptions(tooHigh).contains(Attribute.LUCK));
  }

  /** CRB p. 66: no Intense Training option for an attribute at 10 (also a Super Mutant's STR). */
  @Test
  void noIntenseTrainingOptionAtTen() {
    PlayerCharacter mutant = character("super-mutant", 2, Map.of(Attribute.STRENGTH, 3), List.of(), List.of());
    assertEquals(10, mutant.attributeValue(Attribute.STRENGTH));
    assertFalse(PerkRules.attributeOptions(mutant).contains(Attribute.STRENGTH));
  }

  /** CRB p. 73: Tag! needs a skill that is not tagged yet. */
  @Test
  void tagNeedsAnUntaggedSkill() {
    Map<Skill, SkillState> allTagged = new EnumMap<>(Skill.class);
    for (Skill skill : Skill.values()) {
      allTagged.put(skill, new SkillState(true, 0, 0));
    }
    PlayerCharacter character = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("vault-dweller")).level(5)
        .skills(allTagged).build();
    assertEquals(List.of("No skill left to tag."), PerkRules.blockers(character, def("tag")));
  }

  @Test
  void tagNeedsItsSkillChoice() {
    PlayerCharacter character = character("vault-dweller", 5, Map.of(),
        List.of(perk("tag", 1), perk("action-boy-girl", 1), perk("jury-rigging", 1), perk("light-step", 1),
            perk("adamantium-skeleton", 1)), List.of());
    assertTrue(messages(character).contains("Choose a skill for every rank of Tag!."));
  }

  /** User decision (perk stays with a hint): a Tag! skill the origin tags later, or a trait forbids. */
  @Test
  void tagSkillTaggedOtherwiseOrForbidden() {
    PlayerCharacter ghoul = character("vault-dweller", 5, Map.of(),
        List.of(new PerkSelection(CatalogRef.of("tag"), 1, 0, List.of(), List.of(Skill.SURVIVAL))), List.of());
    ghoul.chooseOrigin(CatalogRef.of("ghoul"));
    assertTrue(messages(ghoul).contains("Tag!: Survival is already a Tag skill of the origin."));

    PlayerCharacter tribal = character("tribal", 5, Map.of(),
        List.of(new PerkSelection(CatalogRef.of("tag"), 1, 0, List.of(), List.of(Skill.SCIENCE))), List.of());
    tribal.chooseTrait(CatalogRef.of("nomad"));
    assertTrue(messages(tribal).contains("Tag!: Science cannot be a Tag skill with Nomad."));
  }

  /** User decision: Size Matters has no level, so rank 2 needs level 5 (step 4). */
  @Test
  void sizeMattersStepsFromLevelOne() {
    Map<Attribute, Integer> heavy = Map.of(Attribute.ENDURANCE, 2, Attribute.AGILITY, 1);
    PlayerCharacter four = character("vault-dweller", 4, heavy, List.of(perk("size-matters", 1)), List.of());
    assertEquals(List.of("Requirement not met: level 5."), PerkRules.blockers(four, def("size-matters")));
    PlayerCharacter five = character("vault-dweller", 5, heavy, List.of(perk("size-matters", 1)), List.of());
    assertEquals(List.of(), PerkRules.blockers(five, def("size-matters")));
  }

  @Test
  void unknownPerksAreReported() {
    PlayerCharacter character = character("vault-dweller", 1, Map.of(),
        List.of(new PerkSelection(new CatalogRef("thief", "Thief"), 1, 0, List.of(), List.of())), List.of());
    assertEquals(List.of("\"Thief\" is not a perk of the supported books."), messages(character));
  }

  @Test
  void perkOfABookThatIsNotEnabledIsBlockedEvenInGmMode() {
    PerkDefinition base = def("action-boy-girl");
    PerkDefinition fromWg = new PerkDefinition(base.id(), base.name(), List.of(new Source(Book.WG, 50)),
        base.ranks(), base.requirements(), base.choice(), base.summary(), base.legacyNames(),
        base.carryWeightPerRank(), base.physicalDrPerRank(), base.energyDrPerRank(), base.radiationDrPerRank(),
        base.poisonDrPerRank(), base.maxHpPerEndurance(), base.physicalDrByStrength(), base.skillPointsPerRank());
    PlayerCharacter crbOnly = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("vault-dweller"))
        .enabledBooks(EnumSet.of(Book.CRB)).build();

    assertEquals(List.of("Needs a book that is not enabled."), PerkRules.blockers(crbOnly, fromWg));
    crbOnly.setGmMode(true);
    assertFalse(PerkRules.canAdd(crbOnly, fromWg));
    assertTrue(PerkRules.canAdd(crbOnly, base));
  }

  @Test
  void completeCharactersHaveNoProblems() {
    PlayerCharacter complete = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("vault-dweller"))
        .creationComplete(true).build();
    assertEquals(List.of(), messages(complete));
  }

  private static PerkDefinition def(String id) {
    return PerkCatalog.byId(id).orElseThrow();
  }

  private static PerkSelection perk(String id, int rank) {
    return new PerkSelection(CatalogRef.of(id), rank, 0, List.of(), List.of());
  }

  private static List<TraitSelection> traits(String... ids) {
    return Arrays.stream(ids).map(id -> new TraitSelection(CatalogRef.of(id), false, List.of())).toList();
  }

  private static PlayerCharacter character(String origin, int level, Map<Attribute, Integer> allocation,
      List<PerkSelection> perks, List<TraitSelection> traits) {
    return PlayerCharacter.builder().name("Test").origin(CatalogRef.of(origin)).level(level)
        .attributeAllocation(allocation).traits(traits).perks(perks).build();
  }

  private static List<String> messages(PlayerCharacter character) {
    return PerkRules.problems(character).stream().map(CreationProblem::message).toList();
  }
}
