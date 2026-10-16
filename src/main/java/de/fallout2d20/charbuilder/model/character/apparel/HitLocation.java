package de.fallout2d20.charbuilder.model.character.apparel;

import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * A hit location: the six body parts of a humanoid (CRB p. 28) and the locations of a Mister Handy
 * (CRB p. 146), each with its own damage resistance.
 */
public enum HitLocation {
  HEAD("Head", "1–2"),
  TORSO("Torso", "3–8"),
  LEFT_ARM("Left Arm", "9–11"),
  RIGHT_ARM("Right Arm", "12–14"),
  LEFT_LEG("Left Leg", "15–17"),
  RIGHT_LEG("Right Leg", "18–20"),
  OPTICS("Optics", "1–2"),
  MAIN_BODY("Main Body", "3–8"),
  ARM_1("Arm 1", "9–11"),
  ARM_2("Arm 2", "12–14"),
  ARM_3("Arm 3", "15–17"),
  THRUSTER("Thruster", "18–20");

  private final @NonNull String displayName;
  private final @NonNull String dieRange;

  HitLocation(@NonNull String displayName, @NonNull String dieRange) {
    this.displayName = displayName;
    this.dieRange = dieRange;
  }

  /** The d20 results that hit this location (CRB p. 28; Mister Handy CRB p. 54), e.g. "9–11". */
  public @NonNull String dieRange() {
    return dieRange;
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
