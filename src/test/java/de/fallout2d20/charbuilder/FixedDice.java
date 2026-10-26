package de.fallout2d20.charbuilder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.random.RandomGenerator;

/** A random generator for tests whose dice show the given results in order (each {@code nextInt(bound)} is one die). */
public final class FixedDice implements RandomGenerator {

  private final Deque<Integer> results;

  private FixedDice(List<Integer> results) {
    this.results = new ArrayDeque<>(results);
  }

  /** Dice showing these results, e.g. {@code of(14, 7)} for two d20 rolls. */
  public static FixedDice of(Integer... results) {
    return new FixedDice(List.of(results));
  }

  @Override
  public int nextInt(int bound) {
    int result = results.removeFirst();
    if (result < 1 || result > bound) {
      throw new IllegalArgumentException("A die with " + bound + " sides cannot show " + result);
    }
    return result - 1;
  }

  @Override
  public long nextLong() {
    throw new UnsupportedOperationException("Only dice rolls are fixed");
  }
}
