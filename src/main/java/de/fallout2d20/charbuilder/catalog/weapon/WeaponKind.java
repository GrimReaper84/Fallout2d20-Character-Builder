package de.fallout2d20.charbuilder.catalog.weapon;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Kinds of weapons that perks name but the weapon tables do not mark: the catalog lists them per weapon (and per mod
 * that turns a weapon into one, e.g. a Flamer on the Chainsaw); a custom weapon can be given them.
 */
public enum WeaponKind {
  /** Shotgun Surgeon (CRB p. 71). */
  SHOTGUN("Shotgun"),
  /** Bladed melee weapons for Piercing Strike (CRB p. 70). */
  BLADED("Bladed"),
  /** Pipe weapons for Licensed Plumber (WG p. 50). */
  PIPE("Pipe"),
  /** Fire-based weapons for Pyromaniac (CRB p. 70); WG p. 47 names Flamers and Molotov Cocktails as such. */
  FIRE_BASED("Fire-based");

  private final @NonNull String displayName;

  WeaponKind(@NonNull String displayName) {
    this.displayName = displayName;
  }

  public @NonNull String displayName() {
    return displayName;
  }

  /** An unmodifiable copy in declaration order; {@code null} (a missing JSON field) is none. */
  public static @NonNull Set<WeaponKind> copyOf(@Nullable Collection<WeaponKind> kinds) {
    return kinds == null || kinds.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(kinds));
  }
}
