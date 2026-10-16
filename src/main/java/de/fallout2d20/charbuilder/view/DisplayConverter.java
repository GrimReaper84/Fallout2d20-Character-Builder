package de.fallout2d20.charbuilder.view;

import java.util.function.Function;
import javafx.util.StringConverter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Shows items of a non-editable choice control by a display text (e.g. a catalog name). */
public final class DisplayConverter<T> extends StringConverter<T> {

  private final @NonNull Function<T, String> text;

  public DisplayConverter(@NonNull Function<T, String> text) {
    this.text = text;
  }

  @Override
  public @NonNull String toString(@Nullable T value) {
    return value == null ? "" : text.apply(value);
  }

  /** Not used: the controls using this converter are not editable. */
  @Override
  public @Nullable T fromString(@Nullable String string) {
    return null;
  }
}
