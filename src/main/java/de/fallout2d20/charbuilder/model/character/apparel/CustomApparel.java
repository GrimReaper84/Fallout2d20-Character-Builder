package de.fallout2d20.charbuilder.model.character.apparel;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModTable;
import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A worn item outside the catalog: homebrew, an item of a book not catalogued yet, or the hand-typed
 * damage resistance of one location from a legacy save.
 *
 * @param name the item's name
 * @param kind how it layers (clothing, armor, robot armor …)
 * @param locations the hit locations it covers
 * @param physical physical damage resistance
 * @param energy energy damage resistance
 * @param radiation radiation damage resistance (0 when immune)
 * @param radiationImmune whether the covered locations are immune to radiation
 * @param carryWeight change of a robot's carry weight in lbs.
 * @param modTable the mod table chosen for it (user decision 2026-10-22), fitting its kind; {@code null} for none
 */
public record CustomApparel(
    @NonNull String name,
    @NonNull ApparelKind kind,
    @NonNull Set<HitLocation> locations,
    int physical,
    int energy,
    int radiation,
    boolean radiationImmune,
    int carryWeight,
    @Nullable ApparelModTable modTable) {

  public CustomApparel {
    if (name.isBlank() || locations.isEmpty() || physical < 0 || energy < 0 || radiation < 0
        || (radiationImmune && radiation != 0)) {
      throw new IllegalArgumentException("A custom item needs a name, a location and non-negative resistances");
    }
    if (modTable != null && !modTable.fits(kind)) {
      throw new IllegalArgumentException(kind.displayName() + " cannot take " + modTable.displayName() + " mods");
    }
    name = name.strip();
    locations = Set.copyOf(EnumSet.copyOf(locations));
  }

  /** A custom item without mod table. */
  public CustomApparel(@NonNull String name, @NonNull ApparelKind kind, @NonNull Set<HitLocation> locations,
      int physical, int energy, int radiation, boolean radiationImmune, int carryWeight) {
    this(name, kind, locations, physical, energy, radiation, radiationImmune, carryWeight, null);
  }

  public @NonNull CustomApparel withModTable(@Nullable ApparelModTable table) {
    return new CustomApparel(name, kind, locations, physical, energy, radiation, radiationImmune, carryWeight, table);
  }

  /** The values as typed, without mods. */
  public @NonNull ApparelValues values() {
    return new ApparelValues(kind, physical, energy, radiation, radiationImmune, carryWeight);
  }
}
