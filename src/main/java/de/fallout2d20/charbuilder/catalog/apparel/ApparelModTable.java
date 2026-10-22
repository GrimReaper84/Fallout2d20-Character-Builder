package de.fallout2d20.charbuilder.catalog.apparel;

import de.fallout2d20.charbuilder.catalog.Book;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * The mod table a piece of apparel takes its own mods from: the Material mods of its armor type (CRB pp. 133–135,
 * WG pp. 90–93), Ballistic Weave for the clothing and outfits the books name (CRB pp. 125–128, table p. 126; WG
 * p. 86) or the Linings of the Underarmor Suit (WG p. 87). The Armor Upgrades (CRB p. 136) are shared by the armor
 * tables and have no table of their own; Wood armor takes only its Material mod (WG p. 93; user decision
 * 2026-10-22), Legion armor also the upgrades, as the book is silent (user decision 2026-10-22).
 */
public enum ApparelModTable {
  RAIDER("Raider Armor"),
  LEATHER("Leather Armor"),
  METAL("Metal Armor"),
  COMBAT("Combat Armor"),
  SYNTH("Synth Armor"),
  BALLISTIC_WEAVE("Ballistic Weave"),
  LEGION("Legion Armor"),
  MARINE("Marine Armor"),
  SCOUT("Scout Armor"),
  ROBOT("Robot Armor"),
  WOOD("Wood Armor"),
  LINING("Underarmor Lining");

  private final @NonNull String displayName;

  ApparelModTable(@NonNull String displayName) {
    this.displayName = displayName;
  }

  public @NonNull String displayName() {
    return displayName;
  }

  /** The slot the table's mods go into: Ballistic Weave or Lining for those tables, Material for the armor tables. */
  public @NonNull ApparelModSlot slot() {
    return switch (this) {
      case BALLISTIC_WEAVE -> ApparelModSlot.BALLISTIC_WEAVE;
      case LINING -> ApparelModSlot.LINING;
      default -> ApparelModSlot.MATERIAL;
    };
  }

  /** Whether a piece with this table also takes an Armor Upgrade (helmets aside): every armor table but Wood. */
  public boolean takesUpgrades() {
    return slot() == ApparelModSlot.MATERIAL && this != WOOD;
  }

  /**
   * Whether a piece of this kind can use the table: armor tables for armor, the weave for clothing, outfits and
   * headgear, the linings for clothing.
   */
  public boolean fits(@NonNull ApparelKind kind) {
    return switch (slot()) {
      case BALLISTIC_WEAVE ->
          kind == ApparelKind.CLOTHING || kind == ApparelKind.OUTFIT || kind == ApparelKind.HEADGEAR;
      case LINING -> kind == ApparelKind.CLOTHING;
      default -> kind == ApparelKind.ARMOR;
    };
  }

  /**
   * The tables a piece of this kind can use ({@link #fits}) that have mods of the enabled books, in declaration
   * order; a WG table is left out without the Wanderer's Guide.
   */
  public static @NonNull List<ApparelModTable> fitting(@NonNull ApparelKind kind, @NonNull Set<Book> books) {
    return Arrays.stream(values()).filter(table -> table.fits(kind) && ApparelCatalog.mods().stream()
        .anyMatch(mod -> mod.table() == table && mod.isAvailableWith(books))).toList();
  }
}
