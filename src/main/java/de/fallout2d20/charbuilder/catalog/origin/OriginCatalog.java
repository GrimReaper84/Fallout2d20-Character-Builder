package de.fallout2d20.charbuilder.catalog.origin;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.CatalogReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * The origin catalog: the only place origins are loaded. The data file is read once, when the class
 * is first used (class initialization is lazy and thread-safe).
 */
public final class OriginCatalog {

  private static final @NonNull String RESOURCE = "/de/fallout2d20/charbuilder/catalog/origins.json";
  private static final @NonNull Map<String, OriginData> BY_ID = load();

  private OriginCatalog() {}

  /** All origins in catalog order. */
  public static @NonNull List<OriginData> all() {
    return List.copyOf(BY_ID.values());
  }

  public static @NonNull Optional<OriginData> byId(@NonNull String id) {
    return Optional.ofNullable(BY_ID.get(id));
  }

  /** Origins described in at least one of the given books, in catalog order. */
  public static @NonNull List<OriginData> available(@NonNull Set<Book> books) {
    return BY_ID.values().stream().filter(origin -> origin.isAvailableWith(books)).toList();
  }

  private static @NonNull Map<String, OriginData> load() {
    Map<String, OriginData> byId = new LinkedHashMap<>();
    for (OriginData origin : CatalogReader.readList(RESOURCE, OriginData.class)) {
      if (byId.put(origin.id(), origin) != null) {
        throw new IllegalStateException("Duplicate origin id in " + RESOURCE + ": " + origin.id());
      }
    }
    return byId;
  }
}
