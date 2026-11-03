package de.fallout2d20.charbuilder.view.compendium;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * What a catalog section of the compendium lists (H2) for the enabled books.
 *
 * @param columns the columns after the name
 * @param categories the categories in filter order
 * @param entries the entries in catalog order
 */
record SectionContent(@NonNull Section section, @NonNull List<Column> columns, @NonNull List<String> categories,
    @NonNull List<CompendiumEntry> entries) {

  SectionContent {
    columns = List.copyOf(columns);
    categories = List.copyOf(categories);
    entries = List.copyOf(entries);
  }

  @NonNull Optional<CompendiumEntry> entry(@NonNull String id) {
    return entries.stream().filter(entry -> entry.id().equals(id)).findFirst();
  }
}
