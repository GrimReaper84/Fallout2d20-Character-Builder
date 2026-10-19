package de.fallout2d20.charbuilder.model.character;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * A perk taken by a character.
 *
 * <p>CRB p. 49: perks can have several ranks; each rank increases the effect.
 *
 * @param perk the perk
 * @param rank number of ranks taken (at least 1), staged ranks included
 * @param importedRanks ranks taken over from a legacy save; their effects on attributes and skills
 *     are already part of the {@link LegacyBaseline} and must not be applied again
 * @param attributeChoices one attribute per rank for perks such as Intense Training
 * @param skillChoices chosen skills for perks such as Tag! (empty for imported ranks)
 * @param stagedRanks the last ranks, taken after a level-up but not applied yet (see
 *     {@link PlayerCharacter#applyAdvancement()}); they count like other ranks but can be taken back
 */
public record PerkSelection(
    @NonNull CatalogRef perk,
    int rank,
    int importedRanks,
    @NonNull List<Attribute> attributeChoices,
    @NonNull List<Skill> skillChoices,
    int stagedRanks) {

  public PerkSelection {
    if (rank < 1) {
      throw new IllegalArgumentException("Perk rank must be at least 1");
    }
    if (importedRanks < 0 || stagedRanks < 0 || importedRanks + stagedRanks > rank) {
      throw new IllegalArgumentException("Imported and staged ranks must lie between 0 and the rank");
    }
    attributeChoices = List.copyOf(attributeChoices);
    skillChoices = List.copyOf(skillChoices);
  }

  /** Whether the rank given back next (the last one) is imported from a legacy save. */
  public boolean lastRankImported() {
    return stagedRanks == 0 && rank <= importedRanks;
  }

  /** A perk selection without staged ranks. */
  public PerkSelection(@NonNull CatalogRef perk, int rank, int importedRanks,
      @NonNull List<Attribute> attributeChoices, @NonNull List<Skill> skillChoices) {
    this(perk, rank, importedRanks, attributeChoices, skillChoices, 0);
  }

  /** The attributes (abbreviated) and skills chosen for the perk's ranks, e.g. "AGI, STR" or "Sneak". */
  public @NonNull String choiceText() {
    List<String> names = new ArrayList<>();
    attributeChoices.forEach(attribute -> names.add(attribute.abbreviation()));
    skillChoices.forEach(skill -> names.add(skill.displayName()));
    return String.join(", ", names);
  }
}
