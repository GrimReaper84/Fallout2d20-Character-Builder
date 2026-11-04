package de.fallout2d20.charbuilder.catalog.loot;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A loot table: CRB pp. 200-208 (2d20, Oddities 3d20), publications CRB p. 172, rare books WG p. 108, junk tables WG
 * pp. 120-125 (d20). The rows must cover every possible roll without gaps; only rolls the book itself leaves out are
 * listed in {@code missingRolls}.
 *
 * @param id stable id, also the target of {@link LootRow#next()}
 * @param name the table's name as printed
 * @param source book and page
 * @param dice how many d20 are rolled and added
 * @param note a short remark on the whole table, in own words
 * @param missingRolls rolls without a row in the book (Random Ranged Weapons has no 10 and no 19, CRB p. 204)
 * @param rows the rows in rising order
 */
public record LootTable(
    @JsonProperty(required = true) @NonNull String id,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull Source source,
    @JsonProperty(required = true) int dice,
    @Nullable String note,
    @Nullable List<Integer> missingRolls,
    @JsonProperty(required = true) @NonNull List<LootRow> rows) {

  /** The faces of the dice of every loot table. */
  private static final int DIE = 20;

  public LootTable {
    if (id == null || id.isBlank() || dice < 1 || rows == null || rows.isEmpty()) {
      throw new IllegalArgumentException("Invalid loot table " + id);
    }
    missingRolls = missingRolls == null ? List.of() : List.copyOf(missingRolls);
    rows = List.copyOf(rows);
    int expected = dice;
    for (LootRow row : rows) {
      while (missingRolls.contains(expected)) {
        expected++;
      }
      if (row.from() != expected) {
        throw new IllegalArgumentException("Loot table " + id + " has a gap at " + expected);
      }
      expected = row.to() + 1;
    }
    if (expected != dice * DIE + 1) {
      throw new IllegalArgumentException("Loot table " + id + " ends at " + (expected - 1));
    }
  }

  public int minRoll() {
    return dice;
  }

  public int maxRoll() {
    return dice * DIE;
  }

  /** How the table is rolled, e.g. {@code 2d20}. */
  public @NonNull String diceText() {
    return dice == 1 ? "d20" : dice + "d20";
  }

  /** The row of this roll; empty for a roll outside the table or one the book leaves out. */
  public @NonNull Optional<LootRow> row(int roll) {
    return rows.stream().filter(row -> row.covers(roll)).findFirst();
  }
}
