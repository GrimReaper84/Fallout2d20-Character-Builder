package de.fallout2d20.charbuilder.view.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Picks the mod for one slot (weapon or apparel): a table of the mods that fit plus "No mod" (named in the first
 * column, the others empty) to remove it. Perks
 * the character lacks are marked, never blocking (user decision 2026-10-17: installing is rolled at the table,
 * found equipment is often modded already).
 */
final class ModPickerDialog {

  private static final @NonNull String NO_MOD = "No mod";

  private ModPickerDialog() {}

  /** Shows the dialog with {@code current} selected; empty if cancelled. */
  static <T> @NonNull Optional<ModChoice<T>> show(@Nullable Window owner, @NonNull String title,
      @NonNull String header, @NonNull List<T> options, @Nullable T current, @NonNull List<ModColumn<T>> columns,
      @NonNull Function<T, String> details, @NonNull Predicate<T> lacksPerk) {
    List<ModChoice<T>> rows = new ArrayList<>();
    rows.add(new ModChoice<>(null));
    options.forEach(mod -> rows.add(new ModChoice<>(mod)));
    TableView<ModChoice<T>> table = new TableView<>();
    table.getItems().setAll(rows);
    for (int i = 0; i < columns.size(); i++) {
      table.getColumns().add(column(columns.get(i), i == 0 ? NO_MOD : ""));
    }
    table.setRowFactory(_ -> new ModChoiceRow<>(details, lacksPerk));
    table.setPrefSize(900, 380);
    rows.stream().filter(row -> Objects.equals(row.mod(), current)).findFirst()
        .ifPresent(row -> table.getSelectionModel().select(row));

    Dialog<ModChoice<T>> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle(title);
    dialog.setHeaderText(header);
    dialog.getDialogPane().setContent(table);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dialog.getDialogPane().lookupButton(ButtonType.OK)
        .disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
    dialog.setResultConverter(button -> button == ButtonType.OK ? table.getSelectionModel().getSelectedItem() : null);
    return dialog.showAndWait();
  }

  private static <T> @NonNull TableColumn<ModChoice<T>, String> column(@NonNull ModColumn<T> spec,
      @NonNull String noModText) {
    TableColumn<ModChoice<T>, String> column = new TableColumn<>(spec.title());
    column.setPrefWidth(spec.width());
    column.setCellValueFactory(cell -> {
      T mod = cell.getValue().mod();
      return new ReadOnlyStringWrapper(mod != null ? spec.text().apply(mod) : noModText);
    });
    return column;
  }
}
