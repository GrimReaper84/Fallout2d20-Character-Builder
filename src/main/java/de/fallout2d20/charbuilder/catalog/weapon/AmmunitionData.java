package de.fallout2d20.charbuilder.catalog.weapon;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One ammunition type (CRB p. 91, Ammunition Availability and Rarity). Ammunition is counted in shots
 * per type; all weapons using the type share them (CRB p. 91, no magazines).
 *
 * @param id stable catalog id, referenced by weapons and save files
 * @param name name as printed in the book
 * @param sources book and page of the table entry
 * @param quantityFound shots found when available, as printed, e.g. {@code 10+5 CD}; {@code null} where the
 *     book gives no table row (WOA p. 23: cryo cells are only named)
 * @param weight weight per shot as printed, e.g. {@code <1} (the unit is decided with carry weight, C4);
 *     {@code null} without table row
 * @param cost cost per shot in caps; {@code null} if it varies (Syringer ammo) or without table row
 * @param rarity rarity 0–6; {@code null} without table row
 */
public record AmmunitionData(
    @JsonProperty(required = true) @NonNull String id,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull List<Source> sources,
    @Nullable String quantityFound,
    @Nullable String weight,
    @Nullable Integer cost,
    @Nullable Integer rarity) {

  public AmmunitionData {
    if (id == null || id.isBlank() || name == null || name.isBlank() || sources == null || sources.isEmpty()
        || (quantityFound != null && quantityFound.isBlank()) || (weight != null && weight.isBlank())
        || (cost != null && cost < 0) || (rarity != null && rarity < 0)) {
      throw new IllegalArgumentException("Invalid ammunition: " + id);
    }
    sources = List.copyOf(sources);
  }

  /** Whether one of the books describing the ammunition is enabled. */
  public boolean isAvailableWith(@NonNull Set<Book> books) {
    return sources.stream().anyMatch(source -> books.contains(source.book()));
  }
}
