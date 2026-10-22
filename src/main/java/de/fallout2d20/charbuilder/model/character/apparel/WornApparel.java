package de.fallout2d20.charbuilder.model.character.apparel;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModSlot;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModTable;
import de.fallout2d20.charbuilder.catalog.apparel.Coverage;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.equipment.StartingSource;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An item the character wears: a catalog item or a custom one (exactly one of both), the chosen limb for
 * a piece covering a single arm or leg (CRB p. 130; robot armor: one of the three arms, CRB p. 146, or of
 * the two arms of a Protectron), a free note, whether it is only carried instead of worn (user decision
 * 2026-10-16: only worn pieces protect; all count toward the load) and the mods installed per slot (CRB p. 87;
 * which fit via {@link ApparelMods}, values via {@link ModdedApparel}). A mod id the catalog does not know is
 * kept but has no effect. {@code startingSource} marks a piece of the starting equipment ({@code null}: added by
 * hand).
 */
public record WornApparel(
    @Nullable CatalogRef apparel,
    @Nullable CustomApparel custom,
    @Nullable HitLocation location,
    @NonNull String notes,
    boolean carried,
    @Nullable StartingSource startingSource,
    @NonNull Map<ApparelModSlot, CatalogRef> mods) {

  public WornApparel {
    if ((apparel == null) == (custom == null)) {
      throw new IllegalArgumentException("A worn item is either a catalog item or a custom one");
    }
    mods = mods.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(mods));
  }

  /** An item without mods. */
  public WornApparel(@Nullable CatalogRef apparel, @Nullable CustomApparel custom, @Nullable HitLocation location,
      @NonNull String notes, boolean carried, @Nullable StartingSource startingSource) {
    this(apparel, custom, location, notes, carried, startingSource, Map.of());
  }

  /** An item added by hand. */
  public WornApparel(@Nullable CatalogRef apparel, @Nullable CustomApparel custom, @Nullable HitLocation location,
      @NonNull String notes, boolean carried) {
    this(apparel, custom, location, notes, carried, null, Map.of());
  }

  /** A catalog item without chosen limb and note. */
  public static @NonNull WornApparel of(@NonNull CatalogRef apparel) {
    return new WornApparel(apparel, null, null, "", false, null);
  }

  /** A custom item without note. */
  public static @NonNull WornApparel of(@NonNull CustomApparel custom) {
    return new WornApparel(null, custom, null, "", false, null);
  }

  /** The catalog entry; empty for a custom item or an id outside the catalog. */
  public @NonNull Optional<ApparelData> data() {
    return apparel == null ? Optional.empty() : ApparelCatalog.byId(apparel.id());
  }

  /** Kind, resistances and carry weight change with the installed mods; empty for an id outside the catalog. */
  public @NonNull Optional<ApparelValues> values() {
    return ModdedApparel.of(this).map(ModdedApparel::values);
  }

  public @NonNull String displayName() {
    if (custom != null) {
      return custom.name();
    }
    return data().map(ApparelData::name).orElseGet(() -> apparel == null ? "" : apparel.displayName());
  }

  /**
   * Whether a limb must still be chosen: the catalog piece covers a single limb and none of its limbs on
   * this body is set (none at all, or one that does not fit, e.g. from an edited file or another origin).
   *
   * @param body the hit locations of the wearer
   */
  public boolean needsLocation(@NonNull List<HitLocation> body) {
    return data().map(data -> data.coverage().stream().filter(Coverage::isChosen)
        .anyMatch(coverage -> location == null || !coverage.locationsOn(body).contains(location))).orElse(false);
  }

  /**
   * The hit locations covered: the custom item's locations, or per catalog coverage the fixed locations
   * and, for a single limb, the chosen one (none while it is not chosen); robot coverage is mapped onto a
   * humanoid body ({@link Coverage#locationsOn}).
   *
   * @param body the hit locations of the wearer
   */
  public @NonNull Set<HitLocation> locations(@NonNull List<HitLocation> body) {
    if (custom != null) {
      return custom.locations();
    }
    Set<HitLocation> covered = EnumSet.noneOf(HitLocation.class);
    for (Coverage coverage : data().map(ApparelData::coverage).orElse(List.of())) {
      List<HitLocation> onBody = coverage.locationsOn(body);
      if (!coverage.isChosen()) {
        covered.addAll(onBody);
      } else if (location != null && onBody.contains(location)) {
        covered.add(location);
      }
    }
    return covered;
  }

  public @NonNull WornApparel withLocation(@Nullable HitLocation newLocation) {
    return new WornApparel(apparel, custom, newLocation, notes, carried, startingSource, mods);
  }

  public @NonNull WornApparel withNotes(@Nullable String newNotes) {
    return new WornApparel(apparel, custom, location, newNotes == null ? "" : newNotes, carried, startingSource, mods);
  }

  public @NonNull WornApparel withCarried(boolean newCarried) {
    return new WornApparel(apparel, custom, location, notes, newCarried, startingSource, mods);
  }

  /** The item marked as part of the starting equipment; {@code null} for one added by hand. */
  public @NonNull WornApparel withStartingSource(@Nullable StartingSource source) {
    return new WornApparel(apparel, custom, location, notes, carried, source, mods);
  }

  /**
   * The item with {@code mod} in the slot ({@code null} removes the slot's mod); the fitting rules are applied by
   * {@link ApparelMods#install}.
   */
  public @NonNull WornApparel withMod(@NonNull ApparelModSlot slot, @Nullable CatalogRef mod) {
    Map<ApparelModSlot, CatalogRef> newMods = new EnumMap<>(ApparelModSlot.class);
    newMods.putAll(mods);
    if (mod == null) {
      newMods.remove(slot);
    } else {
      newMods.put(slot, mod);
    }
    return new WornApparel(apparel, custom, location, notes, carried, startingSource, newMods);
  }

  /** The custom item with another mod table; installed mods that no longer fit (or are unknown) are removed. */
  public @NonNull WornApparel withCustomModTable(@Nullable ApparelModTable table) {
    if (custom == null) {
      throw new IllegalStateException("Only a custom item chooses its mod table");
    }
    WornApparel retabled = new WornApparel(null, custom.withModTable(table), location, notes, carried,
        startingSource, Map.of());
    Map<ApparelModSlot, CatalogRef> kept = new EnumMap<>(ApparelModSlot.class);
    mods.forEach((slot, ref) -> {
      if (ApparelCatalog.modById(ref.id()).map(mod -> ApparelMods.fits(retabled, slot, mod)).orElse(false)) {
        kept.put(slot, ref);
      }
    });
    return kept.isEmpty() ? retabled : new WornApparel(null, retabled.custom(), location, notes, carried,
        startingSource, kept);
  }
}
