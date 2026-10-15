package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class StartingEquipmentAllowanceTest {

  /** CRB p. 81: Additional Caps by Starting Level. */
  @Test
  void matchesTheBookTable() {
    assertEquals(100, StartingEquipmentAllowance.caps(2));
    assertEquals(OptionalInt.of(1), StartingEquipmentAllowance.maxRarity(2));
    assertEquals(700, StartingEquipmentAllowance.caps(5));
    assertEquals(OptionalInt.of(2), StartingEquipmentAllowance.maxRarity(5));
    assertEquals(2_200, StartingEquipmentAllowance.caps(9));
    assertEquals(OptionalInt.of(3), StartingEquipmentAllowance.maxRarity(9));
    assertEquals(4_500, StartingEquipmentAllowance.caps(13));
    assertEquals(OptionalInt.of(4), StartingEquipmentAllowance.maxRarity(13));
    assertEquals(10_450, StartingEquipmentAllowance.caps(20));
    assertEquals(OptionalInt.of(5), StartingEquipmentAllowance.maxRarity(20));
  }

  /** CRB p. 81: every row of the table, checked against the closed formula. */
  @Test
  void everyTableRow() {
    long[] book = {100, 250, 450, 700, 1_000, 1_350, 1_750, 2_200, 2_700, 3_250,
        3_850, 4_500, 5_200, 5_950, 6_750, 7_600, 8_500, 9_450, 10_450};
    for (int level = 2; level <= 20; level++) {
      assertEquals(book[level - 2], StartingEquipmentAllowance.caps(level), "level " + level);
    }
  }

  /** No overflow or endless loop: huge levels either compute or fail loudly, never go negative. */
  @Test
  void hugeLevelsNeverOverflow() {
    assertTrue(StartingEquipmentAllowance.caps(400_000_000) > 0);
    assertThrows(ArithmeticException.class, () -> StartingEquipmentAllowance.caps(Integer.MAX_VALUE));
    assertEquals(OptionalInt.empty(), StartingEquipmentAllowance.maxRarity(Integer.MAX_VALUE));
  }

  /** User decision: from 21 each level adds level × 50 caps; any rarity. */
  @Test
  void beyondLevelTwenty() {
    assertEquals(11_500, StartingEquipmentAllowance.caps(21));
    assertEquals(12_600, StartingEquipmentAllowance.caps(22));
    assertEquals(OptionalInt.empty(), StartingEquipmentAllowance.maxRarity(21));
  }

  @Test
  void levelOneHasNoExtraCaps() {
    assertEquals(0, StartingEquipmentAllowance.caps(1));
    assertThrows(IllegalArgumentException.class, () -> StartingEquipmentAllowance.caps(0));
    assertThrows(IllegalArgumentException.class, () -> StartingEquipmentAllowance.maxRarity(1));
  }
}
