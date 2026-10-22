package de.fallout2d20.charbuilder.model.character.apparel;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import org.jspecify.annotations.NonNull;

/**
 * Shadowed armor (CRB p. 133 sidebar): the worn pieces with a Shadowed mod, a torso piece counting as two, give a
 * Sneak bonus in dim light or darkness that grows at one, three and five pieces. Shadowed catalog pieces count as
 * many pieces as their entry says (WG p. 86: Hunter's Pelt Outfit as four). The book's exclusion inside Power
 * Armor does not arise yet (no Power Armor, D3).
 */
public final class ShadowedArmor {

  private ShadowedArmor() {}

  /**
   * The number of worn Shadowed pieces, a modded piece covering the torso counting as two, a Shadowed catalog piece
   * as its {@link ApparelData#shadowedPieces}.
   */
  public static int count(@NonNull PlayerCharacter character) {
    int count = 0;
    for (WornApparel item : character.gear().getWornApparel()) {
      if (ModdedApparel.of(item).map(ModdedApparel::shadowed).orElse(false)) {
        count += ApparelMods.reach(item).contains(HitLocation.TORSO) ? 2 : 1;
      } else {
        count += item.data().map(ApparelData::shadowedPieces).orElse(0);
      }
    }
    return count;
  }

  /** The bonus reached with {@code count} pieces, in own words; empty for none. */
  public static @NonNull String effect(int count) {
    if (count >= 5) {
      return "Re-roll one d20 on any Sneak test in dim light or darkness.";
    }
    if (count >= 3) {
      return "Re-roll one d20 on one Sneak test per scene in dim light or darkness.";
    }
    if (count >= 1) {
      return "Ignore the first complication of a Sneak test in dim light or darkness.";
    }
    return "";
  }
}
