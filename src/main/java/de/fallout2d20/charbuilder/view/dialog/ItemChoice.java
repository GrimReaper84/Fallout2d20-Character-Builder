package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.item.ItemData;
import de.fallout2d20.charbuilder.catalog.item.MagazineData;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One row of the item catalog table: an item or a publication.
 *
 * @param id catalog id
 * @param name name as printed
 * @param weight weight as printed, e.g. {@code <1}; {@code null} where the table has none
 * @param effect the effect in short, or the number of issues of a publication; empty for none
 * @param source where the entry is described, e.g. {@code CRB p. 154}
 */
record ItemChoice(@NonNull String id, @NonNull String name, @Nullable String weight, @NonNull String effect,
    @NonNull String source) {

  static @NonNull ItemChoice of(@NonNull ItemData item) {
    String effects = item.effects();
    return new ItemChoice(item.id(), item.name(), item.weight(), effects == null ? "" : effects,
        item.sources().getFirst().toString());
  }

  static @NonNull ItemChoice of(@NonNull MagazineData magazine) {
    return new ItemChoice(magazine.id(), magazine.name(), MagazineData.WEIGHT,
        magazine.issues().isEmpty() ? magazine.summary() : magazine.issues().size() + " issues (chosen in the Gear tab)",
        magazine.sources().getFirst().toString());
  }
}
