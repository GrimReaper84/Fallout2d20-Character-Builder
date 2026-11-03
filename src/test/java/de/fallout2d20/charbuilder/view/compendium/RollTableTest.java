package de.fallout2d20.charbuilder.view.compendium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.FixedDice;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentCatalog;
import org.junit.jupiter.api.Test;

class RollTableTest {

  @Test
  void aPlainTableHasTwentyRowsInRollOrder() {
    for (RollTable table : new RollTable[] {RollTable.TRINKETS, RollTable.OUTCAST_EQUIPMENT}) {
      assertEquals(20, table.rows().size());
      for (int i = 0; i < 20; i++) {
        assertEquals(i + 1, table.rows().get(i).roll());
      }
    }
    assertTrue(RollTable.LEGENDARY_WEAPON.rows().isEmpty());
  }

  @Test
  void rollingSelectsTheRowOfTheDie() {
    assertEquals(EquipmentCatalog.trinket(14), RollTable.TRINKETS.roll(FixedDice.of(14)));
    assertEquals(EquipmentCatalog.outcastEquipment(3), RollTable.OUTCAST_EQUIPMENT.roll(FixedDice.of(3)));
  }

  @Test
  void aLegendaryTableIsNotRolledAsPlainTable() {
    FixedDice dice = FixedDice.of(5);
    assertThrows(IllegalStateException.class, () -> RollTable.LEGENDARY_ARMOR.roll(dice));
  }
}
