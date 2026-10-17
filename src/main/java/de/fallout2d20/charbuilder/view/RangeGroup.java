package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponAttack;
import org.jspecify.annotations.NonNull;

/**
 * The groups of the weapon tree, from near to far: melee weapons, the four ranges of ranged weapons (CRB
 * p. 89) and the weapons with no range at all (mines that are only placed, custom weapons without range).
 */
enum RangeGroup implements WeaponTreeNode {
  MELEE("Melee"),
  CLOSE(WeaponRange.CLOSE.displayName()),
  MEDIUM(WeaponRange.MEDIUM.displayName()),
  LONG(WeaponRange.LONG.displayName()),
  EXTREME(WeaponRange.EXTREME.displayName()),
  NO_RANGE("No range");

  private final @NonNull String displayName;

  RangeGroup(@NonNull String displayName) {
    this.displayName = displayName;
  }

  @NonNull String displayName() {
    return displayName;
  }

  /** The group of an attack by its effective range; melee and unarmed weapons without one are Melee. */
  static @NonNull RangeGroup of(@NonNull WeaponAttack attack) {
    WeaponRange range = attack.effectiveRange();
    if (range != null) {
      return switch (range) {
        case CLOSE -> CLOSE;
        case MEDIUM -> MEDIUM;
        case LONG -> LONG;
        case EXTREME -> EXTREME;
      };
    }
    return attack.skill() == Skill.MELEE_WEAPONS || attack.skill() == Skill.UNARMED ? MELEE : NO_RANGE;
  }
}
