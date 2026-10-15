package de.fallout2d20.charbuilder.catalog.equipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Source;
import org.jspecify.annotations.NonNull;

/**
 * A row of the random trinket table (catalog entry). CRB p. 80: a personal trinket may be invented or
 * rolled on this d20 table.
 *
 * @param roll the d20 result, 1 to 20
 * @param name the trinket
 */
public record TrinketData(
    @JsonProperty(required = true) int roll,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull Source source) {

  /** The table is rolled with one d20. */
  public static final int DIE_SIZE = 20;

  public TrinketData {
    if (roll < 1 || roll > DIE_SIZE || name == null || name.isBlank() || source == null) {
      throw new IllegalArgumentException("Invalid trinket table row " + roll);
    }
  }
}
