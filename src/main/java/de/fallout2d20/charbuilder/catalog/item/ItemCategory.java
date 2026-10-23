package de.fallout2d20.charbuilder.catalog.item;

import org.jspecify.annotations.NonNull;

/**
 * The tables an item comes from (CRB pp. 149–187; WG pp. 108, 120–125: rare books, junk); magazines are a catalog of
 * their own.
 */
public enum ItemCategory {
  FOOD("Food"),
  BEVERAGE("Beverage"),
  CHEM("Chem"),
  OTHER_CONSUMABLE("Other consumable"),
  TOOL("Tool"),
  ROBOT_MODULE("Robot module"),
  RARE_BOOK("Rare book"),
  JUNK("Junk");

  private final @NonNull String displayName;

  ItemCategory(@NonNull String displayName) {
    this.displayName = displayName;
  }

  public @NonNull String displayName() {
    return displayName;
  }
}
