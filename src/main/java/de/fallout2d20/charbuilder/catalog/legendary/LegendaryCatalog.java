package de.fallout2d20.charbuilder.catalog.legendary;

import de.fallout2d20.charbuilder.catalog.CatalogReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;

/**
 * The catalogs of legendary weapon and armor properties (WG pp. 96–108): the only place they are loaded, once, when
 * the class is first used. Every weapon type or location column has a property (so a roll ends), and within a column
 * no two properties share a d20 result.
 */
public final class LegendaryCatalog {

  private static final @NonNull String WEAPONS = "/de/fallout2d20/charbuilder/catalog/legendary_weapons.json";
  private static final @NonNull String ARMOR = "/de/fallout2d20/charbuilder/catalog/legendary_armor.json";
  private static final @NonNull Map<String, LegendaryWeaponData> WEAPONS_BY_ID = load(WEAPONS,
      LegendaryWeaponData.class, LegendaryWeaponData::id);
  private static final @NonNull Map<String, LegendaryArmorData> ARMOR_BY_ID = load(ARMOR, LegendaryArmorData.class,
      LegendaryArmorData::id);

  static {
    for (LegendaryWeaponType type : LegendaryWeaponType.values()) {
      for (int table = 1; table <= 2; table++) {
        int number = table;
        unique(WEAPONS, type + " table " + table, WEAPONS_BY_ID.values().stream()
            .filter(property -> property.table() == number).map(property -> property.roll(type)).toList());
      }
    }
    for (LegendaryArmorLocation location : LegendaryArmorLocation.values()) {
      unique(ARMOR, location.name(), ARMOR_BY_ID.values().stream().map(property -> property.roll(location)).toList());
    }
  }

  private LegendaryCatalog() {}

  /** All legendary weapon properties in catalog (table) order. */
  public static @NonNull List<LegendaryWeaponData> weaponProperties() {
    return List.copyOf(WEAPONS_BY_ID.values());
  }

  public static @NonNull Optional<LegendaryWeaponData> weaponPropertyById(@NonNull String id) {
    return Optional.ofNullable(WEAPONS_BY_ID.get(id));
  }

  /** All legendary armor properties in catalog (table) order. */
  public static @NonNull List<LegendaryArmorData> armorProperties() {
    return List.copyOf(ARMOR_BY_ID.values());
  }

  public static @NonNull Optional<LegendaryArmorData> armorPropertyById(@NonNull String id) {
    return Optional.ofNullable(ARMOR_BY_ID.get(id));
  }

  private static <T> @NonNull Map<String, T> load(@NonNull String resource, @NonNull Class<T> type,
      @NonNull Function<T, String> id) {
    Map<String, T> byId = new LinkedHashMap<>();
    for (T entry : CatalogReader.readList(resource, type)) {
      if (byId.put(id.apply(entry), entry) != null) {
        throw new IllegalStateException("Duplicate id in " + resource + ": " + id.apply(entry));
      }
    }
    return byId;
  }

  private static void unique(@NonNull String resource, @NonNull String column, @NonNull List<Integer> rolls) {
    List<Integer> present = rolls.stream().filter(Objects::nonNull).toList();
    if (present.isEmpty()) {
      throw new IllegalStateException("No property in " + resource + ", column " + column);
    }
    if (present.stream().distinct().count() != present.size()) {
      throw new IllegalStateException("Duplicate d20 result in " + resource + ", column " + column);
    }
  }
}
