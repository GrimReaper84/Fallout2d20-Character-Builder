package de.fallout2d20.charbuilder.model.character.weapon;

import de.fallout2d20.charbuilder.catalog.Source;
import org.jspecify.annotations.NonNull;

/**
 * A situational bonus of one perk for a weapon, shown with it and never computed ({@link PerkWeaponBonuses}).
 *
 * @param perk the perk's name, with the rank for perks of several ranks (e.g. {@code Nerd Rage! 2})
 * @param hint the bonus at that rank, e.g. {@code +2 CD on sneak attacks; not in Power Armor.}
 * @param source where the perk is described
 */
public record PerkWeaponHint(@NonNull String perk, @NonNull String hint, @NonNull Source source) {

  /** As shown to the player, e.g. {@code Ninja: +2 CD on sneak attacks; not in Power Armor.} */
  public @NonNull String text() {
    return perk + ": " + hint;
  }
}
