package de.fallout2d20.charbuilder.model.character;

/**
 * Experience points needed per level.
 *
 * <p>CRB p. 49: the Level and Experience table lists the total XP for levels 1 to 21; beyond level
 * 21 a level needs level × (level − 1) / 2 × 100 total XP, which also reproduces the table.
 */
public final class ExperienceTable {

  private static final long[] TABLE = {
    0, 100, 300, 600, 1_000, 1_500, 2_100, 2_800, 3_600, 4_500,
    5_500, 6_600, 7_800, 9_100, 10_500, 12_000, 13_600, 15_300, 17_100, 19_000, 21_000
  };

  private ExperienceTable() {}

  /** Total XP needed to reach {@code level} (at least 1). */
  public static long xpForLevel(int level) {
    if (level < 1) {
      throw new IllegalArgumentException("Level must be at least 1");
    }
    if (level <= TABLE.length) {
      return TABLE[level - 1];
    }
    return (long) level * (level - 1) / 2 * 100;
  }

  /** Highest level whose XP threshold is reached by {@code xp}. */
  public static int levelForXp(long xp) {
    if (xp < 0) {
      throw new IllegalArgumentException("XP must not be negative");
    }
    int level = 1;
    while (xpForLevel(level + 1) <= xp) {
      level++;
    }
    return level;
  }
}
