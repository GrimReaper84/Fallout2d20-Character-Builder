package de.fallout2d20.charbuilder.model.scavenging;

import de.fallout2d20.charbuilder.catalog.loot.LootCategory;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * Starting values for the item listing of a location. <b>House rule without book evidence:</b> the book leaves the
 * listing to the GM (CRB p. 195); these values only spare the GM from filling in every line, and stay editable.
 */
public final class ScaleSuggestion {

  private ScaleSuggestion() {}

  public static @NonNull LocationItems of(@NonNull LocationScale scale) {
    return new LocationItems(switch (scale) {
      case TINY -> Map.of(
          LootCategory.AMMUNITION, new ItemRange(0, 1),
          LootCategory.CHEMS, new ItemRange(0, 1),
          LootCategory.ODDITIES, new ItemRange(1, 2));
      case SMALL -> Map.of(
          LootCategory.JUNK, new ItemRange(1, 2),
          LootCategory.FOOD, new ItemRange(0, 1),
          LootCategory.BEVERAGES, new ItemRange(0, 1),
          LootCategory.AMMUNITION, new ItemRange(0, 1),
          LootCategory.CLOTHING, new ItemRange(0, 1),
          LootCategory.WEAPONS_MELEE, new ItemRange(0, 1),
          LootCategory.WEAPONS_THROWN, new ItemRange(0, 1));
      case AVERAGE -> Map.ofEntries(
          Map.entry(LootCategory.JUNK, new ItemRange(2, 4)),
          Map.entry(LootCategory.FOOD, new ItemRange(1, 3)),
          Map.entry(LootCategory.BEVERAGES, new ItemRange(1, 2)),
          Map.entry(LootCategory.CLOTHING, new ItemRange(1, 2)),
          Map.entry(LootCategory.ARMOR, new ItemRange(0, 1)),
          Map.entry(LootCategory.WEAPONS_MELEE, new ItemRange(0, 1)),
          Map.entry(LootCategory.WEAPONS_RANGED, new ItemRange(0, 1)),
          Map.entry(LootCategory.WEAPONS_THROWN, new ItemRange(0, 1)),
          Map.entry(LootCategory.AMMUNITION, new ItemRange(0, 2)),
          Map.entry(LootCategory.CHEMS, new ItemRange(0, 1)),
          Map.entry(LootCategory.ODDITIES, new ItemRange(0, 1)));
      case LARGE -> Map.ofEntries(
          Map.entry(LootCategory.JUNK, new ItemRange(4, 8)),
          Map.entry(LootCategory.FOOD, new ItemRange(2, 4)),
          Map.entry(LootCategory.BEVERAGES, new ItemRange(2, 4)),
          Map.entry(LootCategory.CLOTHING, new ItemRange(1, 4)),
          Map.entry(LootCategory.ARMOR, new ItemRange(1, 3)),
          Map.entry(LootCategory.WEAPONS_MELEE, new ItemRange(1, 2)),
          Map.entry(LootCategory.WEAPONS_RANGED, new ItemRange(1, 3)),
          Map.entry(LootCategory.WEAPONS_THROWN, new ItemRange(1, 2)),
          Map.entry(LootCategory.AMMUNITION, new ItemRange(1, 4)),
          Map.entry(LootCategory.CHEMS, new ItemRange(1, 3)),
          Map.entry(LootCategory.ODDITIES, new ItemRange(1, 3)));
    });
  }
}
