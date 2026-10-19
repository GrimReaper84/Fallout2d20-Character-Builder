package de.fallout2d20.charbuilder.model.character.equipment;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import org.jspecify.annotations.NonNull;

/**
 * Where an inventory entry made from the starting equipment comes from (CRB p. 50, step 6): the equipment
 * pack with its choices, arms and trinket (CRB pp. 76–80) or the items of the Tag skills (CRB p. 81).
 * Marked entries are replaced when that part of the starting equipment changes during creation
 * ({@link PlayerCharacter#applyStartingEquipment(StartingEquipment)}); entries added by
 * hand carry no marker and are never touched.
 */
public enum StartingSource {
  PACK("From the equipment pack", "[Pack] Roll: "),
  TAG_SKILL("From the Tag skill items", "[Tag skill] Roll: ");

  private final @NonNull String description;
  private final @NonNull String rollPrefix;

  StartingSource(@NonNull String description, @NonNull String rollPrefix) {
    this.description = description;
    this.rollPrefix = rollPrefix;
  }

  /** E.g. {@code From the equipment pack}, for tooltips. */
  public @NonNull String description() {
    return description;
  }

  /** Start of the gear-notes lines with the amounts of this source to roll ({@link StartingNotes}). */
  public @NonNull String rollPrefix() {
    return rollPrefix;
  }
}
