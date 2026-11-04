package de.fallout2d20.charbuilder.model.scavenging;

import de.fallout2d20.charbuilder.catalog.loot.LootCategory;
import java.util.List;
import java.util.StringJoiner;
import org.jspecify.annotations.NonNull;

/** Finds as plain text, to pass on to the players. */
public final class FindReport {

  private FindReport() {}

  /**
   * The finds under a heading.
   *
   * @param heading the first line
   * @param finds the finds; they are listed per category in book order
   */
  public static @NonNull String text(@NonNull String heading, @NonNull List<Find> finds) {
    StringJoiner text = new StringJoiner(System.lineSeparator());
    text.add(heading);
    for (LootCategory category : LootCategory.values()) {
      List<Find> inCategory = finds.stream().filter(find -> find.category() == category).toList();
      if (!inCategory.isEmpty()) {
        text.add(category.displayName() + ":");
        inCategory.forEach(find -> text.add("- " + find.fullText()));
      }
    }
    return text.toString();
  }
}
