package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.apparel.Coverage;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.apparel.CustomApparel;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.view.DisplayConverter;
import de.fallout2d20.charbuilder.view.Layouts;
import de.fallout2d20.charbuilder.view.WholeNumber;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Picks an item to wear: a catalog item (searchable by name) or a custom one (homebrew, books not
 * catalogued yet) with its kind, covered locations and resistances.
 */
public final class AddApparelDialog {

  private AddApparelDialog() {}

  /**
   * Shows the dialog; empty if cancelled.
   *
   * @param items the catalog items to offer (the caller filters them for the character)
   * @param locations the character's hit locations, offered for a custom item
   * @param defaultKind the preselected kind of a custom item (robot armor for a Mister Handy)
   */
  public static @NonNull Optional<WornApparel> show(@Nullable Window owner, @NonNull List<ApparelData> items,
      @NonNull List<HitLocation> locations, @NonNull ApparelKind defaultKind) {
    FilteredList<ApparelData> filtered = new FilteredList<>(FXCollections.observableArrayList(items));
    TextField search = new TextField();
    search.setPromptText("Search");
    search.textProperty().addListener((_, _, text) -> {
      String key = text.strip().toLowerCase(Locale.ROOT);
      filtered.setPredicate(item -> item.name().toLowerCase(Locale.ROOT).contains(key));
    });
    ListView<ApparelData> list = new ListView<>(filtered);
    list.setCellFactory(TextFieldListCell.forListView(new DisplayConverter<>(AddApparelDialog::describe)));
    list.setPrefSize(520, 360);
    Tab catalog = new Tab("Catalog", new VBox(6, search, list));

    TextField name = new TextField();
    ComboBox<ApparelKind> kind = new ComboBox<>(FXCollections.observableArrayList(ApparelKind.values()));
    kind.setConverter(new DisplayConverter<>(ApparelKind::displayName));
    kind.setValue(defaultKind);
    Map<HitLocation, CheckBox> covered = new EnumMap<>(HitLocation.class);
    FlowPane locationBoxes = new FlowPane(8, 4);
    for (HitLocation location : locations) {
      CheckBox box = new CheckBox(location.displayName());
      covered.put(location, box);
      locationBoxes.getChildren().add(box);
    }
    TextField physical = numberField();
    TextField energy = numberField();
    TextField radiation = numberField();
    CheckBox immune = new CheckBox("Immune to radiation");
    TextField carryWeight = new TextField("0");
    MigPane form = Layouts.dialogForm();
    addRow(form, "Name", name);
    addRow(form, "Kind", kind);
    addRow(form, "Locations", locationBoxes);
    addRow(form, "Physical", physical);
    addRow(form, "Energy", energy);
    addRow(form, "Radiation", radiation);
    addRow(form, "", immune);
    addRow(form, "Carry weight (robots)", carryWeight);
    Tab custom = new Tab("Custom item", form);

    TabPane tabs = new TabPane(catalog, custom);
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    Dialog<WornApparel> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle("Add apparel");
    dialog.setHeaderText("Choose an item from the catalog or describe a custom one.");
    dialog.getDialogPane().setContent(tabs);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    List<Observable> dependencies = new ArrayList<>(List.of(catalog.selectedProperty(),
        list.getSelectionModel().selectedItemProperty(), name.textProperty(), physical.textProperty(),
        energy.textProperty(), radiation.textProperty(), immune.selectedProperty(), carryWeight.textProperty()));
    covered.values().forEach(box -> dependencies.add(box.selectedProperty()));
    Node ok = dialog.getDialogPane().lookupButton(ButtonType.OK);
    ok.disableProperty().bind(Bindings.createBooleanBinding(
        () -> catalog.isSelected() ? list.getSelectionModel().getSelectedItem() == null
            : name.getText().isBlank() || selected(covered).isEmpty() || resistance(physical) < 0
                || resistance(energy) < 0 || (!immune.isSelected() && resistance(radiation) < 0)
                || WholeNumber.parse(carryWeight.getText()).isEmpty(),
        dependencies.toArray(Observable[]::new)));
    dialog.setResultConverter(button -> {
      if (button != ButtonType.OK) {
        return null;
      }
      if (catalog.isSelected()) {
        ApparelData selected = list.getSelectionModel().getSelectedItem();
        return selected == null ? null : WornApparel.of(CatalogRef.of(selected.id()));
      }
      ApparelKind chosenKind = kind.getValue();
      return WornApparel.of(new CustomApparel(name.getText(), chosenKind == null ? defaultKind : chosenKind,
          selected(covered), resistance(physical), resistance(energy),
          immune.isSelected() ? 0 : resistance(radiation), immune.isSelected(),
          WholeNumber.parse(carryWeight.getText()).orElse(0)));
    });
    return dialog.showAndWait();
  }

  /** One list line, e.g. "Raider Arm – Armor, Arm, 1/1/0, CRB p. 130". */
  private static @NonNull String describe(@NonNull ApparelData item) {
    String radiation = item.radiationImmune() ? "Immune" : Integer.toString(item.radiation());
    return item.name() + " – " + item.kind().displayName() + ", "
        + item.coverage().stream().map(Coverage::displayName).collect(Collectors.joining(", "))
        + ", " + item.physical() + "/" + item.energy() + "/" + radiation + ", " + item.sources().getFirst();
  }

  private static @NonNull TextField numberField() {
    TextField field = new TextField("0");
    field.setPrefColumnCount(4);
    return field;
  }

  /** The typed resistance, or −1 if it is not a whole number of at least 0. */
  private static int resistance(@NonNull TextField field) {
    OptionalInt value = WholeNumber.parse(field.getText());
    return value.isPresent() && value.getAsInt() >= 0 ? value.getAsInt() : -1;
  }

  private static @NonNull Set<HitLocation> selected(@NonNull Map<HitLocation, CheckBox> boxes) {
    Set<HitLocation> selected = EnumSet.noneOf(HitLocation.class);
    boxes.forEach((location, box) -> {
      if (box.isSelected()) {
        selected.add(location);
      }
    });
    return selected;
  }

  private static void addRow(@NonNull MigPane form, @NonNull String label, @NonNull Node control) {
    form.add(new Label(label));
    form.add(control);
  }
}
