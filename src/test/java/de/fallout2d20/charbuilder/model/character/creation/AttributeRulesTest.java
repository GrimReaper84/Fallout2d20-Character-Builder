package de.fallout2d20.charbuilder.model.character.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Vitals;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AttributeRulesTest {

  /** CRB p. 58 Balanced array: 6, 6, 6, 6, 6, 5, 5. */
  @Test
  void balancedArrayIsValid() {
    Character character = character("survivor", 1, 1, 1, 1, 1, 0, 0);
    assertEquals(0, AttributeRules.pointsLeft(character));
    assertEquals(List.of(), messages(character));
  }

  /** CRB p. 58 Focused (8, 7, 6, 6, 5, 4, 4) and Specialized (9, 8, 5, 5, 5, 4, 4) arrays. */
  @Test
  void focusedAndSpecializedArraysAreValid() {
    assertEquals(List.of(), messages(character("survivor", 3, 2, 1, 1, 0, -1, -1)));
    assertEquals(List.of(), messages(character("survivor", 4, 3, 0, 0, 0, -1, -1)));
  }

  /** User decision: all points must be spent. */
  @Test
  void unspentAndOverspentPoints() {
    Character fresh = Character.createNew();
    assertEquals(5, AttributeRules.pointsLeft(fresh));
    assertEquals(List.of("Spend all S.P.E.C.I.A.L. points (5 left)."), messages(fresh));
    assertEquals(List.of("Too many S.P.E.C.I.A.L. points spent (1 too many)."),
        messages(character("survivor", 2, 2, 2, 0, 0, 0, 0)));
  }

  /** CRB p. 58: an attribute may be lowered by one point, not more. */
  @Test
  void lowerOnlyOneBelowTheStartingValue() {
    Character character = character("survivor", 3, 2, 1, 1, 0, 0, -2);
    assertEquals(List.of("Luck may be lowered by at most 1 below its starting value."), messages(character));
    assertFalse(AttributeRules.canLower(character, Attribute.LUCK));
    assertTrue(AttributeRules.canLower(character, Attribute.AGILITY));
  }

  /** User decision: Super Mutant STR (starting at 7, CRB p. 53) may drop to 6, not to 5. */
  @Test
  void superMutantStrengthMayDropToSix() {
    Character mutant = character("super-mutant", -1, 1, 1, 1, 1, 1, 1);
    assertEquals(6, mutant.attributeValue(Attribute.STRENGTH));
    assertEquals(List.of(), messages(mutant));
    assertFalse(AttributeRules.canLower(mutant, Attribute.STRENGTH));
    Character five = character("super-mutant", -2, 1, 1, 1, 1, 1, 2);
    assertEquals(List.of("Strength may be lowered by at most 1 below its starting value."), messages(five));
  }

  /** CRB p. 53: Super Mutant INT at most 6; no raising without points left. */
  @Test
  void canRaiseRespectsMaximumAndBudget() {
    Character mutant = character("super-mutant", 0, 0, 0, 0, 1, 0, 0);
    assertFalse(AttributeRules.canRaise(mutant, Attribute.INTELLIGENCE));
    assertTrue(AttributeRules.canRaise(mutant, Attribute.STRENGTH));
    Character spent = character("survivor", 1, 1, 1, 1, 1, 0, 0);
    assertFalse(AttributeRules.canRaise(spent, Attribute.LUCK));
  }

  @Test
  void completeCharactersHaveNoProblems() {
    Character complete = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 1, 0, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(), Progression.NONE,
        Vitals.FULL, null, null);
    assertEquals(List.of(), messages(complete));
  }

  /** Allocation in S.P.E.C.I.A.L. order. */
  private static Character character(String origin, int... allocation) {
    Map<Attribute, Integer> map = new EnumMap<>(Attribute.class);
    for (Attribute attribute : Attribute.values()) {
      map.put(attribute, allocation[attribute.ordinal()]);
    }
    return new Character("Test", CatalogRef.of(origin), OriginChoices.NONE, 1, 0, false,
        EnumSet.allOf(Book.class), map, Map.of(), List.of(), List.of(), Progression.NONE,
        Vitals.FULL, null, null);
  }

  private static List<String> messages(Character character) {
    return AttributeRules.problems(character).stream().map(CreationProblem::message).toList();
  }
}
