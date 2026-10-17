package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import java.util.Optional;
import java.util.Set;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Chooses the mod slots of a custom weapon after it was added (user decision 2026-10-17). */
public final class ModSlotsDialog {

  private ModSlotsDialog() {}

  /** Shows the dialog with the current slots ticked; empty if cancelled. */
  public static @NonNull Optional<Set<ModSlot>> show(@Nullable Window owner, @NonNull String weaponName,
      @NonNull Set<ModSlot> current) {
    ModSlotCheckBoxes boxes = new ModSlotCheckBoxes(current);
    Dialog<Set<ModSlot>> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle("Mod slots");
    dialog.setHeaderText("Which mod slots does " + weaponName + " have? Mods of unticked slots are removed.");
    dialog.getDialogPane().setContent(boxes.node());
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dialog.setResultConverter(button -> button == ButtonType.OK ? boxes.selected() : null);
    return dialog.showAndWait();
  }
}
