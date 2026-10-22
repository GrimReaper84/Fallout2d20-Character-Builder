package de.fallout2d20.charbuilder.catalog.apparel;

import org.jspecify.annotations.NonNull;

/**
 * Where a mod sits on a piece of apparel (CRB p. 87): an armor piece takes one Material and one Upgrade mod,
 * clothing a single mod: Ballistic Weave (CRB p. 126) or, on the Underarmor Suit, a Lining (WG p. 87). Declared in
 * display order.
 */
public enum ApparelModSlot {
  MATERIAL("Material"),
  UPGRADE("Upgrade"),
  BALLISTIC_WEAVE("Ballistic Weave"),
  LINING("Lining");

  private final @NonNull String displayName;

  ApparelModSlot(@NonNull String displayName) {
    this.displayName = displayName;
  }

  public @NonNull String displayName() {
    return displayName;
  }

  /** Whether the slot is the single mod slot of clothing (Ballistic Weave, Lining) rather than an armor slot. */
  public boolean isSingle() {
    return this == BALLISTIC_WEAVE || this == LINING;
  }
}
