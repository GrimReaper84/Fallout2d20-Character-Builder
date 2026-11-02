package de.fallout2d20.charbuilder.model.character.weapon;

import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * The perks' effect on a weapon ({@link PerkWeaponBonuses}).
 *
 * @param changes the computed changes, one per perk, in the order of the character's perks
 * @param hints the situational bonuses, one per perk, in the order of the character's perks
 */
public record PerkWeaponResult(@NonNull List<PerkWeaponChange> changes, @NonNull List<PerkWeaponHint> hints) {

  public static final @NonNull PerkWeaponResult NONE = new PerkWeaponResult(List.of(), List.of());

  public PerkWeaponResult {
    changes = List.copyOf(changes);
    hints = List.copyOf(hints);
  }
}
