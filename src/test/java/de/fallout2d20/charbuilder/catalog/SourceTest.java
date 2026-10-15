package de.fallout2d20.charbuilder.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SourceTest {

  @Test
  void citesBookAndPrintedPage() {
    assertEquals("CRB p. 56", new Source(Book.CRB, 56).toString());
  }

  @Test
  void rejectsPagesBelowOne() {
    assertThrows(IllegalArgumentException.class, () -> new Source(Book.WOA, 0));
  }
}
