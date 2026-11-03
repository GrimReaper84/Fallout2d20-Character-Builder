package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.equipment.D20TableRow;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentCatalog;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import org.jspecify.annotations.NonNull;

/**
 * The roll tables of the GM compendium (H2). The trinket (CRB p. 80) and Outcast (WG p. 19) tables are plain d20
 * tables; the legendary tables need a weapon type or armor location and roll with {@code LegendaryRoll}.
 */
enum RollTable {
  TRINKETS("Trinkets", Book.CRB),
  OUTCAST_EQUIPMENT("Outcast Equipment Table", Book.WG),
  LEGENDARY_WEAPON("Legendary weapon", Book.WG),
  LEGENDARY_ARMOR("Legendary armor", Book.WG);

  private final @NonNull String title;
  private final @NonNull Book book;

  RollTable(@NonNull String title, @NonNull Book book) {
    this.title = title;
    this.book = book;
  }

  @NonNull String title() {
    return title;
  }

  @NonNull Book book() {
    return book;
  }

  /** The 20 rows of a plain d20 table in roll order; empty for the legendary tables. */
  @NonNull List<D20TableRow> rows() {
    return isD20Table() ? IntStream.rangeClosed(1, D20TableRow.DIE_SIZE).mapToObj(this::row).toList() : List.of();
  }

  /**
   * Rolls a d20 on a plain table.
   *
   * @throws IllegalStateException for a legendary table
   */
  @NonNull D20TableRow roll(@NonNull RandomGenerator random) {
    if (!isD20Table()) {
      throw new IllegalStateException(title + " needs a weapon type or armor location");
    }
    return row(random.nextInt(D20TableRow.DIE_SIZE) + 1);
  }

  /** Whether the table is a plain d20 table with {@link #rows()}. */
  boolean isD20Table() {
    return this == TRINKETS || this == OUTCAST_EQUIPMENT;
  }

  private @NonNull D20TableRow row(int roll) {
    return this == TRINKETS ? EquipmentCatalog.trinket(roll) : EquipmentCatalog.outcastEquipment(roll);
  }
}
