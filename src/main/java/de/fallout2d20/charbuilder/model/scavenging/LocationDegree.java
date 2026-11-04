package de.fallout2d20.charbuilder.model.scavenging;

import org.jspecify.annotations.NonNull;

/** How thoroughly a location was searched before; sets the difficulty of the search (CRB p. 196). */
public enum LocationDegree {
  UNTOUCHED("Untouched", 0),
  PARTLY_SEARCHED("Partly Searched", 1),
  MOSTLY_SEARCHED("Mostly Searched", 2),
  HEAVILY_SEARCHED("Heavily Searched", 3);

  private final @NonNull String displayName;
  private final int difficulty;

  LocationDegree(@NonNull String displayName, int difficulty) {
    this.displayName = displayName;
    this.difficulty = difficulty;
  }

  public @NonNull String displayName() {
    return displayName;
  }

  public int difficulty() {
    return difficulty;
  }
}
