package de.fallout2d20.charbuilder.catalog.apparel;

import org.jspecify.annotations.NonNull;

/**
 * The kind of a worn item, which decides how it combines with others: clothing and headgear can be worn
 * under armor, outfits cannot (CRB p. 124, 130); robot plating covers a whole Mister Handy and robot armor
 * adds to it (CRB p. 146).
 */
public enum ApparelKind {
  CLOTHING("Clothing"),
  OUTFIT("Outfit"),
  HEADGEAR("Headgear"),
  ARMOR("Armor"),
  ROBOT_PLATING("Robot plating"),
  ROBOT_ARMOR("Robot armor");

  private final @NonNull String displayName;

  ApparelKind(@NonNull String displayName) {
    this.displayName = displayName;
  }

  public @NonNull String displayName() {
    return displayName;
  }

  /** Whether the item is made for a Mister Handy rather than a humanoid. */
  public boolean isRobot() {
    return this == ROBOT_PLATING || this == ROBOT_ARMOR;
  }
}
