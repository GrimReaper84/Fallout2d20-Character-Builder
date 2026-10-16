package de.fallout2d20.charbuilder.catalog.weapon;

import org.jspecify.annotations.NonNull;

/** The type of damage a weapon inflicts (CRB p. 89); resisted by the matching damage resistance. */
public enum DamageType {
  PHYSICAL("Physical"),
  ENERGY("Energy"),
  RADIATION("Radiation"),
  POISON("Poison");

  private final @NonNull String displayName;

  DamageType(@NonNull String displayName) {
    this.displayName = displayName;
  }

  public @NonNull String displayName() {
    return displayName;
  }
}
