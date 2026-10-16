package de.fallout2d20.charbuilder.catalog.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;

/**
 * One row of a publication's issue table (CRB pp. 173–180).
 *
 * @param roll the d20 results, as printed, e.g. {@code 1-2}
 * @param title the issue title as printed
 * @param effect the temporary perk in short (own words)
 */
public record MagazineIssue(
    @JsonProperty(required = true) @NonNull String roll,
    @JsonProperty(required = true) @NonNull String title,
    @JsonProperty(required = true) @NonNull String effect) {

  public MagazineIssue {
    if (roll == null || roll.isBlank() || title == null || title.isBlank() || effect == null || effect.isBlank()) {
      throw new IllegalArgumentException("A magazine issue needs roll, title and effect: " + title);
    }
  }
}
