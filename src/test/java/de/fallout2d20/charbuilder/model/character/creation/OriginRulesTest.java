package de.fallout2d20.charbuilder.model.character.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.SkillState;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OriginRulesTest {

  @Test
  void requiresAnOriginOfTheSupportedBooks() {
    assertEquals(List.of("Choose an origin."), messages(PlayerCharacter.createNew()));
    assertEquals(List.of("\"NCR\" is not an origin of the supported books."),
        messages(character(new CatalogRef("legacy-ncr", "NCR"), EnumSet.allOf(Book.class),
            Map.of(), Map.of(), List.of())));
  }

  @Test
  void originMustBeAvailableWithTheEnabledBooks() {
    PlayerCharacter synth = character("generation-3-synth", EnumSet.of(Book.CRB, Book.WG));
    synth.chooseOriginTagSkill(Skill.SCIENCE);
    assertEquals(List.of("Generation 3 Synth needs a book that is not enabled."), messages(synth));
  }

  /** CRB p. 53: INT/CHA at most 6; the skill cap 4 is checked by SkillRules. */
  @Test
  void superMutantMaximums() {
    PlayerCharacter mutant = character(CatalogRef.of("super-mutant"), EnumSet.allOf(Book.class),
        Map.of(Attribute.INTELLIGENCE, 2, Attribute.STRENGTH, 5),
        Map.of(Skill.MELEE_WEAPONS, new SkillState(true, 3, 0)), List.of());
    assertEquals(List.of("Intelligence may not exceed 6 (Super Mutant)."), messages(mutant));
  }

  /** WG p. 14: Nightkin INT/CHA at most 8. */
  @Test
  void nightkinIntelligenceMaximumIsEight() {
    Map<Attribute, Integer> eight = Map.of(Attribute.INTELLIGENCE, 3);
    assertEquals(List.of(), messages(character(CatalogRef.of("nightkin"), EnumSet.allOf(Book.class),
        eight, Map.of(), List.of())));
    Map<Attribute, Integer> nine = Map.of(Attribute.INTELLIGENCE, 4);
    assertEquals(List.of("Intelligence may not exceed 8 (Nightkin)."), messages(character(
        CatalogRef.of("nightkin"), EnumSet.allOf(Book.class), nine, Map.of(), List.of())));
  }

  /** CRB p. 58: no attribute above 10 for origins without own maximums. */
  @Test
  void generalAttributeMaximumIsTen() {
    PlayerCharacter ghoul = character(CatalogRef.of("ghoul"), EnumSet.allOf(Book.class),
        Map.of(Attribute.AGILITY, 6), Map.of(), List.of());
    assertEquals(List.of("Agility may not exceed 10 (Ghoul)."), messages(ghoul));
  }

  /** CRB p. 58: Gifted (CRB p. 56) may not push an attribute above 10 either. */
  @Test
  void giftedCannotExceedTheAttributeMaximum() {
    PlayerCharacter survivor = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor"))
        .attributeAllocation(Map.of(Attribute.AGILITY, 5))
        .traits(List.of(new TraitSelection(CatalogRef.of("gifted"), false, List.of(Attribute.AGILITY, Attribute.LUCK))))
        .build();
    assertEquals(List.of("Agility may not exceed 10 (Survivor)."),
        OriginRules.problems(survivor).stream().map(CreationProblem::message).toList());
  }

  /** CRB p. 51: the extra Tag skill must be Energy Weapons, Science or Repair and not taken twice. */
  @Test
  void brotherhoodExtraTagSkill() {
    PlayerCharacter initiate = character(CatalogRef.of("brotherhood-initiate"), EnumSet.allOf(Book.class),
        Map.of(), Map.of(Skill.REPAIR, new SkillState(true, 0, 0)), List.of());
    assertEquals(List.of("Choose the extra Tag skill granted by Brotherhood Initiate."), messages(initiate));
    initiate.chooseOriginTagSkill(Skill.SNEAK);
    assertEquals(List.of("Sneak cannot be the extra Tag skill granted by Brotherhood Initiate."),
        messages(initiate));
    initiate.chooseOriginTagSkill(Skill.SCIENCE);
    assertEquals(List.of(), messages(initiate));
  }

  /**
   * A saved character may carry a free Tag on the extra Tag skill (choosing the extra Tag in the
   * model removes the free one, so only loaded data reaches this state).
   */
  @Test
  void extraTagSkillAlreadyFreelyTagged() {
    PlayerCharacter loaded = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("brotherhood-initiate"))
        .originChoices(OriginChoices.NONE.withTagSkill(Skill.REPAIR))
        .skills(Map.of(Skill.REPAIR, new SkillState(true, 0, 0))).build();
    assertEquals(List.of("Repair is already a Tag skill; choose another extra Tag skill."),
        messages(loaded));
  }

  /** CRB p. 57: the Vault Dweller may tag any skill. */
  @Test
  void vaultDwellerMayTagAnySkill() {
    PlayerCharacter dweller = character("vault-dweller", EnumSet.allOf(Book.class));
    dweller.chooseOriginTagSkill(Skill.SNEAK);
    assertEquals(List.of(), messages(dweller));
  }

  /** CRB p. 56: one trait instead of two gives an extra perk (the trait count is a trait rule). */
  @Test
  void survivorWithOneTraitGetsAnExtraPerk() {
    assertEquals(1, OriginRules.extraPerks(survivor(1)));
    assertEquals(0, OriginRules.extraPerks(survivor(2)));
  }

  /** WG p. 15: a Tribal may also take one trait and an extra perk. */
  @Test
  void tribalWithOneTraitGetsAnExtraPerk() {
    PlayerCharacter tribal = character(CatalogRef.of("tribal"), EnumSet.allOf(Book.class), Map.of(),
        Map.of(), List.of(new TraitSelection(CatalogRef.of("nomad"), false, List.of())));
    assertEquals(List.of(), messages(tribal));
    assertEquals(1, OriginRules.extraPerks(tribal));
  }

  /** WOA p. 25: one extra perk at level 1. */
  @Test
  void childOfAtomHasAnExtraPerk() {
    assertEquals(1, OriginRules.extraPerks(character("child-of-atom", EnumSet.allOf(Book.class))));
    assertEquals(0, OriginRules.extraPerks(character("ghoul", EnumSet.allOf(Book.class))));
  }

  /** WOA p. 21: a Protectron needs a purpose. */
  @Test
  void protectronNeedsAPurpose() {
    PlayerCharacter protectron = character("protectron", EnumSet.allOf(Book.class));
    assertEquals(List.of("Choose a purpose (Protectron)."), messages(protectron));
    protectron.setPurpose("Law Enforcer");
    assertEquals(List.of(), messages(protectron));
  }

  /** Choices that do not belong to the origin can only come from edited files. */
  @Test
  void reportsChoicesTheOriginDoesNotHave() {
    PlayerCharacter ghoul = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("ghoul"))
        .originChoices(new OriginChoices(Skill.SNEAK, "Medic")).build();
    assertEquals(Set.of("Ghoul has no purpose to choose.", "Ghoul grants no extra Tag skill."),
        Set.copyOf(messages(ghoul)));
  }

  @Test
  void completeCharactersAreNotChecked() {
    PlayerCharacter complete = PlayerCharacter.builder().name("Test").creationComplete(true).build();
    assertTrue(OriginRules.problems(complete).isEmpty());
  }

  private static PlayerCharacter survivor(int traits) {
    List<TraitSelection> selections = List.of("educated", "fast-shot", "gifted").subList(0, traits)
        .stream().map(id -> new TraitSelection(CatalogRef.of(id), false, List.of())).toList();
    return character(CatalogRef.of("survivor"), EnumSet.allOf(Book.class), Map.of(), Map.of(),
        selections);
  }

  private static PlayerCharacter character(String origin, Set<Book> books) {
    PlayerCharacter character = PlayerCharacter.builder().name("Test").enabledBooks(books).build();
    character.chooseOrigin(CatalogRef.of(origin));
    return character;
  }

  private static PlayerCharacter character(CatalogRef origin, Set<Book> books,
      Map<Attribute, Integer> allocation, Map<Skill, SkillState> skills, List<TraitSelection> traits) {
    return PlayerCharacter.builder().name("Test").origin(origin).enabledBooks(books).attributeAllocation(allocation)
        .skills(skills).traits(traits).build();
  }

  private static List<String> messages(PlayerCharacter character) {
    return OriginRules.problems(character).stream().map(CreationProblem::message).toList();
  }
}
