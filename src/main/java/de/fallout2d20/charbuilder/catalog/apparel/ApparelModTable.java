package de.fallout2d20.charbuilder.catalog.apparel;

import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * The mod table a piece of apparel takes its own mods from: the Material mods of its armor type (CRB pp. 133–135)
 * or Ballistic Weave for the clothing the book names (CRB pp. 125–128, table p. 126). The Armor Upgrades
 * (CRB p. 136) are shared by all armor tables and have no table of their own.
 */
public enum ApparelModTable {
  RAIDER("Raider Armor"),
  LEATHER("Leather Armor"),
  METAL("Metal Armor"),
  COMBAT("Combat Armor"),
  SYNTH("Synth Armor"),
  BALLISTIC_WEAVE("Ballistic Weave");

  private final @NonNull String displayName;

  ApparelModTable(@NonNull String displayName) {
    this.displayName = displayName;
  }

  public @NonNull String displayName() {
    return displayName;
  }

  /** The slot the table's mods go into: Ballistic Weave for the weave table, Material for the armor tables. */
  public @NonNull ApparelModSlot slot() {
    return this == BALLISTIC_WEAVE ? ApparelModSlot.BALLISTIC_WEAVE : ApparelModSlot.MATERIAL;
  }

  /** Whether a piece of this kind can use the table: armor tables for armor, the weave for clothing, outfits and headgear. */
  public boolean fits(@NonNull ApparelKind kind) {
    return this == BALLISTIC_WEAVE
        ? kind == ApparelKind.CLOTHING || kind == ApparelKind.OUTFIT || kind == ApparelKind.HEADGEAR
        : kind == ApparelKind.ARMOR;
  }

  /** The tables a piece of this kind can use ({@link #fits}), in declaration order. */
  public static @NonNull List<ApparelModTable> fitting(@NonNull ApparelKind kind) {
    return Arrays.stream(values()).filter(table -> table.fits(kind)).toList();
  }
}
