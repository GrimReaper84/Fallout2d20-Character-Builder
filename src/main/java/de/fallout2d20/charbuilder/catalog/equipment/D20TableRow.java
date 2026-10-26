package de.fallout2d20.charbuilder.catalog.equipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Source;
import org.jspecify.annotations.NonNull;

/**
 * A row of a d20 table of the starting equipment (catalog entry): the random trinket table (CRB p. 80: a personal
 * trinket may be invented or rolled) and the Outcast Equipment Table (WG p. 19: rolls of the Ex-Knight and Ex-Scribe
 * packs).
 *
 * @param roll the d20 result, 1 to 20
 * @param name the trinket or equipment; Outcast rows are worded so the pack matching finds catalog entries (a leading
 *     count is the quantity)
 * @param source book and page of the table
 */
public record D20TableRow(
    @JsonProperty(required = true) int roll,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull Source source) {

  /** The tables are rolled with one d20. */
  public static final int DIE_SIZE = 20;

  public D20TableRow {
    if (roll < 1 || roll > DIE_SIZE || name == null || name.isBlank() || source == null) {
      throw new IllegalArgumentException("Invalid d20 table row " + roll);
    }
  }
}
