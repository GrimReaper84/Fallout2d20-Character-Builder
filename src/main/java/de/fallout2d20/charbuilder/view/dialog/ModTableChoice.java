package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModTable;
import de.fallout2d20.charbuilder.view.DisplayConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The choice of a custom piece's mod table (user decision 2026-10-22): "No mods" or one of the tables fitting its
 * kind with mods of the character's books ({@link ApparelModTable#fitting}); a table chosen earlier stays on offer
 * when its book was switched off since. The items are optionals because a combo box shows no text for a
 * {@code null} item; empty stands for "No mods".
 */
public final class ModTableChoice {

  private final @NonNull ComboBox<Optional<ApparelModTable>> box = new ComboBox<>();
  private final @NonNull Set<Book> books;

  /** @param books the character's enabled books */
  public ModTableChoice(@NonNull ApparelKind kind, @Nullable ApparelModTable selected, @NonNull Set<Book> books) {
    this.books = Set.copyOf(books);
    box.setConverter(new DisplayConverter<>(table -> table.map(ApparelModTable::displayName).orElse("No mods")));
    box.setMaxWidth(Region.USE_PREF_SIZE);
    box.setTooltip(new Tooltip("The mod table this custom piece takes its mods from."));
    fitTo(kind);
    if (selected != null && !box.getItems().contains(Optional.of(selected))) {
      box.getItems().add(Optional.of(selected));
    }
    select(selected);
  }

  public @NonNull ComboBox<Optional<ApparelModTable>> node() {
    return box;
  }

  /** The chosen table; {@code null} for "No mods". */
  public @Nullable ApparelModTable value() {
    Optional<ApparelModTable> value = box.getValue();
    return value == null ? null : value.orElse(null);
  }

  /** Selects the table, or "No mods" for {@code null} or a table not on offer. */
  public void select(@Nullable ApparelModTable table) {
    Optional<ApparelModTable> item = Optional.ofNullable(table);
    box.setValue(box.getItems().contains(item) ? item : Optional.empty());
  }

  /** Offers the tables fitting the kind; keeps the chosen table if it still fits, else "No mods". */
  public void fitTo(@NonNull ApparelKind kind) {
    ApparelModTable previous = value();
    List<Optional<ApparelModTable>> items = new ArrayList<>();
    items.add(Optional.empty());
    ApparelModTable.fitting(kind, books).forEach(table -> items.add(Optional.of(table)));
    box.getItems().setAll(items);
    select(previous);
  }
}
