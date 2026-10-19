package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import org.jspecify.annotations.NonNull;

/** One item of the item tree with its list index and group. */
record ItemEntry(int index, @NonNull CarriedItem item, @NonNull ItemGroup group) implements ItemTreeNode {

  /** Name with the quantity in brackets, e.g. "Stimpak [2]" (user decision 2026-10-19: always shown). */
  @NonNull String label() {
    return item.displayName() + " [" + item.quantity() + "]";
  }
}
