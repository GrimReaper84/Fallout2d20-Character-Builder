package de.fallout2d20.charbuilder.model.character.legendary;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorLocation;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

/** Rolling on the legendary tables (WG pp. 96–98, 104) with fixed d20 results. */
class LegendaryRollTest {

  /** d20 14 picks Table 2; Small Guns 7 is Powerful. */
  @Test
  void weaponTableAndColumn() {
    LegendaryRollResult<?> result = new LegendaryRoll(dice(14, 7)).weapon(LegendaryWeaponType.SMALL_GUNS);
    assertEquals("legendary-weapon-powerful", id(result));
    assertEquals("d20 14: Table 2 · d20 7", result.description());
  }

  /** P. 98: Melee 12–20 on Table 2 is "No Result – Roll Again"; 1 is Mighty. */
  @Test
  void noResultIsRolledAgain() {
    LegendaryRollResult<?> result = new LegendaryRoll(dice(20, 15, 1)).weapon(LegendaryWeaponType.MELEE);
    assertEquals("legendary-weapon-mighty", id(result));
    assertEquals("d20 20: Table 2 · d20 15: no result · d20 1", result.description());
  }

  /** Table 1 for 1–10: Energy Weapons 15 is Lucky. */
  @Test
  void tableOne() {
    assertEquals("legendary-weapon-lucky",
        id(new LegendaryRoll(dice(10, 15)).weapon(LegendaryWeaponType.ENERGY_WEAPONS)));
  }

  /** P. 104: the head column has nothing on 19–20 (rolled again); Head 2 is Cloaking, Leg 1 Acrobat's. */
  @Test
  void armor() {
    LegendaryRollResult<?> head = new LegendaryRoll(dice(19, 2)).armor(LegendaryArmorLocation.HEAD);
    assertEquals("legendary-armor-cloaking", id(head));
    assertEquals("Head · d20 19: no result · d20 2", head.description());
    assertEquals("legendary-armor-acrobats", id(new LegendaryRoll(dice(1)).armor(LegendaryArmorLocation.LEG)));
  }

  private static String id(LegendaryRollResult<?> result) {
    return switch (result.property()) {
      case de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponData weapon -> weapon.id();
      case de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorData armor -> armor.id();
      default -> throw new AssertionError(result);
    };
  }

  /** A generator returning these d20 results in order. */
  private static RandomGenerator dice(Integer... results) {
    Deque<Integer> queue = new ArrayDeque<>(List.of(results));
    return new RandomGenerator() {
      @Override
      public long nextLong() {
        throw new UnsupportedOperationException();
      }

      @Override
      public int nextInt(int bound) {
        return queue.removeFirst() - 1;
      }
    };
  }
}
