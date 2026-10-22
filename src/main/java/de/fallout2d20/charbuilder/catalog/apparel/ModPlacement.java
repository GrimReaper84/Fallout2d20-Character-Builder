package de.fallout2d20.charbuilder.catalog.apparel;

import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/** Which armor pieces an Armor Upgrade fits (CRB p. 136: all locations, torso only, arms only, legs only). */
public enum ModPlacement {
  ALL("All locations"),
  TORSO("Torso only"),
  ARMS("Arms only"),
  LEGS("Legs only");

  private static final @NonNull Set<HitLocation> ARM_LOCATIONS = Set.of(HitLocation.LEFT_ARM, HitLocation.RIGHT_ARM);
  private static final @NonNull Set<HitLocation> LEG_LOCATIONS = Set.of(HitLocation.LEFT_LEG, HitLocation.RIGHT_LEG);

  private final @NonNull String displayName;

  ModPlacement(@NonNull String displayName) {
    this.displayName = displayName;
  }

  public @NonNull String displayName() {
    return displayName;
  }

  /**
   * Whether an upgrade of this placement fits a piece reaching these locations: torso upgrades a piece covering
   * the torso, arm and leg upgrades a piece covering nothing but arms or legs (a custom piece may cover several
   * locations; assumption confirmed by the user 2026-10-22).
   */
  public boolean fits(@NonNull Set<HitLocation> reach) {
    return switch (this) {
      case ALL -> !reach.isEmpty();
      case TORSO -> reach.contains(HitLocation.TORSO);
      case ARMS -> !reach.isEmpty() && ARM_LOCATIONS.containsAll(reach);
      case LEGS -> !reach.isEmpty() && LEG_LOCATIONS.containsAll(reach);
    };
  }
}
