package de.fallout2d20.charbuilder.model.character;

import org.jspecify.annotations.NonNull;

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
 * @param gmRanks correction by the GM in free edit (D2), may be negative; the effective rank never drops
 *     below 0
 */
public record SkillState(boolean tagged, int creationRanks, int advancementRanks, int stagedRanks, int gmRanks) {

  public static final SkillState UNTRAINED = new SkillState(false, 0, 0, 0, 0);

  public SkillState {
    if (creationRanks < 0 || advancementRanks < 0 || stagedRanks < 0) {
      throw new IllegalArgumentException("Skill ranks must not be negative");
    }
  }

  /** A skill state without GM correction. */
  public SkillState(boolean tagged, int creationRanks, int advancementRanks, int stagedRanks) {
    this(tagged, creationRanks, advancementRanks, stagedRanks, 0);
  }

  /** A skill state without staged ranks and GM correction. */
  public SkillState(boolean tagged, int creationRanks, int advancementRanks) {
    this(tagged, creationRanks, advancementRanks, 0, 0);
  }

  /** The state with other Tag and creation ranks. */
  public @NonNull SkillState withCreation(boolean newTagged, int newCreationRanks) {
    return new SkillState(newTagged, newCreationRanks, advancementRanks, stagedRanks, gmRanks);
  }

  /** The state with other applied and staged advancement ranks. */
  public @NonNull SkillState withAdvancement(int newAdvancementRanks, int newStagedRanks) {
    return new SkillState(tagged, creationRanks, newAdvancementRanks, newStagedRanks, gmRanks);
  }

  /** The state with another GM correction (D2). */
  public @NonNull SkillState withGmRanks(int newGmRanks) {
    return new SkillState(tagged, creationRanks, advancementRanks, stagedRanks, newGmRanks);
  }
}
