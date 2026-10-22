package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelModData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModSlot;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.apparel.ApparelMods;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import java.util.List;
import java.util.Optional;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Picks the mod for one slot of a piece of apparel ({@link ModPickerDialog}); weight and cost as they count on
 * this piece (doubled on torso armor, CRB pp. 133–135).
 */
public final class ApparelModDialog {

  private ApparelModDialog() {}

  /** Shows the dialog with {@code current} selected; empty if cancelled. */
  public static @NonNull Optional<ModChoice<ApparelModData>> show(@Nullable Window owner,
      @NonNull PlayerCharacter character, @NonNull WornApparel item, @NonNull ApparelModSlot slot,
      @Nullable ApparelModData current) {
    List<ModColumn<ApparelModData>> columns = List.of(
        new ModColumn<>("Mod", 180, ApparelModData::name),
        new ModColumn<>("Physical", 70, mod -> ModTexts.bonus(mod.physical())),
        new ModColumn<>("Energy", 60, mod -> ModTexts.bonus(mod.energy())),
        new ModColumn<>("Radiation", 70, mod -> ModTexts.bonus(mod.radiation())),
        new ModColumn<>("Other effects", 260, ModTexts::otherEffects),
        new ModColumn<>("Weight", 60, mod -> ModTexts.signed(ModTexts.counted(item, mod, mod.weight()))),
        new ModColumn<>("Cost", 60, mod -> ModTexts.signed(ModTexts.counted(item, mod, mod.cost()))),
        new ModColumn<>("Perks", 140, mod -> ModTexts.perks(mod.perks())),
        new ModColumn<>("Source", 80, mod -> mod.sources().getFirst().toString()));
    return ModPickerDialog.show(owner, slot.displayName() + " mod",
        "Choose the " + slot.displayName() + " mod of " + item.displayName() + ".",
        ApparelMods.options(item, slot, character.getEnabledBooks()), current, columns,
        mod -> ModTexts.details(item, mod), mod -> !ApparelMods.missingPerks(character, mod).isEmpty());
  }
}
