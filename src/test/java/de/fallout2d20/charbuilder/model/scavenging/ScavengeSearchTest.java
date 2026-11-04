package de.fallout2d20.charbuilder.model.scavenging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.FixedDice;
import de.fallout2d20.charbuilder.catalog.loot.LootCategory;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class ScavengeSearchTest {

  /** The ruined house of the example, CRB p. 196. */
  private static final LocationItems HOUSE = new LocationItems(Map.of(
      LootCategory.CLOTHING, new ItemRange(1, 2),
      LootCategory.FOOD, new ItemRange(2, 4),
      LootCategory.BEVERAGES, new ItemRange(1, 3),
      LootCategory.JUNK, new ItemRange(2, 6),
      LootCategory.WEAPONS_RANGED, new ItemRange(0, 1),
      LootCategory.AMMUNITION, new ItemRange(0, 1),
      LootCategory.CHEMS, new ItemRange(0, 1)));

  private static ScavengeSearch search(int successes, int complications) {
    return new ScavengeSearch(new SearchSetup(HOUSE, LocationScale.AVERAGE, LocationDegree.PARTLY_SEARCHED, 3,
        JunkMode.COUNT, true), successes, complications, 0, new LootRoller(RandomGenerator.getDefault()));
  }

  /** CRB p. 196: difficulty 1, 30 minutes, three successes give 2 AP and the listed minimum rolls. */
  @Test
  void theBookExample() {
    ScavengeSearch search = search(3, 0);
    assertTrue(search.succeeded());
    assertEquals(1, search.difficulty());
    assertEquals(2, search.apLeft());
    assertEquals(Duration.ofMinutes(30), search.time());
    assertEquals(1, search.rolls(LootCategory.CLOTHING));
    assertEquals(2, search.rolls(LootCategory.FOOD));
    assertEquals(1, search.rolls(LootCategory.BEVERAGES));
    assertEquals(2, search.rolls(LootCategory.JUNK));
    assertEquals(0, search.rolls(LootCategory.WEAPONS_RANGED));
    assertEquals(6, search.finds().size());
    // He rolls once on the Weapon table and once on the Ammo table.
    search.addExtraRoll(LootCategory.WEAPONS_RANGED);
    search.addExtraRoll(LootCategory.AMMUNITION);
    assertEquals(0, search.apLeft());
    assertEquals(8, search.finds().size());
    assertEquals(FindOrigin.AP, search.finds().getLast().origin());
  }

  @Test
  void theMinimumRollsUseTheDiceInBookOrder() {
    ScavengeSearch search = new ScavengeSearch(new SearchSetup(
        new LocationItems(Map.of(LootCategory.CLOTHING, new ItemRange(1, 1))), LocationScale.TINY,
        LocationDegree.UNTOUCHED, 1, JunkMode.COUNT, false), 0, 0, 0, new LootRoller(FixedDice.of(14, 14)));
    assertEquals("Heavy Coat", search.finds().getFirst().name());
  }

  /** CRB p. 196: too few successes find nothing. */
  @Test
  void failure() {
    ScavengeSearch search = search(0, 0);
    assertFalse(search.succeeded());
    assertTrue(search.finds().isEmpty());
    assertEquals(0, search.apLeft());
  }

  /** CRB p. 196: another attempt takes the listed time, whatever complications the last one had. */
  @Test
  void aRetryTakesTheScaleTime() {
    ScavengeSearch search = search(0, 1);
    assertEquals(Duration.ofMinutes(45), search.time());
    assertEquals(Duration.ofMinutes(30), search.retryTime());
  }

  /** CRB p. 19: the group pool adds to the AP of the test, so a Min 0 category can be rolled without extra successes. */
  @Test
  void groupApBuyExtraRolls() {
    ScavengeSearch search = new ScavengeSearch(new SearchSetup(HOUSE, LocationScale.AVERAGE,
        LocationDegree.PARTLY_SEARCHED, 3, JunkMode.COUNT, false), 1, 0, 2,
        new LootRoller(RandomGenerator.getDefault()));
    assertEquals(0, search.generatedAp());
    assertEquals(2, search.apLeft());
    assertEquals(0, search.rolls(LootCategory.CHEMS));
    search.addExtraRoll(LootCategory.CHEMS);
    assertEquals(1, search.rolls(LootCategory.CHEMS));
    assertEquals(1, search.apLeft());
  }

  /** CRB p. 196: a failed search finds nothing, so the group's AP have nothing to buy. */
  @Test
  void aFailedSearchHasNoAp() {
    ScavengeSearch search = new ScavengeSearch(new SearchSetup(HOUSE, LocationScale.AVERAGE,
        LocationDegree.MOSTLY_SEARCHED, 3, JunkMode.COUNT, false), 1, 0, 4,
        new LootRoller(RandomGenerator.getDefault()));
    assertEquals(0, search.apLeft());
    assertEquals(0, search.generatedAp());
  }

  @Test
  void negativeGroupApIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new ScavengeSearch(new SearchSetup(HOUSE, LocationScale.TINY,
        LocationDegree.UNTOUCHED, 1, JunkMode.COUNT, false), 0, 0, -1,
        new LootRoller(RandomGenerator.getDefault())));
  }

  /** CRB p. 196: difficulty 0 needs no success. */
  @Test
  void untouchedNeedsNoSuccess() {
    ScavengeSearch search = new ScavengeSearch(new SearchSetup(HOUSE, LocationScale.SMALL, LocationDegree.UNTOUCHED, 1,
        JunkMode.COUNT, false), 0, 0, 0, new LootRoller(RandomGenerator.getDefault()));
    assertTrue(search.succeeded());
  }

  @Test
  void extraRollsStopAtZeroApAndAtTheMaximum() {
    ScavengeSearch search = search(5, 0);
    search.addExtraRoll(LootCategory.CLOTHING);
    assertEquals("The maximum of 2 is reached", search.extraRollBlock(LootCategory.CLOTHING));
    assertThrows(IllegalStateException.class, () -> search.addExtraRoll(LootCategory.CLOTHING));
    assertEquals(3, search.apLeft());
    search.addExtraRoll(LootCategory.FOOD);
    search.addExtraRoll(LootCategory.FOOD);
    search.addExtraRoll(LootCategory.JUNK);
    assertEquals("No AP left", search.extraRollBlock(LootCategory.JUNK));
    assertThrows(IllegalStateException.class, () -> search.addExtraRoll(LootCategory.JUNK));
  }

  @Test
  void freeRollsIgnoreApAndMaximum() {
    ScavengeSearch search = search(1, 0);
    search.addFreeRoll(LootCategory.CLOTHING);
    search.addFreeRoll(LootCategory.CLOTHING);
    assertEquals(0, search.apLeft());
    assertEquals(1, search.rolls(LootCategory.CLOTHING));
    assertEquals(FindOrigin.FREE, search.finds().getLast().origin());
  }

  @Test
  void rollToMaximumFillsEveryCategoryWithoutAp() {
    ScavengeSearch search = search(1, 0);
    search.rollToMaximum();
    assertEquals(2 + 4 + 3 + 6 + 1 + 1 + 1, search.finds().size());
    assertEquals(0, search.apLeft());
    assertEquals(FindOrigin.TO_MAXIMUM, search.finds().getLast().origin());
    assertNotNull(search.extraRollBlock(LootCategory.CHEMS));
  }

  /** CRB p. 196: 2 AP halve the time. */
  @Test
  void halvingTheTime() {
    ScavengeSearch search = search(3, 0);
    assertNull(search.halveTimeBlock());
    search.halveTime();
    assertEquals(Duration.ofMinutes(15), search.time());
    assertEquals(0, search.apLeft());
    assertEquals("The time is already halved", search.halveTimeBlock());
    assertThrows(IllegalStateException.class, search::halveTime);
    assertEquals("Needs 2 AP", search(2, 0).halveTimeBlock());
  }

  /** CRB p. 197: a complication adds half the original time. */
  @Test
  void complicationsAddHalfTheOriginalTime() {
    assertEquals(Duration.ofMinutes(60), search(3, 2).time());
    ScavengeSearch halved = search(3, 1);
    halved.halveTime();
    assertEquals(Duration.ofSeconds(22 * 60 + 30), halved.time());
    assertEquals("22 minutes 30 seconds", SearchTimes.text(halved.time()));
    assertEquals("1 hour", SearchTimes.text(search(3, 2).time()));
  }

  private static ScavengeSearch oneOddityAndJunk(JunkMode mode, boolean rareBooks, Integer... dice) {
    return new ScavengeSearch(new SearchSetup(new LocationItems(Map.of(
        LootCategory.JUNK, new ItemRange(1, 1), LootCategory.ODDITIES, new ItemRange(1, 1))),
        LocationScale.TINY, LocationDegree.UNTOUCHED, 2, mode, rareBooks), 0, 0, 0, new LootRoller(FixedDice.of(dice)));
  }

  @Test
  void luckIsCountedButTheJunkAdjustmentIsNot() {
    ScavengeSearch search = oneOddityAndJunk(JunkMode.DETAILED, true, 5, 5, 5, 3, 5, 3);
    assertEquals("Backwoodsman", search.finds().getFirst().name());
    assertEquals("Ashtray", search.finds().getLast().name());
    assertEquals(FindAction.ADJUST_JUNK, search.findAction(1));
    search.shift(1, 4);
    assertEquals("Bread Box", search.finds().getLast().name());
    assertEquals(0, search.luckSpent());
    assertEquals(FindAction.SHIFT_WITH_LUCK, search.findAction(0));
    search.shift(0, 14);
    assertEquals("Doctor’s Bag", search.finds().getFirst().name());
    assertEquals(1, search.luckSpent());
  }

  /** The reach is the level on item tables (CRB p. 200) and the LCK score on junk tables (WG p. 120). */
  @Test
  void theReachDependsOnTheTable() {
    ScavengeSearch search = oneOddityAndJunk(JunkMode.DETAILED, true, 5, 5, 5, 3, 5, 3);
    assertEquals(List.of(13, 14, 16, 17), search.shiftOptions(0, 9).stream().map(LootOption::roll).toList());
    assertEquals(List.of(2, 4), search.shiftOptions(1, 1).stream().map(LootOption::roll).toList());
  }

  @Test
  void aFindChangesOnlyOnce() {
    ScavengeSearch search = oneOddityAndJunk(JunkMode.COUNT, true, 5, 5, 5, 3, 7, 9);
    assertEquals(FindAction.ADD_LUCK_TO_COUNT, search.findAction(1));
    search.spendLuckOnJunkCount(1, 5);
    assertEquals(21, search.finds().getLast().quantity());
    assertEquals(1, search.luckSpent());
    assertEquals("This find was already changed", search.findActionBlock(1));
    assertThrows(IllegalStateException.class, () -> search.spendLuckOnJunkCount(1, 5));
    assertThrows(IllegalStateException.class, () -> search.spendLuckOnJunkCount(0, 5));
    assertThrows(IllegalStateException.class, () -> search.shift(1, 3));
    search.shift(0, 14);
    assertThrows(IllegalStateException.class, () -> search.shift(0, 15));
    assertThrows(IllegalStateException.class, () -> search.spendLuckOnRareBook(0));
    assertEquals(2, search.luckSpent());
  }

  /** WG p. 108: Rare Books only with the Wanderer's Guide. */
  @Test
  void rareBooksNeedTheWanderersGuide() {
    ScavengeSearch without = oneOddityAndJunk(JunkMode.COUNT, false, 5, 5, 5, 3, 7, 9);
    assertEquals("Rare Books need the Wanderer's Guide", without.rareBookBlock(0));
    assertThrows(IllegalStateException.class, () -> without.spendLuckOnRareBook(0));
    ScavengeSearch with = oneOddityAndJunk(JunkMode.COUNT, true, 5, 5, 5, 3, 7, 9, 1);
    assertNull(with.rareBookBlock(0));
    with.spendLuckOnRareBook(0);
    assertEquals("101 Heroic Last Stands", with.finds().getFirst().name());
    assertEquals(1, with.luckSpent());
  }
}
