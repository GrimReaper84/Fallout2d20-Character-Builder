package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponMods;
import javafx.scene.control.TableRow;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A row of the mod dialog: the mod's details as tooltip, marked when the character lacks a needed perk. */
final class ModChoiceRow extends TableRow<ModChoice> {

  private static final @NonNull String MISSING_STYLE = "-fx-text-fill: #a35200;";

  private final @NonNull PlayerCharacter character;

  ModChoiceRow(@NonNull PlayerCharacter character) {
    this.character = character;
  }

  @Override
  protected void updateItem(@Nullable ModChoice choice, boolean empty) {
    super.updateItem(choice, empty);
    WeaponModData mod = empty || choice == null ? null : choice.mod();
    boolean missing = mod != null && !WeaponMods.missingPerks(character, mod).isEmpty();
    setStyle(missing ? MISSING_STYLE : "");
    if (mod == null) {
      setTooltip(null);
      return;
    }
    String note = missing ? "\nThe character lacks a needed perk (information only)." : "";
    setTooltip(new Tooltip(ModTexts.details(mod) + note));
  }
}
