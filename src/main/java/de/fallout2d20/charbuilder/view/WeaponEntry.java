package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponAttack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One weapon of the weapon tree: a carried weapon with its list index, or a weapon the character has without
 * carrying it – the Unarmed Strike every character has (CRB p. 111; index
 * {@link WeaponsTab#UNARMED_STRIKE_INDEX}) or a weapon of the origin's trait (WG p. 11, negative indices below it,
 * {@link de.fallout2d20.charbuilder.model.character.weapon.InnateWeapons}) – with its catalog row as {@code fixed}.
 */
record WeaponEntry(int index, @NonNull WeaponAttack attack, @Nullable CarriedWeapon weapon, @Nullable WeaponData fixed)
    implements WeaponTreeNode {

  /** A carried weapon. */
  WeaponEntry(int index, @NonNull WeaponAttack attack, @NonNull CarriedWeapon weapon) {
    this(index, attack, weapon, null);
  }

  /**
   * Whether the weapon is built into the character: a carried built-in weapon or one of the origin's trait, whose
   * entries have the indices below the Unarmed Strike's.
   */
  boolean isBuiltIn() {
    return weapon != null ? weapon.isIntegrated() : index < WeaponsTab.UNARMED_STRIKE_INDEX;
  }

  /**
   * Name (with the legendary mark and property), the quantity of counted weapons and a marker for weapons built into
   * the character.
   */
  @NonNull String label() {
    if (weapon == null) {
      return attack.name() + (isBuiltIn() ? " (built in)" : "");
    }
    return weapon.legendaryName() + (weapon.isCounted() ? " ×" + weapon.quantity() : "")
        + (weapon.isIntegrated() ? " (built in)" : "");
  }

  /** A custom weapon imported without skill, whose skill still has to be chosen. */
  boolean needsSkill() {
    return weapon != null && weapon.custom() != null && attack.skill() == null;
  }
}
