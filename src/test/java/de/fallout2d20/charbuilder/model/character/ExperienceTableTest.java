package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExperienceTableTest {

  /** CRB p. 49, Level and Experience table. */
  private static final long[] BOOK = {
    0, 100, 300, 600, 1_000, 1_500, 2_100, 2_800, 3_600, 4_500,
    5_500, 6_600, 7_800, 9_100, 10_500, 12_000, 13_600, 15_300, 17_100, 19_000, 21_000
  };

  @Test
  void matchesTheTableUpToLevel21() {
    for (int level = 1; level <= BOOK.length; level++) {
      assertEquals(BOOK[level - 1], ExperienceTable.xpForLevel(level), "level " + level);
    }
  }

  /** CRB p. 49: beyond level 21, level × (level − 1) / 2 × 100. */
  @Test
  void usesTheFormulaBeyondLevel21() {
    assertEquals(23_100, ExperienceTable.xpForLevel(22));
    assertEquals(30_000, ExperienceTable.xpForLevel(25));
  }

  @Test
  void findsTheLevelForXp() {
    assertEquals(1, ExperienceTable.levelForXp(0));
    assertEquals(2, ExperienceTable.levelForXp(299));
    assertEquals(3, ExperienceTable.levelForXp(300));
    assertEquals(21, ExperienceTable.levelForXp(23_099));
    assertEquals(22, ExperienceTable.levelForXp(23_100));
  }
}
