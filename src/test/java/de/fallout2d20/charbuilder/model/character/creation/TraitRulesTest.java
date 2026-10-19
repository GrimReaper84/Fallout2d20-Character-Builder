package de.fallout2d20.charbuilder.model.character.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.SkillState;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TraitRulesTest {

  /** CRB p. 56: two traits, or one trait and an extra perk. */
  @Test
  void survivorTraitCount() {
    assertEquals(List.of("Choose two traits, or one trait and an extra perk."), messages(survivor()));
    assertEquals(List.of("Choose two traits, or one trait and an extra perk."),
        messages(survivor("educated", "fast-shot", "small-frame")));
    assertEquals(List.of(), messages(survivor("educated")));
    assertEquals(List.of(), messages(survivor("educated", "fast-shot")));
  }

  @Test
  void originsWithoutTraitListsOfferNoTraits() {
    assertEquals(List.of("Ghoul offers no traits."),
        messages(character("ghoul", List.of(trait("educated")), Map.of())));
  }

  @Test
  void traitsMustBeOnTheOriginsLists() {
    assertEquals(List.of("Nomad is not on the trait lists of Survivor."),
        messages(survivor("educated", "nomad")).subList(0, 1));
    assertEquals(List.of("Good Natured is not a trait of the supported books."),
        messages(character("survivor", List.of(trait("educated"),
            new TraitSelection(new CatalogRef("good-natured", "Good Natured"), false, List.of())),
            Map.of())));
  }

  @Test
  void aTraitCannotBeChosenTwice() {
    assertEquals(List.of("Educated is chosen twice."), messages(survivor("educated", "educated")));
  }

  /** WG p. 15: two Tribal or two Survivor traits, but not one of each. */
  @Test
  void tribalTraitsComeFromOneList() {
    assertEquals(List.of(), messages(tribal("nomad", "rite-of-passage")));
    assertEquals(List.of(), messages(tribal("educated", "fast-shot")));
    assertEquals(List.of("Both traits must come from the same list."),
        messages(tribal("rite-of-passage", "educated")));
    assertEquals(List.of(), messages(tribal("the-chosen-one")));
  }

  /** CRB p. 56: Gifted raises two different attributes. */
  @Test
  void giftedNeedsTwoDifferentAttributes() {
    String problem = "Choose 2 different attributes for Gifted.";
    assertEquals(List.of(problem), messages(gifted()));
    assertEquals(List.of(problem), messages(gifted(Attribute.STRENGTH, Attribute.STRENGTH)));
    assertEquals(List.of(), messages(gifted(Attribute.STRENGTH, Attribute.LUCK)));
  }

  /** WG p. 15: with Nomad, Science cannot be a Tag skill. */
  @Test
  void nomadForbidsScienceAsTag() {
    PlayerCharacter nomad = character("tribal", List.of(trait("nomad"), trait("rite-of-passage")),
        Map.of(Skill.SCIENCE, new SkillState(true, 0, 0)));
    assertEquals(List.of("Science cannot be a Tag skill with Nomad."), messages(nomad));
  }

  @Test
  void importedGiftedNeedsNoChoicesAndCompleteCharactersAreNotChecked() {
    PlayerCharacter imported = character("survivor",
        List.of(new TraitSelection(CatalogRef.of("gifted"), true, List.of()), trait("educated")), Map.of());
    assertEquals(List.of(), messages(imported));
    PlayerCharacter complete = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("ghoul"))
        .creationComplete(true).traits(List.of(trait("educated"))).build();
    assertTrue(TraitRules.problems(complete).isEmpty());
  }

  private static PlayerCharacter gifted(Attribute... choices) {
    return character("survivor", List.of(trait("educated"),
        new TraitSelection(CatalogRef.of("gifted"), false, Arrays.asList(choices))), Map.of());
  }

  private static PlayerCharacter survivor(String... traits) {
    return character("survivor", Arrays.stream(traits).map(TraitRulesTest::trait).toList(), Map.of());
  }

  private static PlayerCharacter tribal(String... traits) {
    return character("tribal", Arrays.stream(traits).map(TraitRulesTest::trait).toList(), Map.of());
  }

  private static TraitSelection trait(String id) {
    return new TraitSelection(CatalogRef.of(id), false, List.of());
  }

  private static PlayerCharacter character(String origin, List<TraitSelection> traits,
      Map<Skill, SkillState> skills) {
    return PlayerCharacter.builder().name("Test").origin(CatalogRef.of(origin)).skills(skills).traits(traits).build();
  }

  private static List<String> messages(PlayerCharacter character) {
    return TraitRules.problems(character).stream().map(CreationProblem::message).toList();
  }
}
