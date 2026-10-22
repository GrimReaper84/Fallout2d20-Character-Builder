package de.fallout2d20.charbuilder.view.dialog;

import java.util.function.Function;
import java.util.function.Predicate;
import javafx.scene.control.TableRow;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A row of a mod dialog: the mod's details as tooltip, marked when the character lacks a needed perk. */
final class ModChoiceRow<T> extends TableRow<ModChoice<T>> {

  private static final @NonNull String MISSING_STYLE = "-fx-text-fill: #a35200;";

  private final @NonNull Function<T, String> details;
  private final @NonNull Predicate<T> lacksPerk;

  ModChoiceRow(@NonNull Function<T, String> details, @NonNull Predicate<T> lacksPerk) {
    this.details = details;
    this.lacksPerk = lacksPerk;
  }

  @Override
  protected void updateItem(@Nullable ModChoice<T> choice, boolean empty) {
    super.updateItem(choice, empty);
    T mod = empty || choice == null ? null : choice.mod();
    boolean missing = mod != null && lacksPerk.test(mod);
    setStyle(missing ? MISSING_STYLE : "");
    if (mod == null) {
      setTooltip(null);
      return;
    }
    String note = missing ? "\nThe character lacks a needed perk (information only)." : "";
    setTooltip(new Tooltip(details.apply(mod) + note));
  }
}
