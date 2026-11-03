package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.equipment.D20TableRow;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorLocation;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponType;
import de.fallout2d20.charbuilder.model.character.legendary.LegendaryRoll;
import de.fallout2d20.charbuilder.model.character.legendary.LegendaryRollResult;
import de.fallout2d20.charbuilder.view.DisplayConverter;
import de.fallout2d20.charbuilder.view.Layouts;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * The Roll tables section of the GM compendium (H2): choose a table of the enabled books and roll. A plain d20 table
 * shows its rows and selects the rolled one; the legendary tables roll for the chosen weapon type or armor location
 * and link the result to the Legendary section.
 */
final class RollTablesSection {

  private static final double PADDING = 8;

  private final @NonNull RandomGenerator random;
  private final @NonNull Consumer<Reference> open;
  private final @NonNull ComboBox<LegendaryWeaponType> weaponType = new ComboBox<>(
      FXCollections.observableArrayList(LegendaryWeaponType.values()));
  private final @NonNull ComboBox<LegendaryArmorLocation> armorLocation = new ComboBox<>(
      FXCollections.observableArrayList(LegendaryArmorLocation.values()));
  private final @NonNull TableView<D20TableRow> rows = new TableView<>();
  private final @NonNull HBox result = new HBox(Layouts.GAP);
  private final @NonNull VBox node;

  RollTablesSection(@NonNull Set<Book> books, @NonNull RandomGenerator random, @NonNull Consumer<Reference> open) {
    this.random = random;
    this.open = open;
    List<RollTable> tables = Arrays.stream(RollTable.values()).filter(table -> books.contains(table.book())).toList();
    ComboBox<RollTable> table = new ComboBox<>(FXCollections.observableArrayList(tables));
    table.setConverter(new DisplayConverter<>(RollTable::title));
    weaponType.setConverter(new DisplayConverter<>(LegendaryWeaponType::displayName));
    weaponType.setValue(LegendaryWeaponType.SMALL_GUNS);
    armorLocation.setConverter(new DisplayConverter<>(LegendaryArmorLocation::displayName));
    armorLocation.setValue(LegendaryArmorLocation.TORSO);

    TableColumn<D20TableRow, Integer> roll = new TableColumn<>("d20");
    roll.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().roll()));
    roll.setSortable(false);
    TableColumn<D20TableRow, String> name = new TableColumn<>("Result");
    name.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().name()));
    name.setSortable(false);
    TableColumn<D20TableRow, String> source = new TableColumn<>("Source");
    source.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().source().toString()));
    source.setSortable(false);
    rows.getColumns().setAll(List.of(roll, name, source));
    rows.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    rows.setPlaceholder(new Label("The legendary tables are rolled for a weapon type or armor location."));

    Button rollButton = new Button("Roll", new FontIcon("fas-dice-d20"));
    rollButton.setOnAction(_ -> roll(table.getValue()));
    rollButton.disableProperty().bind(table.valueProperty().isNull());
    HBox controls = new HBox(Layouts.GAP, new Label("Table"), table, weaponType, armorLocation, rollButton);
    controls.setAlignment(Pos.CENTER_LEFT);
    result.setAlignment(Pos.CENTER_LEFT);
    table.valueProperty().addListener((_, _, chosen) -> show(chosen));
    node = new VBox(Layouts.GAP, controls, result, rows);
    node.setPadding(new Insets(PADDING));
    VBox.setVgrow(rows, Priority.ALWAYS);
    if (!tables.isEmpty()) {
      table.setValue(tables.getFirst());
    }
  }

  @NonNull Node node() {
    return node;
  }

  private void show(@Nullable RollTable table) {
    result.getChildren().clear();
    boolean weapon = table == RollTable.LEGENDARY_WEAPON;
    boolean armor = table == RollTable.LEGENDARY_ARMOR;
    weaponType.setVisible(weapon);
    weaponType.setManaged(weapon);
    armorLocation.setVisible(armor);
    armorLocation.setManaged(armor);
    rows.getItems().setAll(table == null ? List.of() : table.rows());
  }

  private void roll(@Nullable RollTable table) {
    if (table == null) {
      return;
    }
    result.getChildren().clear();
    switch (table) {
      case TRINKETS, OUTCAST_EQUIPMENT -> {
        D20TableRow row = table.roll(random);
        rows.getSelectionModel().select(row);
        rows.scrollTo(row);
        result.getChildren().add(bold("d20 " + row.roll() + ": " + row.name()));
      }
      case LEGENDARY_WEAPON -> {
        LegendaryRollResult<LegendaryWeaponData> rolled = new LegendaryRoll(random).weapon(weaponType.getValue());
        showLegendary(rolled.property().name(), rolled.description(),
            EquipmentEntries.legendaryWeaponId(rolled.property().id()));
      }
      case LEGENDARY_ARMOR -> {
        LegendaryRollResult<LegendaryArmorData> rolled = new LegendaryRoll(random).armor(armorLocation.getValue());
        showLegendary(rolled.property().name(), rolled.description(),
            EquipmentEntries.legendaryArmorId(rolled.property().id()));
      }
    }
  }

  private void showLegendary(@NonNull String name, @NonNull String steps, @NonNull String id) {
    Hyperlink link = new Hyperlink(name);
    link.setStyle("-fx-font-weight: bold;");
    link.setOnAction(_ -> open.accept(new Reference(Section.LEGENDARY, id, name)));
    result.getChildren().addAll(link, new Label("(" + steps + ")"));
  }

  private static @NonNull Label bold(@NonNull String text) {
    Label label = new Label(text);
    label.setStyle("-fx-font-weight: bold;");
    return label;
  }
}
