package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.weapon.ModEffect;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponMods;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** How a weapon mod is described to the player in the Weapons tab and the mod dialog. */
public final class ModTexts {

  private ModTexts() {}

  /** The effects in table order, e.g. "+1 CD damage, Range +1 step"; a dash for none. */
  public static @NonNull String effects(@NonNull WeaponModData mod) {
    return mod.effects().isEmpty() ? "–"
        : mod.effects().stream().map(ModEffect::description).collect(Collectors.joining(", "));
  }

  /** Effects, weight, cost, perks, skill and source, one per line (tooltip of an installed mod). */
  public static @NonNull String details(@NonNull WeaponModData mod) {
    return mod.name() + " (" + mod.tableName() + ")"
        + "\n" + effects(mod)
        + "\nWeight " + signed(mod.weight()) + " lbs., cost " + signed(mod.cost()) + " caps"
        + "\nPerks: " + perks(mod) + "; installed with " + mod.skill().displayName()
        + "\n" + mod.sources().getFirst();
  }

  /** The needed perk ranks, e.g. "Gun Nut 2, Science! 1"; a dash for none. */
  public static @NonNull String perks(@NonNull WeaponModData mod) {
    return mod.perks().isEmpty() ? "–"
        : mod.perks().stream().map(WeaponMods::perkText).collect(Collectors.joining(", "));
  }

  /** A signed number as in the mod tables, e.g. "+2", "-1"; a dash for none. */
  public static @NonNull String signed(@Nullable Integer value) {
    if (value == null) {
      return "–";
    }
    return value > 0 ? "+" + value : value.toString();
  }
}
