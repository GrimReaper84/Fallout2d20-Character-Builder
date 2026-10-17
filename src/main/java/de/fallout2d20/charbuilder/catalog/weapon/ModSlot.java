package de.fallout2d20.charbuilder.catalog.weapon;

import org.jspecify.annotations.NonNull;

/**
 * Where a mod sits on a weapon (CRB p. 87: a ranged weapon takes one mod of each type its description
 * lists, a melee weapon a single upgrade). Declared in the order the mod tables list them (CRB pp. 100–119,
 * WOA p. 23), which is also the order of the name prefixes.
 */
public enum ModSlot {
  RECEIVER("Receiver"),
  CAPACITOR("Capacitor"),
  FUEL("Fuel"),
  BARREL("Barrel"),
  DISH("Dish"),
  TANK("Tank"),
  NOZZLE("Nozzle"),
  GRIP("Grip"),
  STOCK("Stock"),
  SIGHTS("Sights"),
  MUZZLE("Muzzle"),
  UPGRADE("Upgrade");

  private final @NonNull String displayName;

  ModSlot(@NonNull String displayName) {
    this.displayName = displayName;
  }

  public @NonNull String displayName() {
    return displayName;
  }
}
