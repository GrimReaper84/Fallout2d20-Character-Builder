package de.fallout2d20.charbuilder.model.character;

/**
 * Stored parts of a skill rank. The effective rank is computed by {@link Character#skillRank}.
 *
 * <p>CRB p. 58: a Tag skill starts at rank 2, so the two free ranks are not stored here but derived
 * from {@code tagged}.
 *
 * @param tagged whether the skill is a Tag skill
 * @param creationRanks ranks bought with skill points during character creation
 * @param advancementRanks ranks gained by levelling up after creation
 */
public record SkillState(boolean tagged, int creationRanks, int advancementRanks) {

  public static final SkillState UNTRAINED = new SkillState(false, 0, 0);

  public SkillState {
    if (creationRanks < 0 || advancementRanks < 0) {
      throw new IllegalArgumentException("Skill ranks must not be negative");
    }
  }
}
