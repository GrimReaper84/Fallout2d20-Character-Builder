package de.fallout2d20.charbuilder.model.character.item;

import org.jspecify.annotations.NonNull;

/**
 * Junk items and crafting materials (CRB p. 208), only counted: a junk item weighs 2 lbs. and is worth 2
 * caps; a unit of material weighs 1 lb. and is worth 1 (common), 3 (uncommon) or 5 caps (rare).
 *
 * @param junk junk items
 * @param common units of common materials
 * @param uncommon units of uncommon materials
 * @param rare units of rare materials
 */
public record Materials(int junk, int common, int uncommon, int rare) {

  /** CRB p. 208: weight of one junk item in lbs. */
  public static final int JUNK_WEIGHT = 2;
  /** CRB p. 208: weight of one unit of material in lbs. */
  public static final int MATERIAL_WEIGHT = 1;

  public static final @NonNull Materials NONE = new Materials(0, 0, 0, 0);

  public Materials {
    if (junk < 0 || common < 0 || uncommon < 0 || rare < 0) {
      throw new IllegalArgumentException("Junk and materials cannot be negative");
    }
  }

  /** Weight in lbs. of everything counted here. */
  public int weight() {
    return junk * JUNK_WEIGHT + (common + uncommon + rare) * MATERIAL_WEIGHT;
  }
}
