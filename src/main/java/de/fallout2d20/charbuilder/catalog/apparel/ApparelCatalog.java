package de.fallout2d20.charbuilder.catalog.apparel;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.CatalogNames;
import de.fallout2d20.charbuilder.catalog.CatalogReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * The apparel catalog: the only place clothing, armor, robot armor and the apparel mods are loaded (once, on
 * first use).
 */
public final class ApparelCatalog {

  private static final @NonNull String RESOURCE = "/de/fallout2d20/charbuilder/catalog/apparel.json";
  private static final @NonNull String MODS = "/de/fallout2d20/charbuilder/catalog/apparel_mods.json";
  private static final @NonNull Map<String, ApparelModData> MODS_BY_ID = loadMods();
  private static final @NonNull Map<String, ApparelData> BY_ID = load();

  private ApparelCatalog() {}

  /** All items in catalog (book) order. */
  public static @NonNull List<ApparelData> all() {
    return List.copyOf(BY_ID.values());
  }

  public static @NonNull Optional<ApparelData> byId(@NonNull String id) {
    return Optional.ofNullable(BY_ID.get(id));
  }

  /** Items of the enabled books, in catalog order. */
  public static @NonNull List<ApparelData> available(@NonNull Set<Book> books) {
    return BY_ID.values().stream().filter(item -> item.isAvailableWith(books)).toList();
  }

  /** The item with this name or alias, ignoring case, blanks and apostrophe form ({@link CatalogNames}). */
  public static @NonNull Optional<ApparelData> byName(@NonNull String name) {
    String key = CatalogNames.normalize(name);
    return BY_ID.values().stream()
        .filter(item -> CatalogNames.normalize(item.name()).equals(key)
            || item.aliases().stream().anyMatch(alias -> CatalogNames.normalize(alias).equals(key)))
        .findFirst();
  }

  /** All apparel mods in catalog (table) order. */
  public static @NonNull List<ApparelModData> mods() {
    return List.copyOf(MODS_BY_ID.values());
  }

  public static @NonNull Optional<ApparelModData> modById(@NonNull String id) {
    return Optional.ofNullable(MODS_BY_ID.get(id));
  }

  private static @NonNull Map<String, ApparelModData> loadMods() {
    Map<String, ApparelModData> byId = new LinkedHashMap<>();
    for (ApparelModData mod : CatalogReader.readList(MODS, ApparelModData.class)) {
      if (byId.put(mod.id(), mod) != null) {
        throw new IllegalStateException("Duplicate apparel mod id in " + MODS + ": " + mod.id());
      }
    }
    return byId;
  }

  private static @NonNull Map<String, ApparelData> load() {
    Map<String, ApparelData> byId = new LinkedHashMap<>();
    for (ApparelData item : CatalogReader.readList(RESOURCE, ApparelData.class)) {
      if (byId.put(item.id(), item) != null) {
        throw new IllegalStateException("Duplicate apparel id in " + RESOURCE + ": " + item.id());
      }
    }
    return byId;
  }
}
