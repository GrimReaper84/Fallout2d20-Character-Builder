package de.fallout2d20.charbuilder.model.character;

import java.util.List;

/**
 * A perk taken by a character.
 *
 * <p>CRB p. 49: perks can have several ranks; each rank increases the effect.
 *
 * @param perk the perk
 * @param rank number of ranks taken (at least 1)
 * @param importedRanks ranks taken over from a legacy save; their effects on attributes and skills
 *     are already part of the {@link LegacyBaseline} and must not be applied again
 * @param attributeChoices one attribute per rank for perks such as Intense Training
 * @param skillChoices chosen skills for perks such as Tag! (empty for imported ranks)
 */
public record PerkSelection(
    CatalogRef perk,
    int rank,
    int importedRanks,
    List<Attribute> attributeChoices,
    List<Skill> skillChoices) {

  public PerkSelection {
    if (rank < 1) {
      throw new IllegalArgumentException("Perk rank must be at least 1");
    }
    if (importedRanks < 0 || importedRanks > rank) {
      throw new IllegalArgumentException("Imported ranks must lie between 0 and the rank");
    }
    attributeChoices = List.copyOf(attributeChoices);
    skillChoices = List.copyOf(skillChoices);
  }
}
