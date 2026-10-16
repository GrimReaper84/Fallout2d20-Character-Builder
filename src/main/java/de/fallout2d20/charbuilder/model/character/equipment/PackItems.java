package de.fallout2d20.charbuilder.model.character.equipment;

import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * The inventory entries made from the starting equipment ({@link PackInventory}).
 *
 * @param weapons recognized catalog weapons
 * @param apparel recognized catalog apparel, worn
 * @param items recognized catalog items and custom items for every line that was not recognized
 */
public record PackItems(
    @NonNull List<CarriedWeapon> weapons,
    @NonNull List<WornApparel> apparel,
    @NonNull List<CarriedItem> items) {

  public PackItems {
    weapons = List.copyOf(weapons);
    apparel = List.copyOf(apparel);
    items = List.copyOf(items);
  }

  /** Names of the entries that became custom items (rolls, ammunition formulas, unknown things). */
  public @NonNull List<String> customNames() {
    return items.stream().filter(item -> item.custom() != null).map(CarriedItem::displayName).toList();
  }
}
