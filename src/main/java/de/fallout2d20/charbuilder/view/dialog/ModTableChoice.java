package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModTable;
import de.fallout2d20.charbuilder.view.DisplayConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The choice of a custom piece's mod table (user decision 2026-10-22): "No mods" or one of the tables fitting its
 * kind ({@link ApparelModTable#fitting}). The items are optionals because a combo box shows no text for a
 * {@code null} item; empty stands for "No mods".
 */
public final class ModTableChoice {

  private final @NonNull ComboBox<Optional<ApparelModTable>> box = new ComboBox<>();

  public ModTableChoice(@NonNull ApparelKind kind, @Nullable ApparelModTable selected) {
    box.setConverter(new DisplayConverter<>(table -> table.map(ApparelModTable::displayName).orElse("No mods")));
    box.setMaxWidth(Region.USE_PREF_SIZE);
    box.setTooltip(new Tooltip("The mod table this custom piece takes its mods from."));
    fitTo(kind);
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
    ApparelModTable.fitting(kind).forEach(table -> items.add(Optional.of(table)));
    box.getItems().setAll(items);
    select(previous);
  }
}
