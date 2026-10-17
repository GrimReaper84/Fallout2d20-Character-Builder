package de.fallout2d20.charbuilder.catalog.weapon;

import java.util.Arrays;
import java.util.Optional;
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

  /** The range with the letter of the weapon tables, e.g. {@code M}; empty for any other text. */
  public static @NonNull Optional<WeaponRange> byCode(@NonNull String code) {
    return Arrays.stream(values()).filter(range -> range.code.equals(code)).findFirst();
  }
}
