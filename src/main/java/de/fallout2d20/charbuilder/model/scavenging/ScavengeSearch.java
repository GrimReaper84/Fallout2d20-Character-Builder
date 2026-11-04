package de.fallout2d20.charbuilder.model.scavenging;

import de.fallout2d20.charbuilder.catalog.loot.LootCategory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One search of a location (CRB p. 196). The PER + Survival test against the degree's difficulty decides: on a
 * failure nothing is found and another attempt costs the time again; on a success every category gets its minimum
 * rolls, and the AP (successes above the difficulty, plus what the group adds from its pool, CRB p. 19) buy extra rolls
 * up to each maximum at 1 AP each, or halve the time for 2 AP. A complication the GM turns into lost time adds half the original time.
 *
 * <p>Beyond the book, for use at the table: free rolls (Can Do!, Pharma Farma, a GM gift) ignore AP and maximum, and
 * Roll to maximum fills every category without AP. Both are marked on the finds.
 */
public final class ScavengeSearch {

  private static final int HALVE_COST = 2;
  private static final int HALF = 2;

  private final @NonNull SearchSetup setup;
  private final int successes;
  private final int complications;
  private final int groupAp;
  private final @NonNull LootRoller roller;
  private final @NonNull List<Find> finds = new ArrayList<>();
  private int apLeft;
  private int luckSpent;
  private boolean timeHalved;

  /**
   * Makes the test and, on a success, the minimum rolls.
   *
   * @param successes the successes of the test
   * @param complications the complications the GM turned into lost time
   * @param groupAp the AP of the group pool available for this search
   */
  public ScavengeSearch(@NonNull SearchSetup setup, int successes, int complications, int groupAp,
      @NonNull LootRoller roller) {
    if (successes < 0 || complications < 0 || groupAp < 0) {
      throw new IllegalArgumentException("Successes, complications and group AP cannot be negative");
    }
    this.setup = setup;
    this.successes = successes;
    this.complications = complications;
    this.groupAp = groupAp;
    this.roller = roller;
    if (succeeded()) {
      apLeft = generatedAp() + groupAp;
      for (LootCategory category : setup.items().categories()) {
        for (int i = 0; i < setup.items().range(category).min(); i++) {
          finds.add(roll(category, FindOrigin.BASIC));
        }
      }
    }
  }

  public boolean succeeded() {
    return successes >= setup.degree().difficulty();
  }

  public int difficulty() {
    return setup.degree().difficulty();
  }

  public int successes() {
    return successes;
  }

  /** The AP of the test: the successes above the difficulty. */
  public int generatedAp() {
    return succeeded() ? successes - setup.degree().difficulty() : 0;
  }

  public int groupAp() {
    return groupAp;
  }

  public int apLeft() {
    return apLeft;
  }

  public int luckSpent() {
    return luckSpent;
  }

  public @NonNull List<LootCategory> categories() {
    return setup.items().categories();
  }

  public @NonNull List<Find> finds() {
    return List.copyOf(finds);
  }

  /** The rolls made that count towards the category's maximum; free rolls do not. */
  public int rolls(@NonNull LootCategory category) {
    return (int) finds.stream().filter(find -> find.category() == category && find.origin() != FindOrigin.FREE).count();
  }

  public int maximum(@NonNull LootCategory category) {
    return setup.items().range(category).max();
  }

  /** The time the search took: the scale's time, plus half of it per complication, halved if AP were spent on it. */
  public @NonNull Duration time() {
    Duration base = setup.scale().time();
    Duration time = base.plus(base.dividedBy(HALF).multipliedBy(complications));
    return timeHalved ? time.dividedBy(HALF) : time;
  }

  /** The time another attempt takes after a failure: the scale's time (CRB p. 196). */
  public @NonNull Duration retryTime() {
    return setup.scale().time();
  }

  public boolean timeHalved() {
    return timeHalved;
  }

  /** Why no AP roll is possible in the category; {@code null} if one is. */
  public @Nullable String extraRollBlock(@NonNull LootCategory category) {
    if (rolls(category) >= maximum(category)) {
      return "The maximum of " + maximum(category) + " is reached";
    }
    return apLeft < 1 ? "No AP left" : null;
  }

  /** Spends 1 AP on one more roll in the category. */
  public void addExtraRoll(@NonNull LootCategory category) {
    if (extraRollBlock(category) != null) {
      throw new IllegalStateException(extraRollBlock(category));
    }
    apLeft--;
    finds.add(roll(category, FindOrigin.AP));
  }

  /** One more roll in the category, outside AP and maximum. */
  public void addFreeRoll(@NonNull LootCategory category) {
    finds.add(roll(category, FindOrigin.FREE));
  }

  /** Fills every category up to its maximum, without AP. */
  public void rollToMaximum() {
    for (LootCategory category : setup.items().categories()) {
      while (rolls(category) < maximum(category)) {
        finds.add(roll(category, FindOrigin.TO_MAXIMUM));
      }
    }
  }

  /** Why the time cannot be halved; {@code null} if it can. */
  public @Nullable String halveTimeBlock() {
    if (timeHalved) {
      return "The time is already halved";
    }
    return apLeft < HALVE_COST ? "Needs 2 AP" : null;
  }

  /** Spends 2 AP to halve the time. */
  public void halveTime() {
    if (halveTimeBlock() != null) {
      throw new IllegalStateException(halveTimeBlock());
    }
    apLeft -= HALVE_COST;
    timeHalved = true;
  }

  /** The action that can change this find. */
  public @NonNull FindAction findAction(int index) {
    Find find = finds.get(index);
    if (find.category() != LootCategory.JUNK) {
      return FindAction.SHIFT_WITH_LUCK;
    }
    return find.tableId() == null ? FindAction.ADD_LUCK_TO_COUNT : FindAction.ADJUST_JUNK;
  }

  /** Why the find's result cannot be changed any more; {@code null} if it can. One change per find. */
  public @Nullable String findActionBlock(int index) {
    return finds.get(index).luckUsed() ? "This find was already changed" : null;
  }

  /** Why a magazine find cannot become a Rare Book; {@code null} if it can. */
  public @Nullable String rareBookBlock(int index) {
    if (!setup.rareBooks()) {
      return "Rare Books need the Wanderer's Guide";
    }
    return findActionBlock(index);
  }

  /**
   * The rows the find's roll can move to.
   *
   * @param luck the LCK score, the reach on a junk table; on other tables the location's level is the reach
   */
  public @NonNull List<LootOption> shiftOptions(int index, int luck) {
    return roller.options(finds.get(index), findAction(index) == FindAction.ADJUST_JUNK ? luck : setup.level());
  }

  /** Moves the find's roll to another row: for 1 Luck, on a junk table for free. */
  public void shift(int index, int roll) {
    FindAction action = findAction(index);
    if (action == FindAction.ADD_LUCK_TO_COUNT) {
      throw new IllegalStateException("A junk count has no rows to move to");
    }
    requireChangeable(index);
    finds.set(index, roller.shift(finds.get(index), roll));
    if (action == FindAction.SHIFT_WITH_LUCK) {
      luckSpent++;
    }
  }

  /** 1 Luck on a junk count: the find grows by the LCK score (CRB p. 208). */
  public void spendLuckOnJunkCount(int index, int luck) {
    if (findAction(index) != FindAction.ADD_LUCK_TO_COUNT) {
      throw new IllegalStateException("This find is no junk count");
    }
    requireChangeable(index);
    finds.set(index, roller.addLuck(finds.get(index), luck));
    luckSpent++;
  }

  /** 1 Luck on a magazine: a Rare Book instead (WG p. 108). */
  public void spendLuckOnRareBook(int index) {
    if (!finds.get(index).magazine() || rareBookBlock(index) != null) {
      throw new IllegalStateException("This find cannot become a Rare Book");
    }
    finds.set(index, roller.rareBook(finds.get(index)));
    luckSpent++;
  }

  private void requireChangeable(int index) {
    if (findActionBlock(index) != null) {
      throw new IllegalStateException(findActionBlock(index));
    }
  }

  private @NonNull Find roll(@NonNull LootCategory category, @NonNull FindOrigin origin) {
    return roller.roll(category, origin, setup.junkMode());
  }
}
