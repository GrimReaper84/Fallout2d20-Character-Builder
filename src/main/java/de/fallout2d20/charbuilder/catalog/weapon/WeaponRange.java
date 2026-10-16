package de.fallout2d20.charbuilder.catalog.weapon;

import org.jspecify.annotations.NonNull;

/** The ideal range of a ranged weapon (CRB p. 89): the zone distance at which it is most effective. */
public enum WeaponRange {
  CLOSE("C", "Close"),
  MEDIUM("M", "Medium"),
  LONG("L", "Long"),
  EXTREME("X", "Extreme");

  private final @NonNull String code;
  private final @NonNull String displayName;

  WeaponRange(@NonNull String code, @NonNull String displayName) {
    this.code = code;
    this.displayName = displayName;
  }

  /** The letter used in the weapon tables, e.g. {@code C}. */
  public @NonNull String code() {
    return code;
  }

  public @NonNull String displayName() {
    return displayName;
  }
}
