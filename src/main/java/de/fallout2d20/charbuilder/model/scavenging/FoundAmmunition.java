package de.fallout2d20.charbuilder.model.scavenging;

import de.fallout2d20.charbuilder.catalog.loot.LootRef;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The standard ammunition found with a ranged weapon (CRB p. 204). Part of the weapon's find, so it does not count
 * towards the maximum of the Ammunition category.
 *
 * @param quantity how many
 * @param name the ammunition as the loot table names it
 * @param ref its catalog entry
 */
public record FoundAmmunition(int quantity, @NonNull String name, @Nullable LootRef ref) {

  /** E.g. {@code with 9 × .308 Ammo}. */
  public @NonNull String text() {
    return "with " + quantity + " × " + name;
  }
}
