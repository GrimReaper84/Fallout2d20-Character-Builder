package de.fallout2d20.charbuilder.catalog.legendary;

import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The columns of the Legendary Weapons tables (WG pp. 97–98): only these weapon types can be legendary; bows,
 * explosives and throwing weapons cannot (WG p. 96).
 */
public enum LegendaryWeaponType {
  SMALL_GUNS("Small Guns"),
  ENERGY_WEAPONS("Energy Weapons"),
  BIG_GUNS("Big Guns"),
  MELEE("Melee Weapons"),
  UNARMED("Unarmed Weapons");

  private final @NonNull String displayName;

  LegendaryWeaponType(@NonNull String displayName) {
    this.displayName = displayName;
  }

  /** Column title as printed. */
  public @NonNull String displayName() {
    return displayName;
  }

  /**
   * The column of a weapon by its attack skill, with the unusual weapons of WG p. 96: a Small Gun with the Blast
   * quality rolls as a Big Gun, a machine gun, Gatling Gun or Minigun ({@code asSmallGun}) as a Small Gun. Empty for
   * every other skill (bows use Athletics, explosives, throwing weapons) and an unknown one.
   *
   * @param qualities the weapon's qualities as printed
   * @param asSmallGun whether the catalog marks the weapon to roll as a Small Gun
   */
  public static @NonNull Optional<LegendaryWeaponType> of(@Nullable Skill skill, @NonNull List<String> qualities,
      boolean asSmallGun) {
    if (skill == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(switch (skill) {
      case SMALL_GUNS -> hasBlast(qualities) ? BIG_GUNS : SMALL_GUNS;
      case ENERGY_WEAPONS -> ENERGY_WEAPONS;
      case BIG_GUNS -> asSmallGun ? SMALL_GUNS : BIG_GUNS;
      case MELEE_WEAPONS -> MELEE;
      case UNARMED -> UNARMED;
      default -> null;
    });
  }

  private static boolean hasBlast(@NonNull List<String> qualities) {
    return qualities.stream().anyMatch(quality -> quality.strip().toLowerCase(Locale.ROOT).equals("blast"));
  }
}
