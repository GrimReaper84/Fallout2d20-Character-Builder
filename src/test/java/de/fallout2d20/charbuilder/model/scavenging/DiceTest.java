package de.fallout2d20.charbuilder.model.scavenging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.fallout2d20.charbuilder.FixedDice;
import de.fallout2d20.charbuilder.catalog.loot.LootCatalog;
import de.fallout2d20.charbuilder.catalog.loot.LootRow;
import de.fallout2d20.charbuilder.catalog.loot.LootTable;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class DiceTest {

  private static Dice dice(Integer... results) {
    return new Dice(FixedDice.of(results));
  }

  /** CRB p. 29: faces 1 and 2 count their value, 3 and 4 nothing, 5 and 6 one plus an Effect. */
  @Test
  void combatDiceFaces() {
    assertEquals(new CombatRoll(1 + 2 + 0 + 0 + 1 + 1, 2), dice(1, 2, 3, 4, 5, 6).combatDice(6));
  }

  @Test
  void quantityForms() {
    assertEquals(new RolledQuantity(1, "1"), dice().quantity("1"));
    assertEquals(new RolledQuantity(17, "2d20 = 17"), dice(9, 8).quantity("2d20"));
    assertEquals(new RolledQuantity(9, "6+3 CD = 9"), dice(1, 2, 3).quantity("6+3 CD"));
    assertEquals(new RolledQuantity(140, "12+6 CD x10 = 140"), dice(2, 3, 3, 3, 3, 3).quantity("12+6 CD x10"));
  }

  @Test
  void anUnknownFormulaIsAnError() {
    assertThrows(IllegalArgumentException.class, () -> dice().quantity("a few"));
  }

  @Test
  void everyFormulaInTheCatalogsParses() {
    Dice dice = new Dice(RandomGenerator.getDefault());
    for (LootTable table : LootCatalog.all()) {
      for (LootRow row : table.rows()) {
        if (row.quantity() != null) {
          assertDoesNotThrow(() -> dice.quantity(row.quantity()), row.name());
        }
      }
    }
  }
}
