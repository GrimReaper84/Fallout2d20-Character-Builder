package de.fallout2d20.charbuilder.catalog.loot;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A row of a loot table.
 *
 * @param from the lowest roll of the row
 * @param to the highest roll of the row
 * @param name the result as printed; a {@code {}} stands for the rolled quantity (e.g. caps)
 * @param quantity how many are found: a number, {@code 5d20} or {@code 6+3 CD}, with an optional {@code x10};
 *     {@code null} for one
 * @param note a short remark in own words
 * @param ref the catalog entry, {@code null} for results without one (Power Armor, dog armor, notes, keys)
 * @param next the table to roll on next, for rows that only send to another table
 * @param rollsLocation whether the result is one piece, whose location is rolled (CRB p. 201)
 * @param pieces the id prefix of the armor's pieces in the apparel catalog; a location without a piece is rolled again
 */
public record LootRow(
    @JsonProperty(required = true) int from,
    @JsonProperty(required = true) int to,
    @JsonProperty(required = true) @NonNull String name,
    @Nullable String quantity,
    @Nullable String note,
    @Nullable LootRef ref,
    @Nullable String next,
    boolean rollsLocation,
    @Nullable String pieces) {

  /** The placeholder of the rolled quantity in a name. */
  public static final @NonNull String QUANTITY_MARK = "{}";

  public LootRow {
    if (from > to || name == null || name.isBlank()) {
      throw new IllegalArgumentException("Invalid loot row " + from + "-" + to + ": " + name);
    }
  }

  public boolean covers(int roll) {
    return from <= roll && roll <= to;
  }

  /** The row as the book shows it, with the formula in place of the result, e.g. {@code 10mm (8+4 CD)}. */
  public @NonNull String label() {
    if (quantity == null) {
      return name;
    }
    return name.contains(QUANTITY_MARK) ? name.replace(QUANTITY_MARK, quantity) : name + " (" + quantity + ")";
  }
}
