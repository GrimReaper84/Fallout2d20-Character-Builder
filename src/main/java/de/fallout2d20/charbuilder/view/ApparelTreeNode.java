package de.fallout2d20.charbuilder.view;

/** A node of the apparel tree in the Armor tab: a group or one piece. */
sealed interface ApparelTreeNode permits ApparelGroup, ApparelEntry {}
