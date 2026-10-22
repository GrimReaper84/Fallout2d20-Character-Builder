package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelModData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModTable;
import de.fallout2d20.charbuilder.catalog.apparel.ModPlacement;
import de.fallout2d20.charbuilder.catalog.weapon.ModEffect;
import de.fallout2d20.charbuilder.catalog.weapon.ModPerk;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import de.fallout2d20.charbuilder.model.character.apparel.ApparelMods;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponMods;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** How weapon and apparel mods are described to the player in the Weapons and Armor tabs and the mod dialogs. */
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
        + "\nWeight " + amount(mod.weight(), " lbs.") + ", cost " + amount(mod.cost(), " caps")
        + "\nPerks: " + perks(mod.perks()) + "; installed with " + mod.skill().displayName()
        + "\n" + mod.sources().getFirst();
  }

  /** The needed perk ranks, e.g. "Gun Nut 2, Science! 1"; a dash for none. */
  public static @NonNull String perks(@NonNull List<ModPerk> perks) {
    return perks.isEmpty() ? "–" : perks.stream().map(WeaponMods::perkText).collect(Collectors.joining(", "));
  }

  /** All effects of an apparel mod, e.g. "+3 Physical, +3 Energy, Shadowed"; a dash for none. */
  public static @NonNull String effects(@NonNull ApparelModData mod) {
    List<String> effects = new ArrayList<>();
    resistance(effects, mod.physical(), "Physical");
    resistance(effects, mod.energy(), "Energy");
    resistance(effects, mod.radiation(), "Radiation");
    String other = otherEffects(mod);
    if (!other.equals("–")) {
      effects.add(other);
    }
    return effects.isEmpty() ? "–" : String.join(", ", effects);
  }

  /** The effects of an apparel mod besides damage resistance: its rule and Shadowed; a dash for none. */
  public static @NonNull String otherEffects(@NonNull ApparelModData mod) {
    String special = mod.special();
    if (mod.shadowed()) {
      return special == null ? "Shadowed (CRB p. 133)" : "Shadowed (CRB p. 133); " + special;
    }
    return special == null ? "–" : special;
  }

  /** Effects, weight and cost on this piece, perks and source, one per line (tooltip of an apparel mod). */
  public static @NonNull String details(@NonNull WornApparel item, @NonNull ApparelModData mod) {
    ApparelModTable table = mod.table();
    ModPlacement placement = mod.placement();
    String origin = table != null ? table.displayName()
        : "Armor Upgrade" + (placement == null ? "" : ", " + placement.displayName().toLowerCase(Locale.ROOT));
    String doubled = ApparelMods.factor(item, mod) > 1 ? " (doubled on torso armor)" : "";
    return mod.name() + " (" + origin + ")"
        + "\n" + effects(mod)
        + "\nWeight " + amount(counted(item, mod, mod.weight()), " lbs.") + ", cost "
        + amount(counted(item, mod, mod.cost()), " caps") + doubled
        + "\nPerks: " + perks(mod.perks()) + "; installed with Repair"
        + "\n" + mod.sources().getFirst();
  }

  /** A printed weight or cost as it counts on this piece (doubled on torso armor); {@code null} for none. */
  public static @Nullable Integer counted(@NonNull WornApparel item, @NonNull ApparelModData mod,
      @Nullable Integer printed) {
    return printed == null ? null : printed * ApparelMods.factor(item, mod);
  }

  /** A signed amount with its unit, e.g. "+4 lbs."; a dash without unit for none. */
  private static @NonNull String amount(@Nullable Integer value, @NonNull String unit) {
    return value == null ? "–" : signed(value) + unit;
  }

  /** A damage resistance bonus as in the tables, e.g. "+2"; a dash for none. */
  public static @NonNull String bonus(int value) {
    return value == 0 ? "–" : "+" + value;
  }

  private static void resistance(@NonNull List<String> effects, int value, @NonNull String type) {
    if (value != 0) {
      effects.add("+" + value + " " + type);
    }
  }

  /** A signed number as in the mod tables, e.g. "+2", "-1"; a dash for none. */
  public static @NonNull String signed(@Nullable Integer value) {
    if (value == null) {
      return "–";
    }
    return value > 0 ? "+" + value : value.toString();
  }
}
