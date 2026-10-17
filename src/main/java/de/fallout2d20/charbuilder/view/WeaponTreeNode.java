package de.fallout2d20.charbuilder.view;

/** A node of the weapon tree in the Weapons tab: a range group or one weapon. */
sealed interface WeaponTreeNode permits RangeGroup, WeaponEntry {}
