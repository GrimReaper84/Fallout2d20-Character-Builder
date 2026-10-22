package de.fallout2d20.charbuilder.catalog.weapon;

import org.jspecify.annotations.NonNull;

/**
 * Where a mod sits on a weapon (CRB p. 87: a ranged weapon takes one mod of each type its description
 * lists, a melee weapon a single upgrade). Declared in the order the mod tables list them (CRB pp. 100–119,
 * WOA p. 23, WG pp. 66–84), which is also the order of the name prefixes. The Wanderer's Guide adds Tesla coils
 * (Gauss Minigun), frames (bows and crossbows), magazines (Gatling Gun) and the Chainsaw's Flamer, which goes in
 * addition to its other mod (WG p. 78), as an add-on.
 */
public enum ModSlot {
  RECEIVER("Receiver"),
  CAPACITOR("Capacitor"),
  FUEL("Fuel"),
  BARREL("Barrel"),
  TESLA_COIL("Tesla Coil"),
  DISH("Dish"),
  TANK("Tank"),
  NOZZLE("Nozzle"),
  FRAME("Frame"),
  GRIP("Grip"),
  STOCK("Stock"),
  MAGAZINE("Magazine"),
  SIGHTS("Sights"),
  MUZZLE("Muzzle"),
  UPGRADE("Upgrade"),
  ADD_ON("Add-on");

  private final @NonNull String displayName;

  ModSlot(@NonNull String displayName) {
    this.displayName = displayName;
  }

  public @NonNull String displayName() {
    return displayName;
  }
}
