package de.fallout2d20.charbuilder.catalog.perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PerkDefinitionTest {

  /** CRB p. 59: Life Giver taken at level 5 needs level 10 for rank 2 and 15 for rank 3. */
  @Test
  void levelForRankFollowsTheBookExample() {
    PerkRequirements lifeGiver = PerkCatalog.byId("life-giver").orElseThrow().requirements();
    assertEquals(5, lifeGiver.levelForRank(1));
    assertEquals(10, lifeGiver.levelForRank(2));
    assertEquals(15, lifeGiver.levelForRank(3));
  }

  /** User decision: no level means level 1 (Size Matters: rank 2 at 5, rank 3 at 9). */
  @Test
  void missingLevelMeansLevelOne() {
    PerkRequirements sizeMatters = PerkCatalog.byId("size-matters").orElseThrow().requirements();
    assertEquals(1, sizeMatters.levelForRank(1));
    assertEquals(5, sizeMatters.levelForRank(2));
    assertEquals(9, sizeMatters.levelForRank(3));
    PerkRequirements armorer = PerkCatalog.byId("armorer").orElseThrow().requirements();
    assertEquals(1, armorer.levelForRank(4));
  }

  @Test
  void rejectsInvalidEntries() {
    List<Source> sources = List.of(new Source(Book.CRB, 59));
    assertThrows(IllegalArgumentException.class, () -> perk(sources, 0));
    assertThrows(IllegalArgumentException.class, () -> perk(List.of(), 1));
    assertThrows(IllegalArgumentException.class,
        () -> new PerkRequirements(Map.of(), 0, null, false, List.of()));
  }

  @Test
  void missingRequirementsMeanNone() {
    assertEquals(PerkRequirements.NONE, perk(List.of(new Source(Book.CRB, 59)), 1).requirements());
  }

  private static PerkDefinition perk(List<Source> sources, int ranks) {
    return new PerkDefinition("test", "Test", sources, ranks, null, null, "Summary.", null,
        0, 0, 0, 0, 0, 0, false, 0);
  }
}
