package de.fallout2d20.charbuilder.catalog.apparel;

import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * The "Locations Covered" of an apparel table row (CRB p. 124, 130, 146). {@code ARM}, {@code LEG} and
 * {@code ROBOT_ARM} cover one limb, chosen when the piece is worn; the others cover fixed locations.
 */
public enum Coverage {
  HEAD("Head", List.of(HitLocation.HEAD)),
  TORSO("Torso", List.of(HitLocation.TORSO)),
  ARMS("Arms", List.of(HitLocation.LEFT_ARM, HitLocation.RIGHT_ARM)),
  LEGS("Legs", List.of(HitLocation.LEFT_LEG, HitLocation.RIGHT_LEG)),
  ARM("Arm", List.of(HitLocation.LEFT_ARM, HitLocation.RIGHT_ARM)),
  LEG("Leg", List.of(HitLocation.LEFT_LEG, HitLocation.RIGHT_LEG)),
  OPTICS("Optics", List.of(HitLocation.OPTICS)),
  MAIN_BODY("Main Body", List.of(HitLocation.MAIN_BODY)),
  ROBOT_ARM("Arm", List.of(HitLocation.ARM_1, HitLocation.ARM_2, HitLocation.ARM_3)),
  THRUSTER("Thruster", List.of(HitLocation.THRUSTER)),
  ALL("All", HitLocation.misterHandy());

  private final @NonNull String displayName;
  private final @NonNull List<HitLocation> locations;

  Coverage(@NonNull String displayName, @NonNull List<HitLocation> locations) {
    this.displayName = displayName;
    this.locations = locations;
  }

  /** As printed in the "Locations Covered" column, e.g. {@code Arms}. */
  public @NonNull String displayName() {
    return displayName;
  }

  /** Whether one of {@link #locations()} is chosen when worn (a single arm or leg). */
  public boolean isChosen() {
    return this == ARM || this == LEG || this == ROBOT_ARM;
  }

  /** The covered locations; for a chosen coverage the locations to choose from. */
  public @NonNull List<HitLocation> locations() {
    return locations;
  }

  /**
   * The covered locations on a body with these hit locations. Robot armor on a humanoid robot (a Protectron,
   * WOA p. 20, whose hit locations the book does not define) is mapped by house rule (user decision
   * 2026-10-16): Optics to the head, Main Body to the torso, an Arm to one chosen arm, the Thruster to both
   * legs, All to every location. Otherwise as {@link #locations()}.
   */
  public @NonNull List<HitLocation> locationsOn(@NonNull List<HitLocation> body) {
    if (body.containsAll(locations)) {
      return locations;
    }
    return switch (this) {
      case OPTICS -> List.of(HitLocation.HEAD);
      case MAIN_BODY -> List.of(HitLocation.TORSO);
      case ROBOT_ARM -> List.of(HitLocation.LEFT_ARM, HitLocation.RIGHT_ARM);
      case THRUSTER -> List.of(HitLocation.LEFT_LEG, HitLocation.RIGHT_LEG);
      case ALL -> body;
      default -> locations;
    };
  }
}
