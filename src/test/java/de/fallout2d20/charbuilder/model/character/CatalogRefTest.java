package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CatalogRefTest {

  @Test
  void derivesKebabCaseIds() {
    assertEquals("jury-rigging", CatalogRef.toId("Jury Rigging"));
    assertEquals("action-boy-girl", CatalogRef.toId("Action Boy/Girl"));
    assertEquals("black-widow-lady-killer", CatalogRef.toId("Black Widow/Lady Killer"));
    assertEquals("tag", CatalogRef.toId("Tag!"));
    assertEquals("can-do", CatalogRef.toId("Can Do!"));
  }

  @Test
  void legacyReferenceKeepsTheOriginalName() {
    CatalogRef ref = CatalogRef.fromLegacyName("Jury Rigging");
    assertEquals("jury-rigging", ref.id());
    assertEquals("Jury Rigging", ref.displayName());
    assertNull(CatalogRef.of("survivor").legacyName());
    assertEquals("survivor", CatalogRef.of("survivor").displayName());
  }
}
