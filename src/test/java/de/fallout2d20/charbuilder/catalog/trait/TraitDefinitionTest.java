package de.fallout2d20.charbuilder.catalog.trait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class TraitDefinitionTest {

  private static final List<Source> CRB = List.of(new Source(Book.CRB, 56));

  @Test
  void rejectsIncompleteEntriesAndInvalidEffects() {
    assertThrows(IllegalArgumentException.class, () -> trait(List.of(), "Benefit", 0, null));
    assertThrows(IllegalArgumentException.class, () -> trait(CRB, " ", 0, null));
    assertThrows(IllegalArgumentException.class, () -> trait(CRB, "Benefit", 8, null));
    assertThrows(IllegalArgumentException.class, () -> trait(CRB, "Benefit", 0, 0));
  }

  @Test
  void absentEffectsMeanNoEffect() {
    assertEquals(List.of(), trait(CRB, "Benefit", 0, null).forbiddenTagSkills());
  }

  private static TraitDefinition trait(List<Source> sources, String benefit, int attributeChoices,
      @Nullable Integer carryPerStrength) {
    return new TraitDefinition("x", "X", "survivor", sources, benefit, "Penalty", 0, attributeChoices,
        null, carryPerStrength, 0, 0);
  }
}
