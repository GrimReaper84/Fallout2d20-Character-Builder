package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.item.ChemDuration;
import de.fallout2d20.charbuilder.catalog.item.ItemCatalog;
import de.fallout2d20.charbuilder.catalog.item.ItemData;
import de.fallout2d20.charbuilder.catalog.item.MagazineData;
import de.fallout2d20.charbuilder.catalog.item.MagazineIssue;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.derived.CarryLoad;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.Materials;
import de.fallout2d20.charbuilder.view.dialog.AddItemDialog;
import de.fallout2d20.charbuilder.view.dialog.ConfirmDialog;
import java.util.List;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The inventory in every mode: caps on hand, junk and material counters (CRB p. 208), one row per carried
 * item (name with source, type, effect – for a magazine its issue –, quantity −/+, weight, note, remove), the
 * add button and the carried load against the carry weight with the encumbrance penalty (CRB p. 87). Effects
 * are shown, not applied (user decision). Typed notes go to {@code onEdited}; everything else to the model
 * and then to {@code onChanged}.
 */
final class ItemSection {

  private static final @NonNull List<String> HEADERS = List.of("Item", "Type", "Effect", "Qty", "Weight", "Notes", "");
  private static final @NonNull String HINT_STYLE = "-fx-text-fill: #a35200;";
  private static final double EFFECT_WIDTH = 320;
  private static final double NOTES_WIDTH = 160;

  private ItemSection() {}

  /** Adds caps, materials, the item table, the add button and the load to a four-column overview grid. */
  static void addTo(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onEdited,
      @NonNull Runnable onChanged) {
    pane.add(new Label("Caps"));
    TextField caps = NumberField.of((int) Math.min(Integer.MAX_VALUE, character.getCaps()), 0, value -> {
      character.setCaps(value);
      onChanged.run();
    });
    caps.setTooltip(new Tooltip("Caps on hand. \"Create Character\" fills in the fixed starting caps; add rolled "
        + "dice (e.g. Barter 2d20) yourself."));
    pane.add(caps, "span 3");
    addMaterials(pane, character, onChanged);

    List<CarriedItem> items = character.getItems();
    if (!items.isEmpty()) {
      MigPane table = new MigPane("insets 0, gap 10 4, wrap " + HEADERS.size());
      for (String header : HEADERS) {
        Label label = new Label(header);
        label.setStyle("-fx-font-weight: bold;");
        table.add(label);
      }
      for (int i = 0; i < items.size(); i++) {
        addItem(table, character, items.get(i), i, onEdited, onChanged);
      }
      pane.add(table, "span 4, left");
    }

    Button add = new Button("Add item");
    add.setGraphic(new FontIcon("fas-plus"));
    add.setOnAction(event -> AddItemDialog.show(window(add), ItemCatalog.available(character.getEnabledBooks()),
        ItemCatalog.availableMagazines(character.getEnabledBooks())).ifPresent(item -> {
          character.addItem(item);
          onChanged.run();
        }));
    pane.add(add, "span 4, left");
    addLoad(pane, character);
  }

  /** CRB p. 208: junk items and the three grades of material, counted. */
  private static void addMaterials(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onChanged) {
    Materials materials = character.getMaterials();
    MigPane row = new MigPane("insets 0, gap 6 0");
    row.add(new Label("Junk"));
    row.add(NumberField.of(materials.junk(), 0, value -> set(character,
        new Materials(value, materials.common(), materials.uncommon(), materials.rare()), onChanged)));
    row.add(new Label("Common"), "gapleft 12");
    row.add(NumberField.of(materials.common(), 0, value -> set(character,
        new Materials(materials.junk(), value, materials.uncommon(), materials.rare()), onChanged)));
    row.add(new Label("Uncommon"), "gapleft 12");
    row.add(NumberField.of(materials.uncommon(), 0, value -> set(character,
        new Materials(materials.junk(), materials.common(), value, materials.rare()), onChanged)));
    row.add(new Label("Rare"), "gapleft 12");
    row.add(NumberField.of(materials.rare(), 0, value -> set(character,
        new Materials(materials.junk(), materials.common(), materials.uncommon(), value), onChanged)));
    Label label = new Label("Junk and materials");
    label.setTooltip(new Tooltip("CRB p. 208: a junk item weighs 2 lbs. (2 caps); a unit of material 1 lb. "
        + "(common 1, uncommon 3, rare 5 caps)."));
    pane.add(label);
    pane.add(row, "span 3");
  }

  private static void set(@NonNull Character character, @NonNull Materials materials, @NonNull Runnable onChanged) {
    character.setMaterials(materials);
    onChanged.run();
  }

  private static void addItem(@NonNull MigPane table, @NonNull Character character, @NonNull CarriedItem item,
      int index, @NonNull Runnable onEdited, @NonNull Runnable onChanged) {
    Label name = new Label(item.displayName());
    Optional<ItemData> data = item.data();
    Optional<MagazineData> magazine = item.magazine();
    name.setTooltip(new Tooltip(data.map(entry -> entry.sources().getFirst().toString())
        .or(() -> magazine.map(entry -> entry.sources().getFirst().toString())).orElse("Custom item")));
    table.add(name);
    table.add(new Label(data.map(ItemSection::type).or(() -> magazine.map(entry -> MagazineData.CATEGORY)).orElse("–")));
    table.add(effect(character, item, magazine, index, onChanged));
    table.add(StepButtons.withValue(new Label(Integer.toString(item.quantity())), StepButtons.NUMBER_WIDTH, StepButtons.of(
        StepButtons.minus(item.quantity() > 1 ? "One less" : "Use the trash button to remove the item",
            item.quantity() > 1, () -> {
              character.setItemQuantity(index, item.quantity() - 1);
              onChanged.run();
            }),
        StepButtons.plus("One more", true, () -> {
          character.setItemQuantity(index, item.quantity() + 1);
          onChanged.run();
        }))));
    String weight = item.printedWeight();
    table.add(new Label(weight == null ? "–" : weight));
    TextField notes = new TextField(item.notes());
    notes.setPromptText("Notes");
    notes.setPrefWidth(NOTES_WIDTH);
    notes.textProperty().addListener((obs, old, text) -> {
      character.setItemNotes(index, text);
      onEdited.run();
    });
    table.add(notes);
    Button remove = new Button();
    remove.setGraphic(new FontIcon("fas-trash"));
    remove.setTooltip(new Tooltip("Remove " + item.displayName()));
    remove.setOnAction(event -> {
      if (ConfirmDialog.confirm(window(remove), "Remove item", "Remove " + item.displayName() + "?",
          "The item, its quantity and note are removed from the inventory.", "Remove")) {
        character.removeItem(index);
        onChanged.run();
      }
    });
    table.add(remove);
  }

  /** The category with what matters at the table: HP healed, irradiated, chem duration and addiction. */
  private static @NonNull String type(@NonNull ItemData item) {
    StringBuilder type = new StringBuilder(item.category().displayName());
    Integer hp = item.hpHealed();
    if (hp != null) {
      type.append(", ").append(hp).append(" HP");
    }
    if (item.irradiated()) {
      type.append(", irradiated");
    }
    ChemDuration duration = item.duration();
    if (duration != null) {
      type.append(", ").append(duration.displayName());
    }
    Integer addictive = item.addictive();
    if (addictive != null) {
      type.append(", addictive ").append(addictive);
    }
    return type.toString();
  }

  /** The effect text; for a publication with issues also the issue choice (CRB p. 172: rolled on its d20 table). */
  private static @NonNull Node effect(@NonNull Character character, @NonNull CarriedItem item,
      @NonNull Optional<MagazineData> magazine, int index, @NonNull Runnable onChanged) {
    Label effect = new Label(item.effects().isBlank() ? "–" : item.effects());
    effect.setWrapText(true);
    effect.setMaxWidth(EFFECT_WIDTH);
    if (magazine.isEmpty() || magazine.get().issues().isEmpty()) {
      return effect;
    }
    ComboBox<MagazineIssue> issue = new ComboBox<>();
    issue.getItems().setAll(magazine.get().issues());
    issue.setConverter(new DisplayConverter<>(entry -> entry.roll() + ": " + entry.title()));
    issue.setPromptText("Roll the issue (d20)");
    magazine.get().issue(item.issue()).ifPresent(issue::setValue);
    issue.setOnAction(event -> {
      MagazineIssue chosen = issue.getValue();
      if (chosen != null && !chosen.title().equals(item.issue())) {
        character.setItemIssue(index, chosen.title());
        onChanged.run();
      }
    });
    return new VBox(4, issue, effect);
  }

  /** CRB p. 87: "Load 143 / 190 lbs." and, when over the carry weight, the penalty. */
  private static void addLoad(@NonNull MigPane pane, @NonNull Character character) {
    CarryLoad load = CarryLoad.of(character);
    pane.add(new Label("Load"));
    Label total = new Label(load.carried() + " / " + load.carryWeight() + " lbs.");
    total.setTooltip(new Tooltip("Weapons, worn apparel, items, heavy ammunition, junk and materials; items of "
        + "<1 lb. count nothing (CRB p. 87). Custom weapons and apparel have no weight."));
    if (!load.isOverEncumbered()) {
      pane.add(total, "span 3");
      return;
    }
    Label penalty = new Label(load.immobile()
        ? "Twice the carry weight: cannot move, STR and AGI tests fail, Initiative 0 (CRB p. 87)."
        : "Over-encumbered: +" + load.penalty() + " difficulty on STR and AGI tests, no Sprint, Initiative −"
            + load.penalty() + " (CRB p. 87).");
    penalty.setStyle(HINT_STYLE);
    OverviewParts.wrapping(penalty);
    pane.add(new VBox(2, total, penalty), "span 3");
  }

  private static @Nullable Window window(@NonNull Node node) {
    return node.getScene() == null ? null : node.getScene().getWindow();
  }
}
