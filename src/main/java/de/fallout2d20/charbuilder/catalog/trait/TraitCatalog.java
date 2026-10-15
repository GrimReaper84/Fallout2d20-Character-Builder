package de.fallout2d20.charbuilder.catalog.trait;

import de.fallout2d20.charbuilder.catalog.CatalogReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * The trait catalog: the only place traits are loaded. The data file is read once, when the class is
 * first used.
 */
public final class TraitCatalog {

  private static final @NonNull String RESOURCE = "/de/fallout2d20/charbuilder/catalog/traits.json";
  private static final @NonNull Map<String, TraitDefinition> BY_ID = load();

  private TraitCatalog() {}

  /** All traits in catalog order. */
  public static @NonNull List<TraitDefinition> all() {
    return List.copyOf(BY_ID.values());
  }

  public static @NonNull Optional<TraitDefinition> byId(@NonNull String id) {
    return Optional.ofNullable(BY_ID.get(id));
  }

  /** Traits of one list (e.g. {@code survivor}), in catalog order. */
  public static @NonNull List<TraitDefinition> inList(@NonNull String listId) {
    return BY_ID.values().stream().filter(trait -> trait.list().equals(listId)).toList();
  }

  private static @NonNull Map<String, TraitDefinition> load() {
    Map<String, TraitDefinition> byId = new LinkedHashMap<>();
    for (TraitDefinition trait : CatalogReader.readList(RESOURCE, TraitDefinition.class)) {
      if (byId.put(trait.id(), trait) != null) {
        throw new IllegalStateException("Duplicate trait id in " + RESOURCE + ": " + trait.id());
      }
    }
    return byId;
  }
}
