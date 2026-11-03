package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.apparel.CustomApparel;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.view.Layouts;
import de.fallout2d20.charbuilder.view.catalog.CatalogColumns;
import de.fallout2d20.charbuilder.view.catalog.CatalogPage;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Adds apparel: catalog pieces offered to the character ({@link CatalogPage}, by the groups of the Armor
 * tree for the character's body) or custom pieces ({@link CustomApparelForm}).
 */
public final class AddApparelDialog {

  private AddApparelDialog() {}

  /**
   * Shows the dialog until it is closed; every add goes to {@code onAdd} at once.
   *
   * @param items the catalog items to offer (the caller filters them for the character)
   * @param categoryOf the Armor tree group of a catalog item
   * @param categories the Armor tree groups in tree order
   * @param locations the character's hit locations, offered for a custom item
   * @param defaultKind the preselected kind of a custom item (robot armor for a Mister Handy)
   * @param books the character's enabled books, for the mod tables of a custom item
   * @param onAdd enters a piece and returns its index
   * @return the index of the last added piece; empty if none was added
   */
  public static @NonNull OptionalInt show(@Nullable Window owner, @NonNull List<ApparelData> items,
      @NonNull Function<ApparelData, String> categoryOf, @NonNull List<String> categories,
      @NonNull List<HitLocation> locations, @NonNull ApparelKind defaultKind, @NonNull Set<Book> books,
      @NonNull ToIntFunction<WornApparel> onAdd) {
    CatalogPage<ApparelData> page = CatalogColumns.apparel(items, categoryOf, categories);
    Tab catalog = new Tab("Catalog", page.node());
    CustomApparelForm form = new CustomApparelForm(locations, defaultKind, books);
    Tab custom = new Tab("Custom item", form.node());
    TabPane tabs = new TabPane(catalog, custom);
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle("Add apparel");
    dialog.setHeaderText("Choose items from the catalog or describe a custom one.");
    AtomicInteger last = new AtomicInteger(-1);
    List<Observable> dependencies = new ArrayList<>(List.of(catalog.selectedProperty(), page.selectedItemProperty()));
    dependencies.addAll(form.dependencies());
    Label status = AddButtons.install(dialog, Bindings.createBooleanBinding(
        () -> piece(catalog.isSelected(), page.selected(), form.apparel()) == null,
        dependencies.toArray(Observable[]::new)), () -> {
          WornApparel piece = piece(catalog.isSelected(), page.selected(), form.apparel());
          if (piece == null) {
            return null;
          }
          last.set(onAdd.applyAsInt(piece));
          return piece.displayName();
        });
    page.setOnActivate(() -> AddButtons.addNow(dialog));
    dialog.getDialogPane().setContent(new VBox(Layouts.GAP, tabs, status));
    dialog.showAndWait();
    return last.get() < 0 ? OptionalInt.empty() : OptionalInt.of(last.get());
  }

  private static @Nullable WornApparel piece(boolean fromCatalog, @Nullable ApparelData selected,
      @Nullable CustomApparel described) {
    if (fromCatalog) {
      return selected == null ? null : WornApparel.of(CatalogRef.of(selected.id()));
    }
    return described == null ? null : WornApparel.of(described);
  }
}
