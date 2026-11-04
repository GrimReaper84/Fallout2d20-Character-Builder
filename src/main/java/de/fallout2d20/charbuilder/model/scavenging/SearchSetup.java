package de.fallout2d20.charbuilder.model.scavenging;

import org.jspecify.annotations.NonNull;

/**
 * What the GM sets before a search; fixed for the whole search.
 *
 * @param items the item listing of the location
 * @param scale its size
 * @param degree how thoroughly it was searched before
 * @param level its level, the reach of Luck on a roll (CRB p. 200)
 * @param junkMode how junk rolls are resolved
 * @param rareBooks whether the Wanderer's Guide is enabled, whose Rare Books replace magazines for 1 Luck (WG p. 108)
 */
public record SearchSetup(@NonNull LocationItems items, @NonNull LocationScale scale, @NonNull LocationDegree degree,
    int level, @NonNull JunkMode junkMode, boolean rareBooks) {

  public SearchSetup {
    if (level < 0) {
      throw new IllegalArgumentException("A location's level cannot be negative");
    }
  }
}
