package de.fallout2d20.charbuilder.catalog.apparel;

import org.jspecify.annotations.NonNull;

/**
 * Where a mod sits on a piece of apparel (CRB p. 87): an armor piece takes one Material and one Upgrade mod,
 * clothing a single mod, which in the Core Rulebook is always Ballistic Weave (CRB p. 126). Declared in display
 * order.
 */
public enum ApparelModSlot {
  MATERIAL("Material"),
  UPGRADE("Upgrade"),
  BALLISTIC_WEAVE("Ballistic Weave");

  private final @NonNull String displayName;

  ApparelModSlot(@NonNull String displayName) {
    this.displayName = displayName;
  }

  public @NonNull String displayName() {
    return displayName;
  }
}
