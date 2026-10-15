package de.fallout2d20.charbuilder.persistence.legacy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.origin.OriginCatalog;
import java.util.Collection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class LegacyOriginIdsTest {

  /** Every id a supported legacy origin maps to must exist in the origin catalog. */
  @ParameterizedTest
  @MethodSource("supportedIds")
  void supportedLegacyOriginsExistInTheCatalog(String id) {
    assertTrue(OriginCatalog.byId(id).isPresent(), id);
  }

  static Collection<String> supportedIds() {
    return LegacyOriginIds.supportedIds();
  }
}
