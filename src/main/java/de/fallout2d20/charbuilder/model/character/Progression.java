package de.fallout2d20.charbuilder.model.character;

/**
 * Advancement choices gained by levelling up but not yet spent.
 *
 * <p>CRB p. 49: each new level grants one skill rank and one perk.
 */
public record Progression(int pendingSkillRanks, int pendingPerks) {

  public static final Progression NONE = new Progression(0, 0);

  public Progression {
    if (pendingSkillRanks < 0 || pendingPerks < 0) {
      throw new IllegalArgumentException("Pending choices must not be negative");
    }
  }
}
