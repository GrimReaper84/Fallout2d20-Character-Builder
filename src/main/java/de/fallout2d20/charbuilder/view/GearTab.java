package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.equipment.EquipmentPackData;
import de.fallout2d20.charbuilder.catalog.item.ItemCatalog;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.LegacyImport;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.Holotape;
import de.fallout2d20.charbuilder.view.dialog.AddItemDialog;
import de.fallout2d20.charbuilder.view.dialog.ConfirmDialog;
import de.fallout2d20.charbuilder.view.dialog.EquipmentPackDialog;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Gear tab, in every mode (user decision 2026-10-19): on top the caps, junk and materials and the load
 * ({@link GearSummary}), the tab's hints and the equipment pack with the button to its dialog
 * ({@link EquipmentPackDialog}); below, in the upper two thirds, the item tree grouped by type
 * ({@link ItemTree}) with add and remove buttons on the left and the details of the selected item
 * ({@link ItemDetails}) on the right, each side scrolling on its own; in the lower third the gear notes.
 * Selecting an item only replaces the details; the selection and both scroll positions survive the rebuild
 * after a change ({@link MasterDetailState}, layout {@link MasterDetail}).
 */
final class GearTab {

  /** Share of the height for the tree and the details; the gear notes get the rest. */
  private static final double TREE_PERCENT = 200.0 / 3;
  private static final double PADDING = 16;
  private static final String NO_PINCER_TOOLTIP =
      "No unarmed attacks or object handling; no Lockpick, Repair or Throwing tests (CRB p. 55).";

  private GearTab() {}

  /** @param hints the hints of this tab */
  static @NonNull Node build(@NonNull Character character, @NonNull List<CreationProblem> hints,
      @NonNull MasterDetailState selection, @NonNull ViewCallbacks callbacks) {
    List<ItemEntry> entries = ItemTree.entries(character.getItems());
    Integer selected = selection.current(entries.size());
    ItemEntry current = selected == null ? null : entries.get(selected);

    ScrollPane details = MasterDetail.right(ItemDetails.build(character, current, callbacks), selection);
    RemoveButton remove = new RemoveButton(() -> {
      Integer index = selection.current(character.getItems().size());
      if (index == null) {
        return;
      }
      CarriedItem item = character.getItems().get(index);
      if (ConfirmDialog.confirm(window(details), "Remove item", "Remove " + item.displayName() + "?",
          "The item, its quantity and note are removed from the gear.", "Remove")) {
        character.removeItem(index);
        selection.clear();
        callbacks.onChanged().run();
      }
    });
    updateRemove(remove, current);
    TreeView<ItemTreeNode> tree = ItemTree.build(entries, current, entry -> {
      if (entry == null) {
        selection.clear();
      } else {
        selection.select(entry.index());
      }
      updateRemove(remove, entry);
      MasterDetail.replace(details, ItemDetails.build(character, entry, callbacks));
    });

    MigPane left = new MigPane("insets 0, gap 0 4, wrap 1", "[grow, fill]");
    OverviewParts.sectionHeading(left, "Gear");
    MigPane buttons = new MigPane("insets 0, gap 8 0");
    buttons.add(addButton(character, selection, callbacks.onChanged()));
    buttons.add(remove.node());
    left.add(buttons);
    left.add(tree, "grow, push");

    GridPane center = new GridPane();
    center.getColumnConstraints().add(fillingColumn());
    center.getRowConstraints().addAll(row(TREE_PERCENT), row(100 - TREE_PERCENT));
    center.add(MasterDetail.layout(MasterDetail.left(left, selection), details), 0, 0);
    center.add(notes(character, callbacks.onEdited()), 0, 1);

    BorderPane pane = new BorderPane(center);
    pane.setTop(top(character, hints, callbacks.onChanged()));
    return pane;
  }

  /** Summary bar, hints, the Mister Handy's missing pincer, the legacy note and the equipment pack row. */
  private static @NonNull Node top(@NonNull Character character, @NonNull List<CreationProblem> hints,
      @NonNull Runnable onChanged) {
    VBox top = new VBox(8, GearSummary.build(character, onChanged));
    if (!hints.isEmpty()) {
      top.getChildren().add(OverviewParts.wrapping(OverviewParts.hintLabel(hints)));
    }
    if (character.getStartingEquipment().lacksPincer()) {
      Label noPincer = new Label("No pincer: this Mister Handy has no unarmed attacks and cannot handle objects.");
      noPincer.setTooltip(new Tooltip(NO_PINCER_TOOLTIP));
      noPincer.setStyle(OverviewParts.HINT_STYLE);
      top.getChildren().add(noPincer);
    }
    LegacyImport legacy = character.getLegacyImport();
    if (legacy != null) {
      top.getChildren().add(new Label("Imported " + legacy.importedOn()
          + ": the legacy equipment text is in the gear notes."));
    }
    top.getChildren().add(packRow(character, onChanged));
    top.setPadding(new Insets(PADDING, PADDING, 0, PADDING));
    return top;
  }

  /** The chosen pack and, while it can still be chosen (creation or GM mode), the button to the dialog. */
  private static @NonNull Node packRow(@NonNull Character character, @NonNull Runnable onChanged) {
    MigPane row = new MigPane("insets 0, gap 8 0");
    CatalogRef pack = character.getStartingEquipment().pack();
    row.add(new Label("Equipment pack: " + (pack == null ? "none"
        : character.getEquipmentPack().map(EquipmentPackData::name).orElse(pack.displayName()))));
    if (!character.isCreationComplete() || character.isGmMode()) {
      Button choose = new Button("Equipment pack…");
      choose.setGraphic(new FontIcon("fas-box-open"));
      choose.setTooltip(new Tooltip("Choose the pack, its options and the Tag skill items; OK enters them."));
      choose.setOnAction(event -> {
        if (EquipmentPackDialog.show(window(choose), character)) {
          onChanged.run();
        }
      });
      row.add(choose, "gapleft 12");
    }
    return row;
  }

  /** The gear notes, filling the lower third. */
  private static @NonNull Node notes(@NonNull Character character, @NonNull Runnable onEdited) {
    TextArea notes = new TextArea(character.getInventoryNotes());
    notes.setPromptText("Gear bought, found or used up, amounts to roll, …");
    notes.setWrapText(true);
    notes.textProperty().addListener((obs, old, text) -> {
      character.setInventoryNotes(text);
      onEdited.run();
    });
    VBox.setVgrow(notes, Priority.ALWAYS);
    MigPane heading = new MigPane("insets 0");
    OverviewParts.sectionHeading(heading, "Gear notes");
    VBox box = new VBox(4, heading, notes);
    box.setPadding(new Insets(0, PADDING, PADDING, PADDING));
    return box;
  }

  /** "Add ▾": a catalog item (dialog) or a new holotape; the new entry is selected. */
  private static @NonNull Node addButton(@NonNull Character character, @NonNull MasterDetailState selection,
      @NonNull Runnable onChanged) {
    MenuButton add = new MenuButton("Add");
    add.setGraphic(new FontIcon("fas-plus"));
    MenuItem item = new MenuItem("Item…");
    item.setOnAction(event -> AddItemDialog.show(window(add), ItemCatalog.available(character.getEnabledBooks()),
        ItemCatalog.availableMagazines(character.getEnabledBooks())).ifPresent(carried -> {
          character.addItem(carried);
          selection.select(character.getItems().size() - 1);
          onChanged.run();
        }));
    MenuItem holotape = new MenuItem("Holotape");
    holotape.setOnAction(event -> {
      character.addItem(CarriedItem.of(new Holotape(Holotape.NEW_NAME, "")));
      selection.select(character.getItems().size() - 1);
      onChanged.run();
    });
    add.getItems().addAll(item, holotape);
    return add;
  }

  private static void updateRemove(@NonNull RemoveButton remove, @Nullable ItemEntry entry) {
    remove.update(entry != null,
        entry == null ? "Select an item to remove it." : "Remove " + entry.item().displayName());
  }

  private static @NonNull ColumnConstraints fillingColumn() {
    ColumnConstraints column = new ColumnConstraints();
    column.setHgrow(Priority.ALWAYS);
    column.setFillWidth(true);
    return column;
  }

  private static @NonNull RowConstraints row(double percent) {
    RowConstraints row = new RowConstraints();
    row.setPercentHeight(percent);
    row.setFillHeight(true);
    return row;
  }

  private static @Nullable Window window(@NonNull Node node) {
    return node.getScene() == null ? null : node.getScene().getWindow();
  }
}
