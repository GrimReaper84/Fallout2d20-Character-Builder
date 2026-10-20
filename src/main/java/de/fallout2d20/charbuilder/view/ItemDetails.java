package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.item.ChemDuration;
import de.fallout2d20.charbuilder.catalog.item.ItemData;
import de.fallout2d20.charbuilder.catalog.item.MagazineData;
import de.fallout2d20.charbuilder.catalog.item.MagazineIssue;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.equipment.StartingSource;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.Holotape;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The details of the item selected in the Gear tab (user decision 2026-10-19): source, type (with HP healed,
 * irradiated, chem duration and addiction), the quantity with −/+, weight, the short description – for a
 * magazine with the issue choice (CRB p. 172) –, where a starting-equipment item comes from, and the note. A
 * holotape shows its name and label as editable fields instead. Effects are shown, not applied (user
 * decision). Typed text goes to {@code onEdited}; everything else to the model and then to {@code onChanged}.
 */
final class ItemDetails {

  private static final int LABEL_ROWS = 4;

  private ItemDetails() {}

  /** The details of {@code entry}, or a prompt to select an item if it is {@code null}. */
  static @NonNull Node build(@NonNull PlayerCharacter character, @Nullable ItemEntry entry,
      @NonNull ViewCallbacks callbacks) {
    MigPane pane = Layouts.column();
    if (entry == null) {
      OverviewParts.sectionHeading(pane, "Gear");
      pane.add(new Label("Select an item on the left."));
      return pane;
    }
    int index = entry.index();
    // From the model, not the entry: typed text changes the item without a rebuild of the tree's entries.
    CarriedItem item = character.gear().getItems().get(index);
    OverviewParts.sectionHeading(pane, item.displayName());

    MigPane rows = Layouts.details(0);
    Holotape holotape = item.holotape();
    if (holotape != null) {
      addHolotape(rows, character, index, holotape, callbacks);
    } else {
      addItem(rows, character, item, index, callbacks.onChanged());
    }
    StartingSource source = item.startingSource();
    if (source != null) {
      OverviewParts.detailRow(rows, "Starting equipment", new Label(source.description()));
    }
    OverviewParts.notesRow(rows, item.notes(), text -> {
      character.gear().setItemNotes(index, text);
      callbacks.onEdited().run();
    });
    pane.add(rows);
    return pane;
  }

  private static void addItem(@NonNull MigPane rows, @NonNull PlayerCharacter character, @NonNull CarriedItem item,
      int index, @NonNull Runnable onChanged) {
    Optional<ItemData> data = item.data();
    Optional<MagazineData> magazine = item.magazine();
    OverviewParts.detailRow(rows, "Source", new Label(data.map(entry -> entry.sources().getFirst().toString())
        .or(() -> magazine.map(entry -> entry.sources().getFirst().toString())).orElse("Custom item")));
    OverviewParts.detailRow(rows, "Type", OverviewParts.wrappingInCell(new Label(data.map(ItemDetails::type)
        .or(() -> magazine.map(_ -> MagazineData.CATEGORY)).orElse("–"))));
    addQuantity(rows, character, item, index, onChanged);
    String weight = item.printedWeight();
    OverviewParts.detailRow(rows, "Weight", new Label(weight == null ? "–" : weight + " lbs."));
    magazine.filter(entry -> !entry.issues().isEmpty())
        .ifPresent(entry -> OverviewParts.detailRow(rows, "Issue", issue(character, item, entry, index, onChanged)));
    OverviewParts.detailRow(rows, "Description",
        OverviewParts.wrappingInCell(new Label(OverviewParts.orDash(item.effects()))));
  }

  /** Name (committed on Enter or when leaving the field, then the tree shows it) and the free label text. */
  private static void addHolotape(@NonNull MigPane rows, @NonNull PlayerCharacter character, int index,
      @NonNull Holotape holotape, @NonNull ViewCallbacks callbacks) {
    OverviewParts.detailRow(rows, "Source", new Label("Holotape"));
    TextField name = new TextField(holotape.name());
    name.setPromptText("Name of the holotape");
    Runnable commitName = () -> {
      Holotape current = character.gear().getItems().get(index).holotape();
      String typed = name.getText().strip();
      if (current != null && !typed.isEmpty() && !typed.equals(current.name())) {
        character.gear().setItemHolotape(index, new Holotape(typed, current.label()));
        callbacks.onChanged().run();
      }
    };
    name.setOnAction(_ -> commitName.run());
    name.focusedProperty().addListener((_, _, focused) -> {
      if (!focused) {
        commitName.run();
      }
    });
    OverviewParts.detailRow(rows, "Name", name);
    TextArea label = new TextArea(holotape.label());
    label.setPromptText("Label, contents or code");
    label.setWrapText(true);
    label.setPrefRowCount(LABEL_ROWS);
    label.textProperty().addListener((_, _, text) -> {
      Holotape current = character.gear().getItems().get(index).holotape();
      if (current != null) {
        character.gear().setItemHolotape(index, new Holotape(current.name(), text));
        callbacks.onEdited().run();
      }
    });
    rows.add(OverviewParts.rowName("Label"), Layouts.top());
    rows.add(label);
    Label weight = new Label("0 lbs.");
    weight.setTooltip(new Tooltip("The book gives holotapes no weight."));
    OverviewParts.detailRow(rows, "Weight", weight);
  }

  private static void addQuantity(@NonNull MigPane rows, @NonNull PlayerCharacter character, @NonNull CarriedItem item,
      int index, @NonNull Runnable onChanged) {
    OverviewParts.detailRow(rows, "Quantity", StepButtons.withValue(new Label(Integer.toString(item.quantity())),
        StepButtons.NUMBER_WIDTH, StepButtons.of(
            StepButtons.minus(item.quantity() > 1 ? "One less" : "Use Remove to take the item out",
                item.quantity() > 1, () -> {
                  character.gear().setItemQuantity(index, item.quantity() - 1);
                  onChanged.run();
                }),
            StepButtons.plus("One more", true, () -> {
              character.gear().setItemQuantity(index, item.quantity() + 1);
              onChanged.run();
            }))));
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

  /** The issue choice of a publication with issues (CRB p. 172: rolled on its d20 table). */
  private static @NonNull Node issue(@NonNull PlayerCharacter character, @NonNull CarriedItem item,
      @NonNull MagazineData magazine, int index, @NonNull Runnable onChanged) {
    ComboBox<MagazineIssue> issue = new ComboBox<>();
    issue.getItems().setAll(magazine.issues());
    issue.setConverter(new DisplayConverter<>(entry -> entry.roll() + ": " + entry.title()));
    issue.setPromptText("Roll the issue (d20)");
    issue.setMaxWidth(Region.USE_PREF_SIZE);
    magazine.issue(item.issue()).ifPresent(issue::setValue);
    issue.setOnAction(_ -> {
      MagazineIssue chosen = issue.getValue();
      if (chosen != null && !chosen.title().equals(item.issue())) {
        character.gear().setItemIssue(index, chosen.title());
        onChanged.run();
      }
    });
    return issue;
  }
}
