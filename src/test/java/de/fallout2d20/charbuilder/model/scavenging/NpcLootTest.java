package de.fallout2d20.charbuilder.model.scavenging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.fallout2d20.charbuilder.FixedDice;
import de.fallout2d20.charbuilder.catalog.loot.LootRow;
import java.util.List;
import org.junit.jupiter.api.Test;

/** CRB p. 335. */
class NpcLootTest {

  private static LootRow ammo(String name) {
    return NpcLoot.ammunitionRows().stream().filter(row -> row.name().equals(name)).findFirst().orElseThrow();
  }

  @Test
  void capsAreTheWealthRatingInD20s() {
    List<Find> finds = new NpcLoot(FixedDice.of(4, 5, 6, 3)).loot(1, 3, null, false);
    assertEquals("15 Caps", finds.getFirst().text());
    assertEquals(2, finds.size());
  }

  @Test
  void junkIsOneItemPerEffect() {
    assertEquals("Junk items, GM chooses: 1", new NpcLoot(FixedDice.of(5)).loot(1, 0, null, false).getLast().text());
    assertEquals("Junk items, GM chooses: 0", new NpcLoot(FixedDice.of(4)).loot(1, 0, null, false).getLast().text());
  }

  @Test
  void prolificScavengersRollTwoDice() {
    assertEquals(2, new NpcLoot(FixedDice.of(5, 6)).loot(1, 0, null, true).getLast().quantity());
  }

  @Test
  void ammunitionHasTheQuantityOfTheLootTable() {
    Find ammo = new NpcLoot(FixedDice.of(1, 2, 3, 3, 3)).loot(1, 0, ammo("10mm"), false).getFirst();
    assertEquals("11 × 10mm", ammo.text());
  }

  @Test
  void severalNpcsAddUp() {
    List<Find> finds = new NpcLoot(FixedDice.of(
        // caps: 2 NPCs with wealth 2
        1, 2, 3, 4,
        // ammunition per NPC: .44 Magnum 4+2 CD
        1, 1, 2, 2,
        // junk: one die per NPC
        5, 6)).loot(2, 2, ammo(".44 Magnum"), false);
    assertEquals("10 Caps", finds.get(0).text());
    assertEquals("14 × .44 Magnum", finds.get(1).text());
    assertEquals("Junk items, GM chooses: 2", finds.get(2).text());
  }

  @Test
  void everyAmmunitionTypeIsOfferedOnce() {
    assertEquals(20, NpcLoot.ammunitionRows().size());
  }

  @Test
  void invalidInputIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new NpcLoot(FixedDice.of()).loot(0, 1, null, false));
    assertThrows(IllegalArgumentException.class, () -> new NpcLoot(FixedDice.of()).loot(1, -1, null, false));
  }
}
