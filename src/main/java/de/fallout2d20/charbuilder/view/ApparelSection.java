package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.apparel.Coverage;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.apparel.ApparelRules;
import de.fallout2d20.charbuilder.model.character.apparel.ApparelValues;
import de.fallout2d20.charbuilder.model.character.apparel.ArmorResistances;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.apparel.LocationResistance;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.view.dialog.AddApparelDialog;
import de.fallout2d20.charbuilder.view.dialog.ConfirmDialog;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Clothing, armor and robot armor, in every mode: one row per item (a worn checkbox – carried pieces are
 * greyed and do not protect –, kind, location – a choice for a
 * single arm or leg –, resistances, a note, e.g. mods, and a remove button), the add button, the damage
 * resistance per hit location ({@link ArmorResistances}) and the apparel hints ({@link ApparelRules}).
 * Typed notes go to {@code onEdited}; everything else to the model and then to {@code onChanged}.
 */
final class ApparelSection {

  private static final @NonNull List<String> HEADERS = List.of("Worn", "Item", "Kind", "Location", "Phys", "Energy",
      "Rad", "Notes", "");
  /** Opacity of the values of a carried piece, which do not protect. */
  private static final double CARRIED_OPACITY = 0.5;
  private static final @NonNull String HINT_STYLE = "-fx-text-fill: #a35200;";
  private static final double NOTES_WIDTH = 160;

  private ApparelSection() {}

  /** Adds the apparel list, the add button, the resistance table and the hints to a four-column grid. */
  static void addTo(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onEdited,
      @NonNull Runnable onChanged) {
    List<WornApparel> apparel = character.getApparel();
    if (!apparel.isEmpty()) {
      MigPane table = new MigPane("insets 0, gap 10 4, wrap " + HEADERS.size());
      for (String header : HEADERS) {
        table.add(bold(header));
      }
      for (int i = 0; i < apparel.size(); i++) {
        addItem(table, character, apparel.get(i), i, onEdited, onChanged);
      }
      pane.add(table, "span 4, growx, wmin 0");
    }

    Button add = new Button("Add apparel");
    add.setGraphic(new FontIcon("fas-plus"));
    ApparelKind defaultKind = ApparelRules.isRobot(character) ? ApparelKind.ROBOT_ARMOR : ApparelKind.ARMOR;
    add.setOnAction(event -> AddApparelDialog.show(window(add), offered(character), character.hitLocations(),
        defaultKind).ifPresent(item -> {
          character.addApparel(item);
          onChanged.run();
        }));
    pane.add(add, "span 4, left");

    addResistances(pane, character);
    List<CreationProblem> problems = ApparelRules.problems(character);
    if (!problems.isEmpty()) {
      Label hints = new Label(problems.stream().map(CreationProblem::message).collect(Collectors.joining("\n")));
      hints.setStyle(HINT_STYLE);
      hints.setWrapText(true);
      pane.add(new Label("Apparel hints"), "top");
      pane.add(hints, "span 3");
    }
  }

  /** Catalog items of the enabled books offered to the character ({@link ApparelRules#isOffered}). */
  private static @NonNull List<ApparelData> offered(@NonNull Character character) {
    return ApparelCatalog.available(character.getEnabledBooks()).stream()
        .filter(item -> ApparelRules.isOffered(character, item.kind(), item.isHat())).toList();
  }

  private static void addItem(@NonNull MigPane table, @NonNull Character character, @NonNull WornApparel item,
      int index, @NonNull Runnable onEdited, @NonNull Runnable onChanged) {
    Optional<ApparelData> data = item.data();
    CheckBox worn = new CheckBox();
    worn.setSelected(!item.carried());
    worn.setTooltip(new Tooltip("Worn pieces protect; carried pieces only count toward the load."));
    worn.setOnAction(event -> {
      character.setApparelCarried(index, !worn.isSelected());
      onChanged.run();
    });
    table.add(worn);
    Label name = new Label(item.displayName());
    name.setTooltip(new Tooltip(data.map(entry -> entry.sources().getFirst()
        + (entry.perk() == null ? "" : ", needs " + entry.perk())).orElse("Custom item")));
    Optional<ApparelValues> values = item.values();
    List<Node> cells = List.of(name, new Label(values.map(entry -> entry.kind().displayName()).orElse("–")),
        location(character, item, data, index, onChanged),
        new Label(values.map(entry -> Integer.toString(entry.physical())).orElse("–")),
        new Label(values.map(entry -> Integer.toString(entry.energy())).orElse("–")),
        new Label(values.map(ApparelSection::radiation).orElse("–")));
    for (Node cell : cells) {
      if (item.carried()) {
        cell.setOpacity(CARRIED_OPACITY);
      }
      table.add(cell);
    }
    TextField notes = new TextField(item.notes());
    notes.setPromptText("Notes, e.g. mods");
    notes.setPrefWidth(NOTES_WIDTH);
    notes.textProperty().addListener((obs, old, text) -> {
      character.setApparelNotes(index, text);
      onEdited.run();
    });
    table.add(notes);
    Button remove = new Button();
    remove.setGraphic(new FontIcon("fas-trash"));
    remove.setTooltip(new Tooltip("Take off " + item.displayName()));
    remove.setOnAction(event -> {
      if (ConfirmDialog.confirm(window(remove), "Remove apparel", "Remove " + item.displayName() + "?",
          "The item and its note are removed.", "Remove")) {
        character.removeApparel(index);
        onChanged.run();
      }
    });
    table.add(remove);
  }

  /** Radiation as in the tables: "Immune", the value, or "–" for robot items, whose table has no radiation. */
  private static @NonNull String radiation(@NonNull ApparelValues values) {
    if (values.radiationImmune()) {
      return "Immune";
    }
    return values.kind().isRobot() && values.radiation() == 0 ? "–" : Integer.toString(values.radiation());
  }

  /** A choice of the limb for a single-limb catalog piece, else the covered locations as text. */
  private static @NonNull Node location(@NonNull Character character, @NonNull WornApparel item,
      @NonNull Optional<ApparelData> data, int index, @NonNull Runnable onChanged) {
    Optional<Coverage> chosen = data.flatMap(entry -> entry.coverage().stream().filter(Coverage::isChosen).findFirst());
    if (chosen.isEmpty()) {
      return new Label(item.locations(character.hitLocations()).stream().map(HitLocation::displayName)
          .collect(Collectors.joining(", ")));
    }
    ComboBox<HitLocation> box = new ComboBox<>();
    box.getItems().setAll(chosen.get().locationsOn(character.hitLocations()));
    box.setConverter(new DisplayConverter<>(HitLocation::displayName));
    box.setPromptText("Choose");
    box.setValue(item.location());
    box.setOnAction(event -> {
      if (box.getValue() != item.location()) {
        character.setApparelLocation(index, box.getValue());
        onChanged.run();
      }
    });
    return box;
  }

  /** CRB p. 130, 146: the damage resistance per hit location, apparel plus innate. */
  private static void addResistances(@NonNull MigPane pane, @NonNull Character character) {
    MigPane table = new MigPane("insets 0, gap 14 2, wrap 5");
    for (String header : List.of("Damage resistance", "Physical", "Energy", "Radiation", "Poison")) {
      table.add(bold(header));
    }
    for (LocationResistance location : ArmorResistances.of(character)) {
      table.add(new Label(location.location().displayName()));
      table.add(new Label(location.physical().toString()));
      table.add(new Label(location.energy().toString()));
      table.add(new Label(location.radiation().toString()));
      table.add(new Label(location.poison().toString()));
    }
    Tooltip.install(table, new Tooltip("Worn apparel plus innate resistances from origin and perks."));
    pane.add(table, "span 4, gaptop 8, left");
  }

  private static @NonNull Label bold(@NonNull String text) {
    Label label = new Label(text);
    label.setStyle("-fx-font-weight: bold;");
    return label;
  }

  private static @Nullable Window window(@NonNull Node node) {
    return node.getScene() == null ? null : node.getScene().getWindow();
  }
}
