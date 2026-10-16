package de.fallout2d20.charbuilder.model.character.apparel;

import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * A hit location: the six body parts of a humanoid (CRB p. 28) and the locations of a Mister Handy
 * (CRB p. 146), each with its own damage resistance.
 */
public enum HitLocation {
  HEAD("Head"),
  TORSO("Torso"),
  LEFT_ARM("Left Arm"),
  RIGHT_ARM("Right Arm"),
  LEFT_LEG("Left Leg"),
  RIGHT_LEG("Right Leg"),
  OPTICS("Optics"),
  MAIN_BODY("Main Body"),
  ARM_1("Arm 1"),
  ARM_2("Arm 2"),
  ARM_3("Arm 3"),
  THRUSTER("Thruster");

  private final @NonNull String displayName;

  HitLocation(@NonNull String displayName) {
    this.displayName = displayName;
  }

  /** Name as printed, also the key of the legacy {@code armor} map. */
  public @NonNull String displayName() {
    return displayName;
  }

  /** CRB p. 28: head, torso, left and right arm, left and right leg. */
  public static @NonNull List<HitLocation> humanoid() {
    return List.of(HEAD, TORSO, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG);
  }

  /** CRB p. 146: optics, main body, three arms and the thruster. */
  public static @NonNull List<HitLocation> misterHandy() {
    return List.of(OPTICS, MAIN_BODY, ARM_1, ARM_2, ARM_3, THRUSTER);
  }
}
