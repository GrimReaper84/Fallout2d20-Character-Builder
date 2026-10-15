package de.fallout2d20.charbuilder.model.character;

import java.util.OptionalInt;
import org.jspecify.annotations.NonNull;

/**
 * Extra caps and maximum item rarity for characters starting above level 1.
 *
 * <p>CRB p. 81 (Additional Caps by Starting Level): for levels 2 to 20 the table lists the caps and the
 * highest rarity of the equipment they may buy; for level 21 and above it says "+ level × 50" with any
 * rarity. Every table row adds level × 50 to the row before, so the caps of level L are
 * 25 × L × (L + 1) − 50 for every level from 2 on; beyond 20 this continues the table (user decision;
 * level 21: 11,500). Levels so high that the caps no longer fit into a {@code long} throw an
 * {@link ArithmeticException} instead of overflowing.
 */
public final class StartingEquipmentAllowance {

  private static final int TABLE_FIRST_LEVEL = 2;
  private static final int[] MAX_RARITY = {1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5};
  private static final int TABLE_LAST_LEVEL = TABLE_FIRST_LEVEL + MAX_RARITY.length - 1;

  private StartingEquipmentAllowance() {}

  /**
   * Extra caps for a character starting at {@code level} (at least 1); none at level 1.
   *
   * @throws ArithmeticException for absurd levels whose caps do not fit into a {@code long}
   */
  public static long caps(int level) {
    if (level < 1) {
      throw new IllegalArgumentException("Level must be at least 1");
    }
    if (level < TABLE_FIRST_LEVEL) {
      return 0;
    }
    return Math.multiplyExact(25L, (long) level * (level + 1)) - 50;
  }

  /**
   * Highest rarity the extra caps may buy at {@code level} (at least 2, the first row of the table);
   * empty for any rarity (level 21 and above).
   */
  public static @NonNull OptionalInt maxRarity(int level) {
    if (level < TABLE_FIRST_LEVEL) {
      throw new IllegalArgumentException("The table starts at level " + TABLE_FIRST_LEVEL);
    }
    return level <= TABLE_LAST_LEVEL ? OptionalInt.of(MAX_RARITY[level - TABLE_FIRST_LEVEL]) : OptionalInt.empty();
  }
}
