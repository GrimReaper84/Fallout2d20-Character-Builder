package de.fallout2d20.charbuilder.model.character.apparel;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelModData;
import de.fallout2d20.charbuilder.model.character.derived.Weight;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The values of a piece of apparel with its mods applied, computed on every call (never stored). The mods' damage
 * resistances add to the piece's own; the higher value of clothing and armor on a location (CRB p. 130) is then
 * taken from these modded values ({@link ArmorResistances}). A radiation-immune piece stays immune. Weight and cost
 * add the mods' printed values, each counted {@link ApparelMods#factor} times (doubled on torso armor, CRB pp.
 * 133–135); a {@code <1} piece stays {@code <1} unless the mods change the weight, which never drops below 0 (WG
 * p. 93: Shrouded weighs −1); a custom piece has no weight or cost of
 * its own, only its mods'.
 *
 * @param values kind, resistances and carry weight change with the mods
 * @param weight weight as printed ({@code 4}, {@code <1}); {@code null} for none
 * @param cost cost in caps; {@code null} for none
 * @param specials rules of the mods that change no value, each with the mod's name
 * @param shadowed whether a mod makes the piece Shadowed (CRB p. 133 sidebar)
 */
public record ModdedApparel(
    @NonNull ApparelValues values,
    @Nullable String weight,
    @Nullable Integer cost,
    @NonNull List<String> specials,
    boolean shadowed) {

  public ModdedApparel {
    specials = List.copyOf(specials);
  }

  /** The piece with its installed mods that fit; empty for an id outside the catalog. */
  public static @NonNull Optional<ModdedApparel> of(@NonNull WornApparel item) {
    return unmodded(item).map(base -> base.apply(item, ApparelMods.installedMods(item)));
  }

  /** The piece's values without mods (also shown as the base value next to a changed one). */
  public static @NonNull Optional<ModdedApparel> unmodded(@NonNull WornApparel item) {
    CustomApparel custom = item.custom();
    if (custom != null) {
      return Optional.of(new ModdedApparel(custom.values(), null, null, List.of(), false));
    }
    return item.data().map(data -> new ModdedApparel(ApparelValues.of(data), data.weight(), data.cost(), List.of(),
        false));
  }

  private @NonNull ModdedApparel apply(@NonNull WornApparel item, @NonNull List<ApparelModData> mods) {
    if (mods.isEmpty()) {
      return this;
    }
    int physical = values.physical();
    int energy = values.energy();
    int radiation = values.radiation();
    Integer weightChange = null;
    Integer costChange = null;
    List<String> modSpecials = new ArrayList<>();
    boolean modShadowed = false;
    for (ApparelModData mod : mods) {
      physical += mod.physical();
      energy += mod.energy();
      radiation += mod.radiation();
      int factor = ApparelMods.factor(item, mod);
      Integer modWeight = mod.weight();
      if (modWeight != null) {
        weightChange = (weightChange == null ? 0 : weightChange) + modWeight * factor;
      }
      Integer modCost = mod.cost();
      if (modCost != null) {
        costChange = (costChange == null ? 0 : costChange) + modCost * factor;
      }
      String special = mod.special();
      if (special != null) {
        modSpecials.add(mod.name() + ": " + special);
      }
      modShadowed |= mod.shadowed();
    }
    ApparelValues modded = new ApparelValues(values.kind(), physical, energy,
        values.radiationImmune() ? 0 : radiation, values.radiationImmune(), values.carryWeight());
    String newWeight = weightChange == null || weightChange == 0 ? weight
        : Integer.toString(Math.max(0, Weight.pounds(weight) + weightChange));
    Integer newCost = costChange == null ? cost : (cost == null ? 0 : cost) + costChange;
    return new ModdedApparel(modded, newWeight, newCost, modSpecials, modShadowed);
  }
}
