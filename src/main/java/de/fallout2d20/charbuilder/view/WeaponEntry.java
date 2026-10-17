package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponAttack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One weapon of the weapon tree: a carried weapon with its list index, or the Unarmed Strike every
 * character has (CRB p. 111; index {@link WeaponsTab#UNARMED_STRIKE_INDEX}, no carried weapon).
 */
record WeaponEntry(int index, @NonNull WeaponAttack attack, @Nullable CarriedWeapon weapon) implements WeaponTreeNode {

  /** Name, the quantity of counted weapons and a marker for weapons built into the character. */
  @NonNull String label() {
    if (weapon == null) {
      return attack.name();
    }
    return attack.name() + (weapon.isCounted() ? " ×" + weapon.quantity() : "")
        + (weapon.isIntegrated() ? " (built in)" : "");
  }

  /** A custom weapon imported without skill, whose skill still has to be chosen. */
  boolean needsSkill() {
    return weapon != null && weapon.custom() != null && attack.skill() == null;
  }
}
