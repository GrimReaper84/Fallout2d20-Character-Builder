package de.fallout2d20.charbuilder.catalog.item;

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
 * The item and magazine catalogs: the only place they are loaded (once, on first use). Ids are unique
 * across both files, so a carried item references either kind by its id.
 */
public final class ItemCatalog {

  private static final @NonNull String ITEMS = "/de/fallout2d20/charbuilder/catalog/items.json";
  private static final @NonNull String MAGAZINES = "/de/fallout2d20/charbuilder/catalog/magazines.json";
  private static final @NonNull Map<String, ItemData> ITEMS_BY_ID = loadItems();
  private static final @NonNull Map<String, MagazineData> MAGAZINES_BY_ID = loadMagazines();

  private ItemCatalog() {}

  /** All items in catalog (book) order. */
  public static @NonNull List<ItemData> all() {
    return List.copyOf(ITEMS_BY_ID.values());
  }

  public static @NonNull Optional<ItemData> byId(@NonNull String id) {
    return Optional.ofNullable(ITEMS_BY_ID.get(id));
  }

  /** Items of the enabled books, in catalog order. */
  public static @NonNull List<ItemData> available(@NonNull Set<Book> books) {
    return ITEMS_BY_ID.values().stream().filter(item -> item.isAvailableWith(books)).toList();
  }

  /** The item with this name or alias, ignoring case, blanks and apostrophe form ({@link CatalogNames}). */
  public static @NonNull Optional<ItemData> byName(@NonNull String name) {
    String key = CatalogNames.normalize(name);
    return ITEMS_BY_ID.values().stream()
        .filter(item -> CatalogNames.normalize(item.name()).equals(key)
            || item.aliases().stream().anyMatch(alias -> CatalogNames.normalize(alias).equals(key)))
        .findFirst();
  }

  /** All publications in book order. */
  public static @NonNull List<MagazineData> magazines() {
    return List.copyOf(MAGAZINES_BY_ID.values());
  }

  public static @NonNull Optional<MagazineData> magazineById(@NonNull String id) {
    return Optional.ofNullable(MAGAZINES_BY_ID.get(id));
  }

  /** Publications of the enabled books, in book order. */
  public static @NonNull List<MagazineData> availableMagazines(@NonNull Set<Book> books) {
    return MAGAZINES_BY_ID.values().stream().filter(magazine -> magazine.isAvailableWith(books)).toList();
  }

  private static @NonNull Map<String, ItemData> loadItems() {
    Map<String, ItemData> byId = new LinkedHashMap<>();
    for (ItemData item : CatalogReader.readList(ITEMS, ItemData.class)) {
      if (byId.put(item.id(), item) != null) {
        throw new IllegalStateException("Duplicate item id in " + ITEMS + ": " + item.id());
      }
    }
    return byId;
  }

  private static @NonNull Map<String, MagazineData> loadMagazines() {
    Map<String, MagazineData> byId = new LinkedHashMap<>();
    for (MagazineData magazine : CatalogReader.readList(MAGAZINES, MagazineData.class)) {
      if (ITEMS_BY_ID.containsKey(magazine.id()) || byId.put(magazine.id(), magazine) != null) {
        throw new IllegalStateException("Duplicate item or magazine id in " + MAGAZINES + ": " + magazine.id());
      }
    }
    return byId;
  }
}
