package de.fallout2d20.charbuilder.model.character.item;

import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * The stored inventory parts a character is created from: carried items, the caps balance, the junk and
 * material counters and whether the equipment pack was already turned into inventory entries.
 *
 * @param items the carried items
 * @param caps caps on hand (at least 0)
 * @param materials junk and crafting materials
 * @param packItemsAdded whether the pack items were added to the inventory (once only)
 */
public record Inventory(@NonNull List<CarriedItem> items, long caps, @NonNull Materials materials,
    boolean packItemsAdded) {

  public static final @NonNull Inventory EMPTY = new Inventory(List.of(), 0, Materials.NONE, false);

  public Inventory {
    if (caps < 0) {
      throw new IllegalArgumentException("Caps cannot be negative");
    }
    items = List.copyOf(items);
  }
}
