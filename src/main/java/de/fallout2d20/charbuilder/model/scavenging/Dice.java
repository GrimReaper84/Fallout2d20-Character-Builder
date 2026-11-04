package de.fallout2d20.charbuilder.model.scavenging;

import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

/**
 * The dice of the scavenging rules: d20 sums, Combat Dice and the quantity formulas of the tables. A Combat Die (CRB
 * p. 29) counts 1 and 2 on its faces 1 and 2, nothing on 3 and 4, and 1 plus an Effect on 5 and 6.
 */
public final class Dice {

  private static final int D20 = 20;
  private static final int D6 = 6;
  private static final int FIRST_EFFECT_FACE = 5;
  private static final int FIRST_BLANK_FACE = 3;
  private static final @NonNull Pattern FIXED = Pattern.compile("\\d+");
  private static final @NonNull Pattern D20_SUM = Pattern.compile("(\\d+)d20");
  private static final @NonNull Pattern COMBAT_DICE = Pattern.compile("(\\d+)\\+(\\d+) CD(?: x(\\d+))?");

  private final @NonNull RandomGenerator random;

  public Dice(@NonNull RandomGenerator random) {
    this.random = random;
  }

  public int d20() {
    return random.nextInt(D20) + 1;
  }

  /** The sum of this many d20. */
  public int d20s(int count) {
    int sum = 0;
    for (int i = 0; i < count; i++) {
      sum += d20();
    }
    return sum;
  }

  public @NonNull CombatRoll combatDice(int count) {
    int total = 0;
    int effects = 0;
    for (int i = 0; i < count; i++) {
      int face = random.nextInt(D6) + 1;
      if (face >= FIRST_EFFECT_FACE) {
        total++;
        effects++;
      } else if (face < FIRST_BLANK_FACE) {
        total += face;
      }
    }
    return new CombatRoll(total, effects);
  }

  /**
   * Rolls a quantity as the tables print it: {@code 1}, {@code 5d20}, {@code 6+3 CD} or {@code 12+6 CD x10}.
   *
   * @throws IllegalArgumentException for any other form, which is an error in the catalog
   */
  public @NonNull RolledQuantity quantity(@NonNull String formula) {
    if (FIXED.matcher(formula).matches()) {
      return new RolledQuantity(Integer.parseInt(formula), formula);
    }
    Matcher sum = D20_SUM.matcher(formula);
    if (sum.matches()) {
      int value = d20s(Integer.parseInt(sum.group(1)));
      return new RolledQuantity(value, formula + " = " + value);
    }
    Matcher combat = COMBAT_DICE.matcher(formula);
    if (combat.matches()) {
      int factor = combat.group(3) == null ? 1 : Integer.parseInt(combat.group(3));
      int value = (Integer.parseInt(combat.group(1)) + combatDice(Integer.parseInt(combat.group(2))).total()) * factor;
      return new RolledQuantity(value, formula + " = " + value);
    }
    throw new IllegalArgumentException("Unknown quantity formula: " + formula);
  }
}
