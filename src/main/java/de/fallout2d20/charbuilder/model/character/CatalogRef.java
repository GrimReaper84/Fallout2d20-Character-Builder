package de.fallout2d20.charbuilder.model.character;

import java.util.Locale;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Reference to a catalog entry (origin, trait, perk, ...) by its stable id.
 *
 * <p>The catalogs are introduced by later features; until a reference can be resolved, its
 * {@code legacyName} (the name or id found in a legacy save) keeps it displayable.
 *
 * @param id catalog id in kebab-case, e.g. {@code jury-rigging}
 * @param legacyName name as stored in a legacy file, {@code null} for references created by this app
 */
public record CatalogRef(@NonNull String id, @Nullable String legacyName) {

  public CatalogRef {
    if (id.isBlank()) {
      throw new IllegalArgumentException("Catalog id must not be blank");
    }
  }

  public static @NonNull CatalogRef of(@NonNull String id) {
    return new CatalogRef(id, null);
  }

  /** Creates a reference whose id is derived from a legacy name, see {@link #toId(String)}. */
  public static @NonNull CatalogRef fromLegacyName(String legacyName) {
    return new CatalogRef(toId(legacyName), legacyName);
  }

  /**
   * Derives a catalog id from a printed or legacy name: lower case, {@code &} becomes {@code and},
   * apostrophes, dots and exclamation marks are removed, every other non-alphanumeric run becomes a
   * single dash. Example: {@code Action Boy/Girl} becomes {@code action-boy-girl}.
   */
  public static @NonNull String toId(@NonNull String name) {
    String id = slug(name);
    if (id.isEmpty()) {
      throw new IllegalArgumentException("Name yields no id: " + name);
    }
    return id;
  }

  /** Whether {@link #toId(String)} can derive an id, i.e. the name contains a letter or digit. */
  public static boolean canDeriveId(@NonNull String name) {
    return !slug(name).isEmpty();
  }

  private static @NonNull String slug(@NonNull String name) {
    return name.toLowerCase(Locale.ROOT)
        .replace("&", " and ")
        .replaceAll("['’.!]", "")
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-|-$", "");
  }

  /** Text to show while no catalog entry is resolved: the legacy name if present, else the id. */
  public String displayName() {
    return legacyName != null ? legacyName : id;
  }
}
