package de.fallout2d20.charbuilder.view.dialog;

import java.util.function.Function;
import javafx.scene.control.TableRow;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A row of a {@link CatalogPage} table: the entry's long values as tooltip, and a double click on a filled
 * row runs {@code onDoubleClick} (clicks on headers, scroll bars or the empty area below do not).
 *
 * @param <T> the catalog entry type
 */
final class TooltipRow<T> extends TableRow<T> {

  private final @NonNull Function<T, String> tooltip;

  TooltipRow(@NonNull Function<T, String> tooltip, @NonNull Runnable onDoubleClick) {
    this.tooltip = tooltip;
    setOnMouseClicked(event -> {
      if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !isEmpty()) {
        onDoubleClick.run();
      }
    });
  }

  @Override
  protected void updateItem(@Nullable T entry, boolean empty) {
    super.updateItem(entry, empty);
    String text = empty || entry == null ? "" : tooltip.apply(entry);
    setTooltip(text.isEmpty() ? null : new Tooltip(text));
  }
}
