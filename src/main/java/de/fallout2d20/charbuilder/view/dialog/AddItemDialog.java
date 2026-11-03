package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.item.ItemData;
import de.fallout2d20.charbuilder.catalog.item.MagazineData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.derived.Weight;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.CustomItem;
import de.fallout2d20.charbuilder.view.Layouts;
import de.fallout2d20.charbuilder.view.catalog.CatalogPage;
import de.fallout2d20.charbuilder.view.WholeNumber;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Adds items: catalog items and publications of the enabled books ({@link CatalogPage}, by the groups of the
 * Gear tree) or custom items with weight and effect, each in the chosen quantity. The issue of a magazine is
 * chosen in the inventory afterwards.
 */
public final class AddItemDialog {

  /** Upper bound of a typed quantity, so adding to a stack can not overflow. */
  private static final int MAX_QUANTITY = 9999;

  private AddItemDialog() {}

  /**
   * Shows the dialog until it is closed; every add goes to {@code onAdd} at once.
   *
   * @param categoryOf the Gear tree group of a catalog id
   * @param categories the Gear tree groups in tree order
   * @param onAdd enters an item and returns its index in the inventory
   * @return the index of the last added item; empty if none was added
   */
  public static @NonNull OptionalInt show(@Nullable Window owner, @NonNull List<ItemData> items,
      @NonNull List<MagazineData> magazines, @NonNull Function<String, String> categoryOf,
      @NonNull List<String> categories, @NonNull ToIntFunction<CarriedItem> onAdd) {
    List<ItemChoice> choices = new ArrayList<>();
    items.forEach(item -> choices.add(ItemChoice.of(item)));
    magazines.forEach(magazine -> choices.add(ItemChoice.of(magazine)));
    Function<ItemChoice, String> category = choice -> categoryOf.apply(choice.id());
    CatalogPage<ItemChoice> page = new CatalogPage<>(choices, ItemChoice::name, category, categories,
        ItemChoice::effect);
    page.addColumn("Category", category);
    page.addNumberColumn("Weight", choice -> Weight.pounds(choice.weight()), AddItemDialog::weightText);
    page.addColumn("Effect", ItemChoice::effect);
    page.addColumn("Source", ItemChoice::source);
    Tab catalog = new Tab("Catalog", page.node());

    TextField name = new TextField();
    TextField weight = new TextField("0");
    TextField effects = new TextField();
    MigPane form = Layouts.dialogForm();
    form.add(new Label("Name"));
    form.add(name);
    form.add(new Label("Weight (lbs., 0 for <1)"));
    form.add(weight);
    form.add(new Label("Effect"));
    form.add(effects);
    Tab custom = new Tab("Custom item", form);
    TabPane tabs = new TabPane(catalog, custom);
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    TextField quantity = new TextField("1");
    quantity.setPrefColumnCount(4);

    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle("Add item");
    dialog.setHeaderText("Choose items or publications from the catalog or describe a custom one.");
    AtomicInteger last = new AtomicInteger(-1);
    Supplier<@Nullable CarriedItem> pick = () -> {
      Integer count = WholeNumber.between(quantity.getText(), 1, MAX_QUANTITY);
      CarriedItem item = catalog.isSelected() ? catalogItem(page.selected()) : customItem(name, weight, effects);
      return item == null || count == null ? null : item.withQuantity(count);
    };
    Label status = AddButtons.install(dialog, Bindings.createBooleanBinding(() -> pick.get() == null,
        catalog.selectedProperty(), page.selectedItemProperty(), name.textProperty(), weight.textProperty(),
        quantity.textProperty()), () -> {
          CarriedItem added = pick.get();
          if (added == null) {
            return null;
          }
          last.set(onAdd.applyAsInt(added));
          return (added.quantity() > 1 ? added.quantity() + " × " : "") + added.displayName();
        });
    page.setOnActivate(() -> AddButtons.addNow(dialog));
    dialog.getDialogPane().setContent(new VBox(Layouts.GAP, tabs,
        new HBox(Layouts.GAP, new Label("Quantity"), quantity), status));
    dialog.showAndWait();
    return last.get() < 0 ? OptionalInt.empty() : OptionalInt.of(last.get());
  }

  private static @Nullable CarriedItem catalogItem(@Nullable ItemChoice selected) {
    return selected == null ? null : CarriedItem.of(CatalogRef.of(selected.id()));
  }

  private static @Nullable CarriedItem customItem(@NonNull TextField name, @NonNull TextField weight,
      @NonNull TextField effects) {
    Integer pounds = WholeNumber.atLeast(weight.getText(), 0);
    if (name.getText().isBlank() || pounds == null) {
      return null;
    }
    return CarriedItem.of(new CustomItem(name.getText().strip(), pounds, effects.getText()));
  }

  private static @NonNull String weightText(@NonNull ItemChoice choice) {
    String weight = choice.weight();
    return weight == null ? "–" : weight;
  }
}
