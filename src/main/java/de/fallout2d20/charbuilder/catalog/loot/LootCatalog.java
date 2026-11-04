package de.fallout2d20.charbuilder.catalog.loot;

import de.fallout2d20.charbuilder.catalog.CatalogReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/** The loot tables: the only place they are loaded (once, on first use). */
public final class LootCatalog {

  private static final @NonNull String TABLES = "/de/fallout2d20/charbuilder/catalog/loot_tables.json";
  private static final @NonNull Map<String, LootTable> BY_ID = load();

  private LootCatalog() {}

  /** All tables in book order. */
  public static @NonNull List<LootTable> all() {
    return List.copyOf(BY_ID.values());
  }

  /**
   * The table with this id.
   *
   * @throws IllegalArgumentException for an unknown id, which is a programming error
   */
  public static @NonNull LootTable table(@NonNull String id) {
    LootTable table = BY_ID.get(id);
    if (table == null) {
      throw new IllegalArgumentException("Unknown loot table: " + id);
    }
    return table;
  }

  private static @NonNull Map<String, LootTable> load() {
    Map<String, LootTable> byId = new LinkedHashMap<>();
    for (LootTable table : CatalogReader.readList(TABLES, LootTable.class)) {
      if (byId.put(table.id(), table) != null) {
        throw new IllegalStateException("Duplicate loot table in " + TABLES + ": " + table.id());
      }
    }
    for (LootTable table : byId.values()) {
      for (LootRow row : table.rows()) {
        if (row.next() != null && !byId.containsKey(row.next())) {
          throw new IllegalStateException("Loot table " + table.id() + " sends to unknown table " + row.next());
        }
      }
    }
    return byId;
  }
}
