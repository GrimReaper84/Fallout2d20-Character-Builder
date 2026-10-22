package de.fallout2d20.charbuilder.model.character.weapon;

import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * The weapons a character's origin trait gives every character of that origin, such as the Assaultron's Claws, Head
 * Laser and Self-Destruct (WG p. 11). They are not carried: they are shown like the Unarmed Strike, weigh nothing
 * and cannot be removed or added by hand (user decision 2026-10-22).
 */
public final class InnateWeapons {

  private InnateWeapons() {}

  /**
   * The origin's innate weapons in the origin's order; none for other origins. They follow the origin alone, not
   * the enabled books, so a trait never shows only some of its weapons.
   */
  public static @NonNull List<WeaponData> of(@NonNull PlayerCharacter character) {
    return character.getOriginData().map(OriginData::innateWeapons).orElse(List.of()).stream()
        .map(WeaponCatalog::byId)
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * The ammunition types the character's weapons use: those of the carried weapons (with their mods), then those of
   * the innate weapons, in that order without repeats; a new set the caller may extend.
   */
  public static @NonNull Set<String> ammunitionUsed(@NonNull PlayerCharacter character) {
    Set<String> ids = new LinkedHashSet<>();
    character.gear().getWeapons().forEach(weapon -> ids.addAll(WeaponAttack.of(character, weapon).ammunition()));
    of(character).forEach(weapon -> ids.addAll(weapon.ammunition()));
    return ids;
  }
}
