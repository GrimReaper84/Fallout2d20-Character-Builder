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
 * @param rolls amounts still to roll (Combat Dice formulas, loot table rolls, d20 caps), for the gear notes
 */
public record PackItems(
    @NonNull List<CarriedWeapon> weapons,
    @NonNull List<WornApparel> apparel,
    @NonNull List<CarriedItem> items,
    @NonNull List<String> rolls) {

  /** No entries: no pack chosen yet. */
  public static final @NonNull PackItems NONE = new PackItems(List.of(), List.of(), List.of(), List.of());

  public PackItems {
    weapons = List.copyOf(weapons);
    apparel = List.copyOf(apparel);
    items = List.copyOf(items);
    rolls = List.copyOf(rolls);
  }

}
