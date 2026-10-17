package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import org.jspecify.annotations.NonNull;

/**
 * The groups of the apparel tree, by where a piece sits on the wearer's body: the humanoid locations (CRB
 * p. 28), those of a Mister Handy (CRB p. 54), Clothing for clothing, which can be worn under armor, Outfit
 * for the other pieces over several groups (outfits and full-body armor, which cannot, CRB p. 124, 130; user
 * decision 2026-10-17) and Plating for robot plating, which covers every location (CRB p. 146). A tree
 * shows only the groups that hold pieces, in this order.
 */
enum ApparelGroup implements ApparelTreeNode {
  HEAD("Head"),
  OPTICS("Optics"),
  TORSO("Torso"),
  MAIN_BODY("Main Body"),
  ARMS("Arms"),
  LEGS("Legs"),
  THRUSTER("Thruster"),
  CLOTHING("Clothing"),
  OUTFIT("Outfit"),
  PLATING("Plating");

  private final @NonNull String displayName;

  ApparelGroup(@NonNull String displayName) {
    this.displayName = displayName;
  }

  @NonNull String displayName() {
    return displayName;
  }

  /** The group of a single hit location. */
  static @NonNull ApparelGroup of(@NonNull HitLocation location) {
    return switch (location) {
      case HEAD -> HEAD;
      case OPTICS -> OPTICS;
      case TORSO -> TORSO;
      case MAIN_BODY -> MAIN_BODY;
      case LEFT_ARM, RIGHT_ARM, ARM_1, ARM_2, ARM_3 -> ARMS;
      case LEFT_LEG, RIGHT_LEG -> LEGS;
      case THRUSTER -> THRUSTER;
    };
  }
}
