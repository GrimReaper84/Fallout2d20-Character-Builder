package de.fallout2d20.charbuilder.model.character.apparel;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.Coverage;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An item the character wears: a catalog item or a custom one (exactly one of both), the chosen limb for
 * a piece covering a single arm or leg (CRB p. 130; robot armor: one of the three arms, CRB p. 146), and a
 * free note, e.g. installed mods (mods as a note until they are modelled).
 */
public record WornApparel(
    @Nullable CatalogRef apparel,
    @Nullable CustomApparel custom,
    @Nullable HitLocation location,
    @NonNull String notes) {

  public WornApparel {
    if ((apparel == null) == (custom == null)) {
      throw new IllegalArgumentException("A worn item is either a catalog item or a custom one");
    }
  }

  /** A catalog item without chosen limb and note. */
  public static @NonNull WornApparel of(@NonNull CatalogRef apparel) {
    return new WornApparel(apparel, null, null, "");
  }

  /** A custom item without note. */
  public static @NonNull WornApparel of(@NonNull CustomApparel custom) {
    return new WornApparel(null, custom, null, "");
  }

  /** The catalog entry; empty for a custom item or an id outside the catalog. */
  public @NonNull Optional<ApparelData> data() {
    return apparel == null ? Optional.empty() : ApparelCatalog.byId(apparel.id());
  }

  /** Kind, resistances and carry weight change; empty for an id outside the catalog. */
  public @NonNull Optional<ApparelValues> values() {
    return custom != null ? Optional.of(custom.values()) : data().map(ApparelValues::of);
  }

  public @NonNull String displayName() {
    if (custom != null) {
      return custom.name();
    }
    return data().map(ApparelData::name).orElseGet(() -> apparel == null ? "" : apparel.displayName());
  }

  /**
   * Whether a limb must still be chosen: the catalog piece covers a single limb and none of its limbs is
   * set (none at all, or one that does not fit, e.g. from an edited file).
   */
  public boolean needsLocation() {
    return data().map(data -> data.coverage().stream().filter(Coverage::isChosen)
        .anyMatch(coverage -> location == null || !coverage.locations().contains(location))).orElse(false);
  }

  /**
   * The hit locations covered: the custom item's locations, or per catalog coverage the fixed locations
   * and, for a single limb, the chosen one (none while it is not chosen).
   */
  public @NonNull Set<HitLocation> locations() {
    if (custom != null) {
      return custom.locations();
    }
    Set<HitLocation> covered = EnumSet.noneOf(HitLocation.class);
    for (Coverage coverage : data().map(ApparelData::coverage).orElse(List.of())) {
      if (!coverage.isChosen()) {
        covered.addAll(coverage.locations());
      } else if (location != null && coverage.locations().contains(location)) {
        covered.add(location);
      }
    }
    return covered;
  }

  public @NonNull WornApparel withLocation(@Nullable HitLocation newLocation) {
    return new WornApparel(apparel, custom, newLocation, notes);
  }

  public @NonNull WornApparel withNotes(@Nullable String newNotes) {
    return new WornApparel(apparel, custom, location, newNotes == null ? "" : newNotes);
  }
}
