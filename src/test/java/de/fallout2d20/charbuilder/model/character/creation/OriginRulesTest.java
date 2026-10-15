package de.fallout2d20.charbuilder.model.character.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.SkillState;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import de.fallout2d20.charbuilder.model.character.Vitals;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OriginRulesTest {

  @Test
  void requiresAnOriginOfTheSupportedBooks() {
    assertEquals(List.of("Choose an origin."), messages(Character.createNew()));
    assertEquals(List.of("\"NCR\" is not an origin of the supported books."),
        messages(character(new CatalogRef("legacy-ncr", "NCR"), EnumSet.allOf(Book.class),
            Map.of(), Map.of(), List.of())));
  }

  @Test
  void originMustBeAvailableWithTheEnabledBooks() {
    Character synth = character("generation-3-synth", EnumSet.of(Book.CRB, Book.WG));
    synth.chooseOriginTagSkill(Skill.SCIENCE);
    assertEquals(List.of("Generation 3 Synth needs a book that is not enabled."), messages(synth));
  }

  /** CRB p. 53: INT/CHA at most 6, skills at most rank 4. */
  @Test
  void superMutantMaximumsAndSkillCap() {
    Character mutant = character(CatalogRef.of("super-mutant"), EnumSet.allOf(Book.class),
        Map.of(Attribute.INTELLIGENCE, 2, Attribute.STRENGTH, 5),
        Map.of(Skill.MELEE_WEAPONS, new SkillState(true, 3, 0)), List.of());
    assertEquals(List.of("Intelligence may not exceed 6 (Super Mutant).",
        "Melee Weapons may not exceed rank 4 (Super Mutant)."), messages(mutant));
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
    Character ghoul = character(CatalogRef.of("ghoul"), EnumSet.allOf(Book.class),
        Map.of(Attribute.AGILITY, 6), Map.of(), List.of());
    assertEquals(List.of("Agility may not exceed 10 (Ghoul)."), messages(ghoul));
  }

  /** CRB p. 58: Gifted (CRB p. 56) may not push an attribute above 10 either. */
  @Test
  void giftedCannotExceedTheAttributeMaximum() {
    Character survivor = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 1, 0,
        false, EnumSet.allOf(Book.class), Map.of(Attribute.AGILITY, 5), Map.of(),
        List.of(new TraitSelection(CatalogRef.of("gifted"), false,
            List.of(Attribute.AGILITY, Attribute.LUCK))),
        List.of(), Progression.NONE, Vitals.FULL, null, null);
    assertEquals(List.of("Agility may not exceed 10 (Survivor)."),
        OriginRules.problems(survivor).stream().map(CreationProblem::message).toList());
  }

  /** CRB p. 51: the extra Tag skill must be Energy Weapons, Science or Repair and not taken twice. */
  @Test
  void brotherhoodExtraTagSkill() {
    Character initiate = character(CatalogRef.of("brotherhood-initiate"), EnumSet.allOf(Book.class),
        Map.of(), Map.of(Skill.REPAIR, new SkillState(true, 0, 0)), List.of());
    assertEquals(List.of("Choose the extra Tag skill granted by Brotherhood Initiate."), messages(initiate));
    initiate.chooseOriginTagSkill(Skill.SNEAK);
    assertEquals(List.of("Sneak cannot be the extra Tag skill granted by Brotherhood Initiate."),
        messages(initiate));
    initiate.chooseOriginTagSkill(Skill.REPAIR);
    assertEquals(List.of("Repair is already a Tag skill; choose another extra Tag skill."),
        messages(initiate));
    initiate.chooseOriginTagSkill(Skill.SCIENCE);
    assertEquals(List.of(), messages(initiate));
  }

  /** CRB p. 57: the Vault Dweller may tag any skill. */
  @Test
  void vaultDwellerMayTagAnySkill() {
    Character dweller = character("vault-dweller", EnumSet.allOf(Book.class));
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
    Character tribal = character(CatalogRef.of("tribal"), EnumSet.allOf(Book.class), Map.of(),
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
    Character protectron = character("protectron", EnumSet.allOf(Book.class));
    assertEquals(List.of("Choose a purpose (Protectron)."), messages(protectron));
    protectron.setPurpose("Law Enforcer");
    assertEquals(List.of(), messages(protectron));
  }

  /** Choices that do not belong to the origin can only come from edited files. */
  @Test
  void reportsChoicesTheOriginDoesNotHave() {
    Character ghoul = new Character("Test", CatalogRef.of("ghoul"),
        new OriginChoices(Skill.SNEAK, true, "Medic"), 1, 0, false, EnumSet.allOf(Book.class),
        Map.of(), Map.of(), List.of(), List.of(), Progression.NONE, Vitals.FULL, null, null);
    assertEquals(Set.of("Ghoul has no purpose to choose.", "Only a Mister Handy can go without a pincer.",
        "Ghoul grants no extra Tag skill."), Set.copyOf(messages(ghoul)));
  }

  @Test
  void completeCharactersAreNotChecked() {
    Character complete = new Character("Test", null, OriginChoices.NONE, 1, 0, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(), Progression.NONE,
        Vitals.FULL, null, null);
    assertTrue(OriginRules.problems(complete).isEmpty());
  }

  private static Character survivor(int traits) {
    List<TraitSelection> selections = List.of("educated", "fast-shot", "gifted").subList(0, traits)
        .stream().map(id -> new TraitSelection(CatalogRef.of(id), false, List.of())).toList();
    return character(CatalogRef.of("survivor"), EnumSet.allOf(Book.class), Map.of(), Map.of(),
        selections);
  }

  private static Character character(String origin, Set<Book> books) {
    Character character = new Character("Test", null, OriginChoices.NONE, 1, 0, false, books,
        Map.of(), Map.of(), List.of(), List.of(), Progression.NONE, Vitals.FULL, null, null);
    character.chooseOrigin(CatalogRef.of(origin));
    return character;
  }

  private static Character character(CatalogRef origin, Set<Book> books,
      Map<Attribute, Integer> allocation, Map<Skill, SkillState> skills, List<TraitSelection> traits) {
    return new Character("Test", origin, OriginChoices.NONE, 1, 0, false, books, allocation, skills,
        traits, List.of(), Progression.NONE, Vitals.FULL, null, null);
  }

  private static List<String> messages(Character character) {
    return OriginRules.problems(character).stream().map(CreationProblem::message).toList();
  }
}
