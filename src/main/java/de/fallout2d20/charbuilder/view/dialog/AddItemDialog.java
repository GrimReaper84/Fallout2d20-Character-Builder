package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.item.ItemCategory;
import de.fallout2d20.charbuilder.catalog.item.ItemData;
import de.fallout2d20.charbuilder.catalog.item.MagazineData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.CustomItem;
import de.fallout2d20.charbuilder.view.DisplayConverter;
import de.fallout2d20.charbuilder.view.WholeNumber;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Picks an item to add: a catalog item or publication of the enabled books (filtered by category,
 * searchable by name) or a custom item with weight and effect. The issue of a magazine is chosen in the
 * inventory afterwards.
 */
public final class AddItemDialog {

  private static final @NonNull String ALL = "All";

  private AddItemDialog() {}

  /** Shows the dialog; empty if cancelled. */
  public static @NonNull Optional<CarriedItem> show(@Nullable Window owner, @NonNull List<ItemData> items,
      @NonNull List<MagazineData> magazines) {
    List<ItemChoice> choices = new ArrayList<>();
    items.forEach(item -> choices.add(ItemChoice.of(item)));
    magazines.forEach(magazine -> choices.add(ItemChoice.of(magazine)));
    FilteredList<ItemChoice> filtered = new FilteredList<>(FXCollections.observableArrayList(choices));
    List<String> categories = new ArrayList<>(List.of(ALL));
    Arrays.stream(ItemCategory.values()).map(ItemCategory::displayName).forEach(categories::add);
    categories.add(MagazineData.CATEGORY);
    ComboBox<String> category = new ComboBox<>(FXCollections.observableArrayList(categories));
    category.setValue(ALL);
    TextField search = new TextField();
    search.setPromptText("Search");
    Runnable filter = () -> {
      String key = search.getText().strip().toLowerCase(Locale.ROOT);
      String chosen = category.getValue();
      filtered.setPredicate(choice -> choice.name().toLowerCase(Locale.ROOT).contains(key)
          && (chosen == null || chosen.equals(ALL) || chosen.equals(choice.category())));
    };
    search.textProperty().addListener((obs, old, text) -> filter.run());
    category.valueProperty().addListener((obs, old, value) -> filter.run());
    ListView<ItemChoice> list = new ListView<>(filtered);
    list.setCellFactory(TextFieldListCell.forListView(new DisplayConverter<>(
        choice -> choice.name() + " (" + choice.category() + ") – " + choice.description())));
    list.setPrefSize(620, 380);
    Tab catalog = new Tab("Catalog", new VBox(6, new HBox(6, category, search), list));

    TextField name = new TextField();
    TextField weight = new TextField("0");
    TextField effects = new TextField();
    MigPane form = new MigPane("wrap 2, insets 8", "[right][grow,fill]");
    form.add(new Label("Name"));
    form.add(name);
    form.add(new Label("Weight (lbs., 0 for <1)"));
    form.add(weight);
    form.add(new Label("Effect"));
    form.add(effects);
    Tab custom = new Tab("Custom item", form);

    TabPane tabs = new TabPane(catalog, custom);
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    Dialog<CarriedItem> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle("Add item");
    dialog.setHeaderText("Choose an item or publication from the catalog or describe a custom one.");
    dialog.getDialogPane().setContent(tabs);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(Bindings.createBooleanBinding(
        () -> catalog.isSelected() ? list.getSelectionModel().getSelectedItem() == null
            : name.getText().isBlank() || pounds(weight.getText()) == null,
        catalog.selectedProperty(), list.getSelectionModel().selectedItemProperty(), name.textProperty(),
        weight.textProperty()));
    dialog.setResultConverter(button -> {
      if (button != ButtonType.OK) {
        return null;
      }
      if (catalog.isSelected()) {
        ItemChoice selected = list.getSelectionModel().getSelectedItem();
        return selected == null ? null : CarriedItem.of(CatalogRef.of(selected.id()));
      }
      Integer pounds = pounds(weight.getText());
      return CarriedItem.of(new CustomItem(name.getText().strip(), pounds == null ? 0 : pounds, effects.getText()));
    });
    return dialog.showAndWait();
  }

  /** A whole number of at least 0, or {@code null} for anything else. */
  private static @Nullable Integer pounds(@NonNull String text) {
    OptionalInt value = WholeNumber.parse(text);
    return value.isPresent() && value.getAsInt() >= 0 ? value.getAsInt() : null;
  }
}
