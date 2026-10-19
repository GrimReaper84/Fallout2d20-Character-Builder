package de.fallout2d20.charbuilder.view;

/** A node of the item tree in the Gear tab: a group or one item. */
sealed interface ItemTreeNode permits ItemGroup, ItemEntry {}
