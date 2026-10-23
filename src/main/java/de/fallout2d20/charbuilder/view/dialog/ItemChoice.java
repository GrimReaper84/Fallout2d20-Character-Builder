package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.item.ItemData;
import de.fallout2d20.charbuilder.catalog.item.MagazineData;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One row of the item catalog table: an item or a publication.
 *
 * @param id catalog id
 * @param name name as printed
 * @param weight weight as printed, e.g. {@code <1}; {@code null} where the table has none
 * @param effect the effect in short, the rare perk of a rare book, the components of a junk item, or the number of
 *     issues of a publication; empty for none
 * @param source where the entry is described, e.g. {@code CRB p. 154}
 */
record ItemChoice(@NonNull String id, @NonNull String name, @Nullable String weight, @NonNull String effect,
    @NonNull String source) {

  /** The effect is the item's, the rare perk of a rare book or the components and tables of a junk item. */
  static @NonNull ItemChoice of(@NonNull ItemData item) {
    return new ItemChoice(item.id(), item.name(), item.weight(), effect(item), item.sources().getFirst().toString());
  }

  private static @NonNull String effect(@NonNull ItemData item) {
    String rarePerk = item.rarePerk();
    if (rarePerk != null) {
      return "Rare perk: " + PerkCatalog.nameOf(rarePerk);
    }
    String components = item.components();
    if (components != null) {
      return components + " (" + String.join(", ", item.junkTables()) + ")";
    }
    String effects = item.effects();
    return effects == null ? "" : effects;
  }

  static @NonNull ItemChoice of(@NonNull MagazineData magazine) {
    return new ItemChoice(magazine.id(), magazine.name(), MagazineData.WEIGHT,
        magazine.issues().isEmpty() ? magazine.summary() : magazine.issues().size() + " issues (chosen in the Gear tab)",
        magazine.sources().getFirst().toString());
  }
}
