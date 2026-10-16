package de.fallout2d20.charbuilder.catalog.item;

import org.jspecify.annotations.NonNull;

/** CRB p. 164: how long a chem works – at once, until the end of the next turn, or the scene. */
public enum ChemDuration {
  INSTANT("Instant"),
  BRIEF("Brief"),
  LASTING("Lasting");

  private final @NonNull String displayName;

  ChemDuration(@NonNull String displayName) {
    this.displayName = displayName;
  }

  public @NonNull String displayName() {
    return displayName;
  }
}
