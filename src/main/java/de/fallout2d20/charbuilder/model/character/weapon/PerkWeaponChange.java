package de.fallout2d20.charbuilder.model.character.weapon;

import de.fallout2d20.charbuilder.catalog.Source;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * The changes of one perk to a weapon, its entries added up ({@link PerkWeaponBonuses}).
 *
 * @param perk the perk's name, with the rank for perks of several ranks (e.g. {@code Commando 2})
 * @param damage CD added
 * @param piercing Piercing gained or added to an existing rating
 * @param fireRate fire rate added
 * @param gainedEffects damage effects gained
 * @param removedQualities qualities lost
 * @param source where the perk is described
 */
public record PerkWeaponChange(
    @NonNull String perk,
    int damage,
    int piercing,
    int fireRate,
    @NonNull List<String> gainedEffects,
    @NonNull List<String> removedQualities,
    @NonNull Source source) {

  public PerkWeaponChange {
    gainedEffects = List.copyOf(gainedEffects);
    removedQualities = List.copyOf(removedQualities);
  }

  /** As shown to the player, e.g. {@code Rifleman 2: +2 CD, Piercing +1}. */
  public @NonNull String text() {
    List<String> parts = new ArrayList<>();
    if (damage > 0) {
      parts.add("+" + damage + " CD");
    }
    if (piercing > 0) {
      parts.add("Piercing +" + piercing);
    }
    if (fireRate > 0) {
      parts.add("+" + fireRate + " Fire Rate");
    }
    parts.addAll(gainedEffects);
    removedQualities.forEach(quality -> parts.add("not " + quality));
    return perk + ": " + String.join(", ", parts);
  }
}
