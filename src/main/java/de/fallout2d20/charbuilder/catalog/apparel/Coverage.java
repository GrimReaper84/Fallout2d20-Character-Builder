package de.fallout2d20.charbuilder.catalog.apparel;

import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * The "Locations Covered" of an apparel table row (CRB p. 124, 130, 146). {@code ARM}, {@code LEG} and
 * {@code ROBOT_ARM} cover one limb, chosen when the piece is worn (on a humanoid robot also {@code THRUSTER},
 * {@link #isChosenOn}); the others cover fixed locations.
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
  /** Always an immutable list ({@code List.of}), so the enum stays immutable. */
  @SuppressWarnings("ImmutableEnumChecker")
  private final @NonNull List<HitLocation> locations;

  Coverage(@NonNull String displayName, @NonNull List<HitLocation> locations) {
    this.displayName = displayName;
    this.locations = locations;
  }

  /** As printed in the "Locations Covered" column, e.g. {@code Arms}. */
  public @NonNull String displayName() {
    return displayName;
  }

  /**
   * Whether one location is chosen when the piece is worn on a body with these hit locations: a single arm or leg
   * (ARM, LEG, ROBOT_ARM), and a robot leg piece (Thruster) on a humanoid robot (Assaultron, Protectron), which covers
   * one chosen leg (user decision 2026-10-26, after WG p. 18, where an Assaultron pack lets the player pick the arm and
   * the leg; it replaces the 2026-10-16 rule of both legs).
   */
  public boolean isChosenOn(@NonNull List<HitLocation> body) {
    return this == ARM || this == LEG || this == ROBOT_ARM
        || (this == THRUSTER && !body.contains(HitLocation.THRUSTER));
  }

  /** The covered locations; for a chosen coverage the locations to choose from. */
  public @NonNull List<HitLocation> locations() {
    return locations;
  }

  /**
   * The covered locations on a body with these hit locations. Robot armor on a humanoid robot (a Protectron,
   * WOA p. 20, or an Assaultron, WG p. 10, whose hit locations the books do not define) is mapped by house rule
   * (user decisions 2026-10-16 and 2026-10-26): Optics to the head, Main Body to the torso, an Arm to one chosen arm,
   * the Thruster to one chosen leg (both legs are the choice, {@link #isChosenOn}), All to every location. Otherwise
   * as {@link #locations()}.
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
