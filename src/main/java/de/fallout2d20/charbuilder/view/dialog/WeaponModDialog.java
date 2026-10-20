package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Picks the mod for one slot of a weapon: the mods that fit, with effects, weight, cost, needed perks and
 * source, plus "No mod" to remove it. Perks the character lacks are marked, never blocking (user decision
 * 2026-10-17: installing is rolled at the table, found weapons are often modded already).
 */
public final class WeaponModDialog {

  private static final @NonNull String NO_MOD = "No mod";

  private WeaponModDialog() {}

  /** Shows the dialog with {@code current} selected; empty if cancelled. */
  public static @NonNull Optional<ModChoice> show(@Nullable Window owner, @NonNull PlayerCharacter character,
      @NonNull String weaponName, @NonNull ModSlot slot, @NonNull List<WeaponModData> options,
      @Nullable WeaponModData current) {
    List<ModChoice> rows = new ArrayList<>();
    rows.add(new ModChoice(null));
    options.forEach(mod -> rows.add(new ModChoice(mod)));
    TableView<ModChoice> table = new TableView<>();
    table.getItems().setAll(rows);
    table.getColumns().add(column("Mod", 200, NO_MOD, WeaponModData::name));
    table.getColumns().add(column("Effects", 300, "", ModTexts::effects));
    table.getColumns().add(column("Weight", 60, "", mod -> ModTexts.signed(mod.weight())));
    table.getColumns().add(column("Cost", 60, "", mod -> ModTexts.signed(mod.cost())));
    table.getColumns().add(column("Perks", 150, "", ModTexts::perks));
    table.getColumns().add(column("Source", 90, "", mod -> mod.sources().getFirst().toString()));
    table.setRowFactory(_ -> new ModChoiceRow(character));
    table.setPrefSize(900, 380);
    rows.stream().filter(row -> Objects.equals(row.mod(), current)).findFirst()
        .ifPresent(row -> table.getSelectionModel().select(row));

    Dialog<ModChoice> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle(slot.displayName() + " mod");
    dialog.setHeaderText("Choose the " + slot.displayName() + " mod of " + weaponName + ".");
    dialog.getDialogPane().setContent(table);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dialog.getDialogPane().lookupButton(ButtonType.OK)
        .disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
    dialog.setResultConverter(button -> button == ButtonType.OK ? table.getSelectionModel().getSelectedItem() : null);
    return dialog.showAndWait();
  }

  /** A text column; the "No mod" row shows {@code noModText}. */
  private static @NonNull TableColumn<ModChoice, String> column(@NonNull String title, double width,
      @NonNull String noModText, @NonNull Function<WeaponModData, String> text) {
    TableColumn<ModChoice, String> column = new TableColumn<>(title);
    column.setPrefWidth(width);
    column.setCellValueFactory(cell -> {
      WeaponModData mod = cell.getValue().mod();
      return new ReadOnlyStringWrapper(mod != null ? text.apply(mod) : noModText);
    });
    return column;
  }
}
