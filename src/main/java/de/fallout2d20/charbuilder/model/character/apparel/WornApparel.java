package de.fallout2d20.charbuilder.model.character.apparel;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModSlot;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModTable;
import de.fallout2d20.charbuilder.catalog.apparel.Coverage;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorLocation;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryCatalog;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.equipment.StartingSource;
import de.fallout2d20.charbuilder.model.character.legendary.LegendaryRules;
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
 * hand). {@code legendary} is the piece's legendary property (WG pp. 104–108; one per piece, which fit via
 * {@link LegendaryRules}); an id the catalog does not know is kept and has no effect.
 */
public record WornApparel(
    @Nullable CatalogRef apparel,
    @Nullable CustomApparel custom,
    @Nullable HitLocation location,
    @NonNull String notes,
    boolean carried,
    @Nullable StartingSource startingSource,
    @NonNull Map<ApparelModSlot, CatalogRef> mods,
    @Nullable CatalogRef legendary) {

  public WornApparel {
    if ((apparel == null) == (custom == null)) {
      throw new IllegalArgumentException("A worn item is either a catalog item or a custom one");
    }
    mods = mods.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(mods));
  }

  /** An item that is not legendary. */
  public WornApparel(@Nullable CatalogRef apparel, @Nullable CustomApparel custom, @Nullable HitLocation location,
      @NonNull String notes, boolean carried, @Nullable StartingSource startingSource,
      @NonNull Map<ApparelModSlot, CatalogRef> mods) {
    this(apparel, custom, location, notes, carried, startingSource, mods, null);
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

  /**
   * Whether the piece is armor for perks that ask for worn armor (Ironclad, Sturdy Frame): armor pieces and the
   * outfits that count as armor ({@link ApparelData#countsAsArmor()}); not clothing, headgear or robot armor (user
   * decision 2026-10-22). Whether it is worn is up to the caller.
   */
  public boolean isArmor() {
    return values().map(ApparelValues::kind).filter(ApparelKind.ARMOR::equals).isPresent()
        || data().map(ApparelData::countsAsArmor).orElse(false);
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
    return data().map(data -> data.coverage().stream().filter(coverage -> coverage.isChosenOn(body))
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
      if (!coverage.isChosenOn(body)) {
        covered.addAll(onBody);
      } else if (location != null && onBody.contains(location)) {
        covered.add(location);
      }
    }
    return covered;
  }

  /** The item with another legendary property; {@code null} makes it an ordinary piece. */
  public @NonNull WornApparel withLegendary(@Nullable CatalogRef property) {
    return new WornApparel(apparel, custom, location, notes, carried, startingSource, mods, property);
  }

  /** The legendary property from the catalog; empty for an ordinary piece or an id outside the catalog. */
  public @NonNull Optional<LegendaryArmorData> legendaryProperty() {
    return legendary == null ? Optional.empty() : LegendaryCatalog.armorPropertyById(legendary.id());
  }

  /**
   * The columns of the Legendary Armor table the piece can roll on (WG p. 104): those of every location it can cover
   * (a single-limb piece counts its limb before one is chosen), in table order.
   */
  public @NonNull Set<LegendaryArmorLocation> legendaryLocations() {
    Set<LegendaryArmorLocation> columns = EnumSet.noneOf(LegendaryArmorLocation.class);
    if (custom != null) {
      custom.locations().forEach(covered -> columns.add(LegendaryArmorLocation.of(covered)));
    }
    data().ifPresent(entry -> entry.coverage().forEach(coverage -> coverage.locations()
        .forEach(covered -> columns.add(LegendaryArmorLocation.of(covered)))));
    return columns;
  }

  /** The name with the star mark and the legendary property, e.g. {@code ★ Sentinel's Combat Armor Chest Piece}. */
  public @NonNull String legendaryName() {
    return legendaryProperty().map(property -> LegendaryRules.name(property.name(), displayName()))
        .orElseGet(this::displayName);
  }

  public @NonNull WornApparel withLocation(@Nullable HitLocation newLocation) {
    return new WornApparel(apparel, custom, newLocation, notes, carried, startingSource, mods, legendary);
  }

  public @NonNull WornApparel withNotes(@Nullable String newNotes) {
    return new WornApparel(apparel, custom, location, newNotes == null ? "" : newNotes, carried, startingSource, mods,
        legendary);
  }

  public @NonNull WornApparel withCarried(boolean newCarried) {
    return new WornApparel(apparel, custom, location, notes, newCarried, startingSource, mods, legendary);
  }

  /** The item marked as part of the starting equipment; {@code null} for one added by hand. */
  public @NonNull WornApparel withStartingSource(@Nullable StartingSource source) {
    return new WornApparel(apparel, custom, location, notes, carried, source, mods, legendary);
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
    return new WornApparel(apparel, custom, location, notes, carried, startingSource, newMods, legendary);
  }

  /** The custom item with another mod table; installed mods that no longer fit (or are unknown) are removed. */
  public @NonNull WornApparel withCustomModTable(@Nullable ApparelModTable table) {
    if (custom == null) {
      throw new IllegalStateException("Only a custom item chooses its mod table");
    }
    WornApparel retabled = new WornApparel(null, custom.withModTable(table), location, notes, carried,
        startingSource, Map.of(), legendary);
    Map<ApparelModSlot, CatalogRef> kept = new EnumMap<>(ApparelModSlot.class);
    mods.forEach((slot, ref) -> {
      if (ApparelCatalog.modById(ref.id()).map(mod -> ApparelMods.fits(retabled, slot, mod)).orElse(false)) {
        kept.put(slot, ref);
      }
    });
    return kept.isEmpty() ? retabled : new WornApparel(null, retabled.custom(), location, notes, carried,
        startingSource, kept, legendary);
  }
}
