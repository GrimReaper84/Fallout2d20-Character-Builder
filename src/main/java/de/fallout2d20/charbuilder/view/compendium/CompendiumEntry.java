package de.fallout2d20.charbuilder.view.compendium;

import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * One row of a compendium section (H2): what the table and the detail panel show of a catalog entry.
 *
 * @param id unique within the section; target of {@link Reference}s
 * @param name the name column
 * @param category the category for the section's category filter
 * @param cells one per {@link SectionContent#columns()}
 * @param details the detail panel's lines, the source last
 */
record CompendiumEntry(@NonNull String id, @NonNull String name, @NonNull String category, @NonNull List<Cell> cells,
    @NonNull List<DetailLine> details) {

  CompendiumEntry {
    cells = List.copyOf(cells);
    details = List.copyOf(details);
  }
}
