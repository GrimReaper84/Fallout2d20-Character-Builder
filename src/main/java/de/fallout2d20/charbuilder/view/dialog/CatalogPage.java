package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.view.Layouts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The catalog tab of an add dialog: a category combo box with the groups of the tab's tree ("All" first,
 * only groups that hold entries, in tree order), a search field for the name and a table sorted by name
 * (case-insensitive) that re-sorts on a click on a column header. Each row has the entry's long values as
 * tooltip; a double click on a row or Enter runs {@link #setOnActivate}. The dialog defines the columns.
 *
 * @param <T> the catalog entry type
 */
final class CatalogPage<T> {

  static final @NonNull String ALL = "All";

  private static final double TABLE_WIDTH = 720;
  private static final double TABLE_HEIGHT = 380;

  private final @NonNull TableView<T> table;
  private final @NonNull VBox node;
  private @NonNull Runnable onActivate = () -> { };

  /**
   * @param entries the entries to offer
   * @param name the name shown in the first column and searched
   * @param categoryOf the tree group of an entry
   * @param categories the tree groups in tree order
   * @param tooltip the row tooltip of an entry; empty for none
   */
  CatalogPage(@NonNull List<T> entries, @NonNull Function<T, String> name, @NonNull Function<T, String> categoryOf,
      @NonNull List<String> categories, @NonNull Function<T, String> tooltip) {
    List<T> alphabetical = new ArrayList<>(entries);
    alphabetical.sort(Comparator.comparing(name, String.CASE_INSENSITIVE_ORDER));
    FilteredList<T> filtered = new FilteredList<>(FXCollections.observableArrayList(alphabetical));
    SortedList<T> sorted = new SortedList<>(filtered);
    table = new TableView<>(sorted);
    sorted.comparatorProperty().bind(table.comparatorProperty());
    table.setPrefSize(TABLE_WIDTH, TABLE_HEIGHT);
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.setPlaceholder(new Label("Nothing matches the category and search."));
    TableColumn<T, String> nameColumn = textColumn("Name", name);
    nameColumn.setComparator(String.CASE_INSENSITIVE_ORDER);
    table.getColumns().add(nameColumn);
    table.getSortOrder().add(nameColumn);
    table.setRowFactory(_ -> new TooltipRow<>(tooltip, () -> onActivate.run()));
    table.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ENTER && selected() != null) {
        event.consume();
        onActivate.run();
      }
    });

    ComboBox<String> category = new ComboBox<>(FXCollections.observableArrayList(present(entries, categoryOf,
        categories)));
    category.setValue(ALL);
    TextField search = new TextField();
    search.setPromptText("Search");
    HBox.setHgrow(search, Priority.ALWAYS);
    Runnable filter = () -> {
      String key = search.getText().strip().toLowerCase(Locale.ROOT);
      String chosen = category.getValue();
      filtered.setPredicate(entry -> name.apply(entry).toLowerCase(Locale.ROOT).contains(key)
          && (chosen == null || chosen.equals(ALL) || chosen.equals(categoryOf.apply(entry))));
    };
    search.textProperty().addListener((_, _, _) -> filter.run());
    category.valueProperty().addListener((_, _, _) -> filter.run());
    HBox filters = new HBox(Layouts.GAP, new Label("Category"), category, search);
    filters.setAlignment(Pos.CENTER_LEFT);
    node = new VBox(Layouts.GAP, filters, table);
    VBox.setVgrow(table, Priority.ALWAYS);
  }

  /** Adds a column with the entry's text, sorted alphabetically. */
  void addColumn(@NonNull String header, @NonNull Function<T, String> text) {
    TableColumn<T, String> column = textColumn(header, text);
    column.setComparator(String.CASE_INSENSITIVE_ORDER);
    table.getColumns().add(column);
  }

  /** Adds a column showing {@code text} but sorted by {@code number} (damage, weight, resistances). */
  void addNumberColumn(@NonNull String header, @NonNull ToIntFunction<T> number, @NonNull Function<T, String> text) {
    TableColumn<T, T> column = new TableColumn<>(header);
    column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
    column.setComparator(Comparator.comparingInt(number));
    column.setCellFactory(_ -> new TableCell<>() {
      @Override
      protected void updateItem(@Nullable T entry, boolean empty) {
        super.updateItem(entry, empty);
        setText(empty || entry == null ? null : text.apply(entry));
      }
    });
    table.getColumns().add(column);
  }

  /** Runs when an entry is double-clicked or Enter is pressed on it (the dialog adds it). */
  void setOnActivate(@NonNull Runnable action) {
    onActivate = action;
  }

  @NonNull VBox node() {
    return node;
  }

  @NonNull ReadOnlyObjectProperty<T> selectedItemProperty() {
    return table.getSelectionModel().selectedItemProperty();
  }

  @Nullable T selected() {
    return table.getSelectionModel().getSelectedItem();
  }

  private static <T> @NonNull TableColumn<T, String> textColumn(@NonNull String header,
      @NonNull Function<T, String> text) {
    TableColumn<T, String> column = new TableColumn<>(header);
    column.setCellValueFactory(cell -> new SimpleStringProperty(text.apply(cell.getValue())));
    return column;
  }

  /** "All" and the categories that hold entries, in the given order. */
  private static <T> @NonNull List<String> present(@NonNull List<T> entries, @NonNull Function<T, String> categoryOf,
      @NonNull List<String> categories) {
    Set<String> used = new LinkedHashSet<>();
    entries.forEach(entry -> used.add(categoryOf.apply(entry)));
    List<String> shown = new ArrayList<>(List.of(ALL));
    categories.stream().filter(used::contains).forEach(shown::add);
    return shown;
  }
}
