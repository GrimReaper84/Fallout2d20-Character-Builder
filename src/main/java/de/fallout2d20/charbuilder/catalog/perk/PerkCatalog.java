package de.fallout2d20.charbuilder.catalog.perk;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.CatalogReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * The perk catalog: the only place perks are loaded. The data file is read once, when the class is
 * first used.
 */
public final class PerkCatalog {

  private static final @NonNull String RESOURCE = "/de/fallout2d20/charbuilder/catalog/perks.json";
  private static final @NonNull Map<String, PerkDefinition> BY_ID = load();

  private PerkCatalog() {}

  /** All perks in catalog (book) order. */
  public static @NonNull List<PerkDefinition> all() {
    return List.copyOf(BY_ID.values());
  }

  public static @NonNull Optional<PerkDefinition> byId(@NonNull String id) {
    return Optional.ofNullable(BY_ID.get(id));
  }

  /** Perks of the enabled books, in catalog order. */
  public static @NonNull List<PerkDefinition> available(@NonNull Set<Book> books) {
    return BY_ID.values().stream().filter(perk -> perk.isAvailableWith(books)).toList();
  }

  private static @NonNull Map<String, PerkDefinition> load() {
    Map<String, PerkDefinition> byId = new LinkedHashMap<>();
    for (PerkDefinition perk : CatalogReader.readList(RESOURCE, PerkDefinition.class)) {
      if (byId.put(perk.id(), perk) != null) {
        throw new IllegalStateException("Duplicate perk id in " + RESOURCE + ": " + perk.id());
      }
    }
    return byId;
  }
}
