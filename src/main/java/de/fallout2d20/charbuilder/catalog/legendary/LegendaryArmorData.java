package de.fallout2d20.charbuilder.catalog.legendary;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A property of Legendary Armor (WG pp. 104–108): its d20 result per location column, the Poison DR it gives and its
 * rule in own words. The +1 Physical and Energy DR every legendary piece gets (WG p. 104) is not part of the entry.
 *
 * @param id stable catalog id, {@code legendary-armor-<name>}, referenced by save files
 * @param name name as printed, e.g. {@code Sentinel's}
 * @param sources book and page of the description
 * @param rolls the d20 result per location; a location without one cannot get the property
 * @param poison Poison DR on the covered locations (Poisoner's, WG p. 107); 0 for none
 * @param rule the whole property in own words, shown to the player
 */
public record LegendaryArmorData(
    @JsonProperty(required = true) @NonNull String id,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull List<Source> sources,
    @JsonProperty(required = true) @NonNull Map<LegendaryArmorLocation, Integer> rolls,
    int poison,
    @JsonProperty(required = true) @NonNull String rule) {

  public LegendaryArmorData {
    if (id == null || id.isBlank() || name == null || name.isBlank() || sources == null || sources.isEmpty()
        || rolls == null || rolls.isEmpty() || rule == null || rule.isBlank() || poison < 0) {
      throw new IllegalArgumentException("Legendary armor property needs id, name, source, rolls and rule: " + id);
    }
    sources = List.copyOf(sources);
    rolls = Map.copyOf(new EnumMap<>(rolls));
  }

  /** Whether a piece covering this location can get the property. */
  public boolean fits(@NonNull LegendaryArmorLocation location) {
    return rolls.containsKey(location);
  }

  /** The d20 result for this location; {@code null} if it has none. */
  public @Nullable Integer roll(@NonNull LegendaryArmorLocation location) {
    return rolls.get(location);
  }

  /** Whether one of the books describing the property is enabled. */
  public boolean isAvailableWith(@NonNull Set<Book> books) {
    return sources.stream().anyMatch(source -> books.contains(source.book()));
  }
}
