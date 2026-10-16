package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.item.ItemData;
import de.fallout2d20.charbuilder.catalog.item.MagazineData;
import org.jspecify.annotations.NonNull;

/**
 * One line of the item catalog list: an item or a publication, with the category it is filtered by.
 *
 * @param id catalog id
 * @param name name as printed
 * @param category category shown and filtered by, e.g. {@code Chem}, {@code Book/Magazine}
 * @param description list text after the name, e.g. the effect and the source
 */
record ItemChoice(@NonNull String id, @NonNull String name, @NonNull String category, @NonNull String description) {

  static @NonNull ItemChoice of(@NonNull ItemData item) {
    String effects = item.effects();
    return new ItemChoice(item.id(), item.name(), item.category().displayName(),
        (effects == null ? "" : effects + " – ") + item.sources().getFirst());
  }

  static @NonNull ItemChoice of(@NonNull MagazineData magazine) {
    return new ItemChoice(magazine.id(), magazine.name(), MagazineData.CATEGORY,
        (magazine.issues().isEmpty() ? "" : magazine.issues().size() + " issues – ") + magazine.sources().getFirst());
  }
}
