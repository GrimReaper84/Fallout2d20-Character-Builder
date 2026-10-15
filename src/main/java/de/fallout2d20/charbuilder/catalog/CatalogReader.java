package de.fallout2d20.charbuilder.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Reads catalog files bundled with the application. Catalogs are part of the build, so a missing or
 * malformed file is a programming error: it fails immediately with an {@link IllegalStateException}
 * naming the file instead of starting with incomplete data.
 */
public final class CatalogReader {

  private static final @NonNull ObjectMapper MAPPER = new ObjectMapper();

  private CatalogReader() {}

  /**
   * Reads a JSON array of entries. Unknown properties are rejected, so typos in field names surface
   * as errors.
   *
   * @param resource absolute resource path, e.g. {@code /de/fallout2d20/charbuilder/catalog/origins.json}
   * @return the entries in file order, unmodifiable
   */
  public static <T> @NonNull List<T> readList(@NonNull String resource, @NonNull Class<T> type) {
    try (InputStream in = CatalogReader.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("Catalog not found: " + resource);
      }
      CollectionType listType = MAPPER.getTypeFactory().constructCollectionType(List.class, type);
      List<T> entries = MAPPER.readValue(in, listType);
      return List.copyOf(entries);
    } catch (IOException e) {
      throw new IllegalStateException("Invalid catalog " + resource + ": " + e.getMessage(), e);
    }
  }
}
