package de.fallout2d20.charbuilder.catalog.weapon;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;

/**
 * A perk rank needed to install a mod (CRB p. 87), e.g. Gun Nut 2; information only (user decision
 * 2026-10-17).
 *
 * @param perk id of the perk in the perk catalog
 * @param rank the rank needed
 */
public record ModPerk(@JsonProperty(required = true) @NonNull String perk, @JsonProperty(required = true) int rank) {

  public ModPerk {
    if (perk == null || perk.isBlank() || rank < 1) {
      throw new IllegalArgumentException("A mod perk needs a perk id and a rank of at least 1");
    }
  }
}
