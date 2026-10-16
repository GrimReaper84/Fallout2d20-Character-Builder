package de.fallout2d20.charbuilder.catalog.origin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class OriginDataTest {

  private static final List<Source> CRB = List.of(new Source(Book.CRB, 56));

  @Test
  void rejectsIncompleteEntriesAndInvalidEffects() {
    assertThrows(IllegalArgumentException.class, () -> origin(List.of(), "Summary", null, 0));
    assertThrows(IllegalArgumentException.class, () -> origin(CRB, " ", null, 0));
    assertThrows(IllegalArgumentException.class, () -> origin(CRB, "Summary", 0, 0));
    assertThrows(IllegalArgumentException.class, () -> origin(CRB, "Summary", null, -1));
    assertThrows(IllegalArgumentException.class, () -> new OriginData("x", "X", CRB, null, "Summary", null,
        false, false, false, 0, Map.of(), Map.of(), null, null, null, List.of(), 0, List.of(), -1));
  }

  @Test
  void rejectsInvalidCreationRules() {
    assertThrows(IllegalArgumentException.class,
        () -> rules(Map.of(), Map.of(Attribute.STRENGTH, 13), null, null, null));
    assertThrows(IllegalArgumentException.class,
        () -> rules(Map.of(Attribute.STRENGTH, 3), Map.of(), null, null, null));
    assertThrows(IllegalArgumentException.class, () -> rules(Map.of(), Map.of(), 7, null, null));
    assertThrows(IllegalArgumentException.class, () -> rules(Map.of(), Map.of(), null,
        Skill.SURVIVAL, new ExtraTagSkill(List.of(Skill.SURVIVAL))));
  }

  /** Absent optional JSON fields mean "no rule": general maximum 10, no modifier, any extra Tag. */
  @Test
  void absentRulesFallBackToTheGeneralRules() {
    OriginData origin = rules(null, null, null, null, null);
    assertEquals(10, origin.attributeMaximum(Attribute.INTELLIGENCE));
    assertEquals(0, origin.attributeModifier(Attribute.STRENGTH));
    assertEquals(List.of(), origin.traitLists());
    assertEquals(true, new ExtraTagSkill(null).allows(Skill.SNEAK));
  }

  private static OriginData origin(List<Source> sources, String summary,
      @Nullable Integer fixedCarry, int baseRadiationDr) {
    return new OriginData("x", "X", sources, null, summary, fixedCarry, false, false, false,
        baseRadiationDr, Map.of(), Map.of(), null, null, null, List.of(), 0, List.of(), 0);
  }

  private static OriginData rules(@Nullable Map<Attribute, Integer> modifiers,
      @Nullable Map<Attribute, Integer> maximums, @Nullable Integer skillCap,
      @Nullable Skill fixedTag, @Nullable ExtraTagSkill extraTag) {
    return new OriginData("x", "X", CRB, null, "Summary", null, false, false, false, 0, modifiers,
        maximums, skillCap, fixedTag, extraTag, null, 0, null, 0);
  }
}
