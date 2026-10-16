package de.fallout2d20.charbuilder.model.character;

/**
 * Stored parts of a skill rank. The effective rank is computed by {@link Character#skillRank}.
 *
 * <p>CRB p. 58: a Tag skill starts at rank 2, so the two free ranks are not stored here but derived
 * from {@code tagged}.
 *
 * @param tagged whether the skill is a Tag skill
 * @param creationRanks ranks bought with skill points during character creation
 * @param advancementRanks ranks gained by levelling up after creation and applied
 * @param stagedRanks ranks gained by levelling up that are chosen but not applied yet; they count
 *     like applied ranks, but can still be taken back (see {@link Character#applyAdvancement()})
 */
public record SkillState(boolean tagged, int creationRanks, int advancementRanks, int stagedRanks) {

  public static final SkillState UNTRAINED = new SkillState(false, 0, 0, 0);

  public SkillState {
    if (creationRanks < 0 || advancementRanks < 0 || stagedRanks < 0) {
      throw new IllegalArgumentException("Skill ranks must not be negative");
    }
  }

  /** A skill state without staged ranks. */
  public SkillState(boolean tagged, int creationRanks, int advancementRanks) {
    this(tagged, creationRanks, advancementRanks, 0);
  }
}
