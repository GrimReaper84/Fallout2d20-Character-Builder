package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import org.jspecify.annotations.NonNull;

/**
 * One piece of the apparel tree with its list index and group.
 *
 * @param needsLocation whether its arm or leg still has to be chosen
 */
record ApparelEntry(int index, @NonNull WornApparel item, @NonNull ApparelGroup group, boolean needsLocation)
    implements ApparelTreeNode {

  /** Name, with the chosen limb of a single-limb piece, e.g. "Leather Arm (Left Arm)". */
  @NonNull String label() {
    HitLocation location = item.location();
    return item.displayName() + (location == null || needsLocation ? "" : " (" + location.displayName() + ")");
  }
}
