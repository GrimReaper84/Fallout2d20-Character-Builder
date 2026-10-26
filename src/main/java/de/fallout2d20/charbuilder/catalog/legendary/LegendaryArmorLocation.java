package de.fallout2d20.charbuilder.catalog.legendary;

import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import org.jspecify.annotations.NonNull;

/**
 * The columns of the Legendary Armor table (WG p. 104). A robot's locations use the column of the humanoid location
 * they map to (as robot armor on a Protectron, {@code Coverage#locationsOn}): Optics the head, Main Body the torso,
 * an arm an arm, the Thruster the legs.
 */
public enum LegendaryArmorLocation {
  HEAD("Head"),
  ARM("Arm"),
  TORSO("Torso"),
  LEG("Leg");

  private final @NonNull String displayName;

  LegendaryArmorLocation(@NonNull String displayName) {
    this.displayName = displayName;
  }

  /** Column title as printed. */
  public @NonNull String displayName() {
    return displayName;
  }

  /** The column for a hit location. */
  public static @NonNull LegendaryArmorLocation of(@NonNull HitLocation location) {
    return switch (location) {
      case HEAD, OPTICS -> HEAD;
      case TORSO, MAIN_BODY -> TORSO;
      case LEFT_ARM, RIGHT_ARM, ARM_1, ARM_2, ARM_3 -> ARM;
      case LEFT_LEG, RIGHT_LEG, THRUSTER -> LEG;
    };
  }
}
