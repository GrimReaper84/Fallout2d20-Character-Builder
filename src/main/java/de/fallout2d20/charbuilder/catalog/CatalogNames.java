package de.fallout2d20.charbuilder.catalog;

import java.util.Locale;
import org.jspecify.annotations.NonNull;

/** How catalog names are compared: ignoring case, surrounding blanks and the apostrophe form (’ or ') */
public final class CatalogNames {

  private CatalogNames() {}

  /** The comparable form of a name. */
  public static @NonNull String normalize(@NonNull String name) {
    return name.strip().replace('’', '\'').toLowerCase(Locale.ROOT);
  }
}
