package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponMods;
import java.util.List;
import java.util.Optional;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Picks the mod for one slot of a weapon: the mods that fit, with effects, weight, cost, needed perks and
 * source, plus "No mod" to remove it ({@link ModPickerDialog}).
 */
public final class WeaponModDialog {

  private WeaponModDialog() {}

  /** Shows the dialog with {@code current} selected; empty if cancelled. */
  public static @NonNull Optional<ModChoice<WeaponModData>> show(@Nullable Window owner,
      @NonNull PlayerCharacter character, @NonNull String weaponName, @NonNull ModSlot slot,
      @NonNull List<WeaponModData> options, @Nullable WeaponModData current) {
    List<ModColumn<WeaponModData>> columns = List.of(
        new ModColumn<>("Mod", 200, WeaponModData::name),
        new ModColumn<>("Effects", 300, ModTexts::effects),
        new ModColumn<>("Weight", 60, mod -> ModTexts.signed(mod.weight())),
        new ModColumn<>("Cost", 60, mod -> ModTexts.signed(mod.cost())),
        new ModColumn<>("Perks", 150, mod -> ModTexts.perks(mod.perks())),
        new ModColumn<>("Source", 90, mod -> mod.sources().getFirst().toString()));
    return ModPickerDialog.show(owner, slot.displayName() + " mod",
        "Choose the " + slot.displayName() + " mod of " + weaponName + ".", options, current, columns,
        ModTexts::details, mod -> !WeaponMods.missingPerks(character, mod).isEmpty());
  }
}
