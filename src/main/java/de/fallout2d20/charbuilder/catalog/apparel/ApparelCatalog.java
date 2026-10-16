package de.fallout2d20.charbuilder.catalog.apparel;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.CatalogReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/** The apparel catalog: the only place clothing, armor and robot armor are loaded (once, on first use). */
public final class ApparelCatalog {

  private static final @NonNull String RESOURCE = "/de/fallout2d20/charbuilder/catalog/apparel.json";
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
