package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.model.character.apparel.CustomApparel;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.view.DisplayConverter;
import de.fallout2d20.charbuilder.view.Layouts;
import de.fallout2d20.charbuilder.view.WholeNumber;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The form of a custom piece of apparel (homebrew, books not catalogued yet): name, kind, the covered hit
 * locations of the character's body, resistances, the carry weight a robot's armor adds and the mod table the
 * piece takes its mods from (only tables fitting the kind; user decision 2026-10-22).
 */
final class CustomApparelForm {

  private final @NonNull ApparelKind defaultKind;
  private final @NonNull TextField name = new TextField();
  private final @NonNull ComboBox<ApparelKind> kind =
      new ComboBox<>(FXCollections.observableArrayList(ApparelKind.values()));
  private final @NonNull Map<HitLocation, CheckBox> covered = new EnumMap<>(HitLocation.class);
  private final @NonNull TextField physical = numberField();
  private final @NonNull TextField energy = numberField();
  private final @NonNull TextField radiation = numberField();
  private final @NonNull CheckBox immune = new CheckBox("Immune to radiation");
  private final @NonNull TextField carryWeight = new TextField("0");
  private final @NonNull ModTableChoice modTable;
  private final @NonNull MigPane form = Layouts.dialogForm();

  /**
   * @param locations the character's hit locations
   * @param defaultKind the preselected kind (robot armor for a Mister Handy)
   */
  CustomApparelForm(@NonNull List<HitLocation> locations, @NonNull ApparelKind defaultKind) {
    this.defaultKind = defaultKind;
    this.modTable = new ModTableChoice(defaultKind, null);
    kind.setConverter(new DisplayConverter<>(ApparelKind::displayName));
    kind.setValue(defaultKind);
    FlowPane locationBoxes = new FlowPane(Layouts.GAP, Layouts.ROW_GAP);
    for (HitLocation location : locations) {
      CheckBox box = new CheckBox(location.displayName());
      covered.put(location, box);
      locationBoxes.getChildren().add(box);
    }
    addRow("Name", name);
    addRow("Kind", kind);
    addRow("Locations", locationBoxes);
    addRow("Physical", physical);
    addRow("Energy", energy);
    addRow("Radiation", radiation);
    addRow("", immune);
    addRow("Carry weight (robots)", carryWeight);
    addRow("Mod table", modTable.node());
    kind.valueProperty().addListener((_, _, chosen) -> modTable.fitTo(chosen == null ? defaultKind : chosen));
  }

  @NonNull Node node() {
    return form;
  }

  /** What {@link #apparel()} depends on, for the bindings of the dialog. */
  @NonNull List<Observable> dependencies() {
    List<Observable> dependencies = new ArrayList<>(List.of(name.textProperty(), physical.textProperty(),
        energy.textProperty(), radiation.textProperty(), immune.selectedProperty(), carryWeight.textProperty(),
        modTable.node().valueProperty()));
    covered.values().forEach(box -> dependencies.add(box.selectedProperty()));
    return dependencies;
  }

  /** The described piece; {@code null} while name or locations are missing or a number is invalid. */
  @Nullable CustomApparel apparel() {
    Set<HitLocation> locations = selectedLocations();
    Integer physicalDr = WholeNumber.atLeast(physical.getText(), 0);
    Integer energyDr = WholeNumber.atLeast(energy.getText(), 0);
    Integer radiationDr = immune.isSelected() ? Integer.valueOf(0) : WholeNumber.atLeast(radiation.getText(), 0);
    OptionalInt carry = WholeNumber.parse(carryWeight.getText());
    if (name.getText().isBlank() || locations.isEmpty() || physicalDr == null || energyDr == null
        || radiationDr == null || carry.isEmpty()) {
      return null;
    }
    ApparelKind chosenKind = kind.getValue();
    return new CustomApparel(name.getText(), chosenKind == null ? defaultKind : chosenKind, locations, physicalDr,
        energyDr, radiationDr, immune.isSelected(), carry.getAsInt(), modTable.value());
  }

  private @NonNull Set<HitLocation> selectedLocations() {
    Set<HitLocation> selected = EnumSet.noneOf(HitLocation.class);
    covered.forEach((location, box) -> {
      if (box.isSelected()) {
        selected.add(location);
      }
    });
    return selected;
  }

  private void addRow(@NonNull String label, @NonNull Node control) {
    form.add(new Label(label));
    form.add(control);
  }

  private static @NonNull TextField numberField() {
    TextField field = new TextField("0");
    field.setPrefColumnCount(4);
    return field;
  }
}
