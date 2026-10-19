package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.item.ItemCategory;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import org.jspecify.annotations.NonNull;

/**
 * The groups of the item tree, by item type (user decision 2026-10-19): the item tables of the book
 * (CRB pp. 149–187), magazines, holotapes, and Custom for items outside the catalog (or a catalog id this
 * version does not know). A tree shows only the groups that hold items, in this order.
 */
enum ItemGroup implements ItemTreeNode {
  FOOD("Food"),
  BEVERAGE("Beverage"),
  CHEM("Chem"),
  OTHER_CONSUMABLE("Other consumable"),
  TOOL("Tool"),
  ROBOT_MODULE("Robot module"),
  MAGAZINES("Magazines"),
  HOLOTAPES("Holotapes"),
  CUSTOM("Custom");

  private final @NonNull String displayName;

  ItemGroup(@NonNull String displayName) {
    this.displayName = displayName;
  }

  @NonNull String displayName() {
    return displayName;
  }

  /** The group of an item. */
  static @NonNull ItemGroup of(@NonNull CarriedItem item) {
    if (item.holotape() != null) {
      return HOLOTAPES;
    }
    if (item.magazine().isPresent()) {
      return MAGAZINES;
    }
    return item.data().map(data -> of(data.category())).orElse(CUSTOM);
  }

  private static @NonNull ItemGroup of(@NonNull ItemCategory category) {
    return switch (category) {
      case FOOD -> FOOD;
      case BEVERAGE -> BEVERAGE;
      case CHEM -> CHEM;
      case OTHER_CONSUMABLE -> OTHER_CONSUMABLE;
      case TOOL -> TOOL;
      case ROBOT_MODULE -> ROBOT_MODULE;
    };
  }
}
