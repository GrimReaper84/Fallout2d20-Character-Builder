package de.fallout2d20.charbuilder.model.character.equipment;

import org.jspecify.annotations.NonNull;

/**
 * The caps a character starts with: those of its equipment pack (CRB pp. 76–80), the extra caps for a
 * starting level above 1 (CRB p. 81) and the d20 rolled for Tag skill items (Barter: 2d20, CRB p. 81).
 * The dice stay a formula (user decision: amounts are rolled at the table).
 *
 * @param packCaps caps of the equipment pack
 * @param levelCaps extra caps for the starting level
 * @param d20 number of d20 rolled for extra caps
 */
public record StartingCaps(int packCaps, long levelCaps, int d20) {

  public StartingCaps {
    if (packCaps < 0 || levelCaps < 0 || d20 < 0) {
      throw new IllegalArgumentException("Caps must not be negative");
    }
  }

  /** The caps without the dice. */
  public long fixedTotal() {
    return packCaps + levelCaps;
  }

  /** E.g. {@code 145 caps + 2d20}, {@code 2d20 caps}, {@code 45 caps}. */
  @Override
  public @NonNull String toString() {
    if (d20 == 0) {
      return fixedTotal() + " caps";
    }
    return fixedTotal() == 0 ? d20 + "d20 caps" : fixedTotal() + " caps + " + d20 + "d20";
  }
}
