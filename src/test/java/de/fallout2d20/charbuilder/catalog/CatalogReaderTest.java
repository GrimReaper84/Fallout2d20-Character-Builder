package de.fallout2d20.charbuilder.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CatalogReaderTest {

  private static final String DIR = "/de/fallout2d20/charbuilder/catalog/";

  @Test
  void readsEntriesInFileOrder() {
    assertEquals(List.of(new Source(Book.CRB, 56), new Source(Book.WG, 13)),
        CatalogReader.readList(DIR + "test-sources.json", Source.class));
  }

  /** Bundled catalogs are part of the build: broken data must fail loudly and name the file. */
  @ParameterizedTest
  @ValueSource(strings = {"test-sources-unknown-field.json", "test-sources-missing-page.json",
      "does-not-exist.json"})
  void brokenCatalogFailsWithItsName(String file) {
    IllegalStateException error = assertThrows(IllegalStateException.class,
        () -> CatalogReader.readList(DIR + file, Source.class));
    assertTrue(error.getMessage().contains(file), error.getMessage());
  }
}
