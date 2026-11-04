package de.fallout2d20.charbuilder.model.scavenging;

import de.fallout2d20.charbuilder.catalog.loot.LootCategory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * The item listing of a location (CRB p. 195): its categories with their rolls. A category without an entry or with a
 * maximum of 0 is not part of the location.
 */
public record LocationItems(@NonNull Map<LootCategory, ItemRange> ranges) {

  private static final @NonNull ItemRange NONE = new ItemRange(0, 0);

  public LocationItems {
    ranges = Map.copyOf(ranges);
  }

  public @NonNull ItemRange range(@NonNull LootCategory category) {
    return ranges.getOrDefault(category, NONE);
  }

  /** The categories of the location, in book order. */
  public @NonNull List<LootCategory> categories() {
    return Arrays.stream(LootCategory.values()).filter(category -> range(category).max() > 0).toList();
  }
}
