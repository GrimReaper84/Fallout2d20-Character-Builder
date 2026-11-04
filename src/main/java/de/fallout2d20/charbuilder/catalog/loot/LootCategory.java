package de.fallout2d20.charbuilder.catalog.loot;

import org.jspecify.annotations.NonNull;

/**
 * The item categories of a location (CRB p. 200), with the table each is rolled on. Junk has no result table in the
 * Core Rulebook: its 2d20 is the number of items (CRB p. 208); {@code junk} is the table selection of WG p. 120.
 */
public enum LootCategory {
  AMMUNITION("Ammunition", "ammunition"),
  ARMOR("Armor", "armor"),
  CLOTHING("Clothing", "clothing"),
  FOOD("Food", "food"),
  BEVERAGES("Beverages", "beverages"),
  CHEMS("Chems", "chems"),
  WEAPONS_RANGED("Weapons (Ranged)", "weapons-ranged"),
  WEAPONS_MELEE("Weapons (Melee)", "weapons-melee"),
  WEAPONS_THROWN("Weapons (Thrown/Explosives)", "weapons-thrown"),
  ODDITIES("Oddities and Valuables", "oddities"),
  JUNK("Junk", "junk");

  private final @NonNull String displayName;
  private final @NonNull String tableId;

  LootCategory(@NonNull String displayName, @NonNull String tableId) {
    this.displayName = displayName;
    this.tableId = tableId;
  }

  public @NonNull String displayName() {
    return displayName;
  }

  public @NonNull String tableId() {
    return tableId;
  }
}
