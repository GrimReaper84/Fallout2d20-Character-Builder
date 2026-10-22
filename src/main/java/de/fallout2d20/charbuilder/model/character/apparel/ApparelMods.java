package de.fallout2d20.charbuilder.model.character.apparel;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModSlot;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModTable;
import de.fallout2d20.charbuilder.catalog.apparel.Coverage;
import de.fallout2d20.charbuilder.catalog.apparel.ModPlacement;
import de.fallout2d20.charbuilder.catalog.weapon.ModPerk;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Which mods fit a piece of apparel. CRB p. 87: an armor piece takes one Material and one Upgrade mod, clothing a
 * single mod. The Material mods come from the table of the piece's armor type (CRB pp. 133–135); Metal, Combat and
 * Synth helmets take only a Material mod (CRB pp. 134–135); the Armor Upgrades fit every armor type with a table
 * (not Vault-Tec Security armor) by location: all locations, torso only, arms only, legs only (CRB p. 136). The
 * clothing the book names takes Ballistic Weave (CRB pp. 125–128). A custom piece uses the table chosen for it (user
 * decision 2026-10-22). Mods on torso armor weigh and cost double; the torso-only rows already show doubled values
 * (CRB pp. 133–135).
 */
public final class ApparelMods {

  private ApparelMods() {}

  /** The slots on offer, in slot order; none for apparel without a mod table or an id outside the catalog. */
  public static @NonNull List<ApparelModSlot> slots(@NonNull WornApparel item) {
    ApparelModTable table = table(item);
    if (table == null) {
      return List.of();
    }
    if (table.slot() == ApparelModSlot.BALLISTIC_WEAVE) {
      return List.of(ApparelModSlot.BALLISTIC_WEAVE);
    }
    return isHelmet(item) ? List.of(ApparelModSlot.MATERIAL) : List.of(ApparelModSlot.MATERIAL, ApparelModSlot.UPGRADE);
  }

  /** The mods that can go into a slot of the piece, of the enabled books, in catalog order. */
  public static @NonNull List<ApparelModData> options(@NonNull WornApparel item, @NonNull ApparelModSlot slot,
      @NonNull Set<Book> books) {
    return ApparelCatalog.mods().stream().filter(mod -> fits(item, slot, mod) && mod.isAvailableWith(books)).toList();
  }

  /**
   * Whether the mod can go into the slot: the slot is on offer and the mod is of the piece's table (Material,
   * Ballistic Weave) or an upgrade whose placement fits the locations the piece covers.
   */
  public static boolean fits(@NonNull WornApparel item, @NonNull ApparelModSlot slot, @NonNull ApparelModData mod) {
    if (mod.slot() != slot || !slots(item).contains(slot)) {
      return false;
    }
    ModPlacement placement = mod.placement();
    return placement != null ? placement.fits(reach(item)) : mod.table() == table(item);
  }

  /**
   * The piece with the mod installed in the slot ({@code null} removes the slot's mod).
   *
   * @throws IllegalArgumentException if the mod does not fit the slot of this piece
   */
  public static @NonNull WornApparel install(@NonNull WornApparel item, @NonNull ApparelModSlot slot,
      @Nullable ApparelModData mod) {
    if (mod != null && !fits(item, slot, mod)) {
      throw new IllegalArgumentException(mod.id() + " does not fit the " + slot + " of " + item.displayName());
    }
    return item.withMod(slot, mod == null ? null : CatalogRef.of(mod.id()));
  }

  /**
   * The installed mods that the catalog knows and that fit their slot, in slot order. Unknown ids and mods that do
   * not fit (a hand-edited file, a later catalog fix) are left out, so they have no effect.
   */
  public static @NonNull List<ApparelModData> installedMods(@NonNull WornApparel item) {
    return item.mods().entrySet().stream()
        .flatMap(entry -> ApparelCatalog.modById(entry.getValue().id())
            .filter(mod -> fits(item, entry.getKey(), mod)).stream())
        .toList();
  }

  /**
   * How often the mod's printed weight and cost count on this piece: twice on a piece covering the torso (CRB pp.
   * 133–135), except for the torso-only upgrades, whose rows are already doubled, and Ballistic Weave, whose table
   * has no such rule (assumption confirmed by the user 2026-10-22).
   */
  public static int factor(@NonNull WornApparel item, @NonNull ApparelModData mod) {
    boolean doubled = reach(item).contains(HitLocation.TORSO) && mod.slot() != ApparelModSlot.BALLISTIC_WEAVE
        && mod.placement() != ModPlacement.TORSO;
    return doubled ? 2 : 1;
  }

  /** The perk ranks needed for the mod that the character does not have; information only (CRB p. 87). */
  public static @NonNull List<ModPerk> missingPerks(@NonNull PlayerCharacter character, @NonNull ApparelModData mod) {
    return mod.perks().stream().filter(perk -> !character.hasPerkRank(perk.perk(), perk.rank())).toList();
  }

  /** The table of the piece's own mods: the catalog entry's or the one chosen for a custom piece. */
  public static @Nullable ApparelModTable table(@NonNull WornApparel item) {
    CustomApparel custom = item.custom();
    if (custom != null) {
      return custom.modTable();
    }
    return item.data().map(ApparelData::modTable).orElse(null);
  }

  /**
   * The locations the piece can cover, whichever limb is chosen: a single-limb catalog piece reaches both arms or
   * both legs, so the arm and leg upgrades fit before a side is chosen.
   */
  static @NonNull Set<HitLocation> reach(@NonNull WornApparel item) {
    CustomApparel custom = item.custom();
    if (custom != null) {
      return custom.locations();
    }
    Set<HitLocation> reach = EnumSet.noneOf(HitLocation.class);
    item.data().ifPresent(data -> data.coverage().stream().map(Coverage::locations).forEach(reach::addAll));
    return reach;
  }

  private static boolean isHelmet(@NonNull WornApparel item) {
    return reach(item).equals(Set.of(HitLocation.HEAD));
  }
}
