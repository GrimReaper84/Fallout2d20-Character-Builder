package de.fallout2d20.charbuilder.view.compendium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.loot.LootCatalog;
import de.fallout2d20.charbuilder.catalog.loot.LootCategory;
import de.fallout2d20.charbuilder.catalog.loot.LootRef;
import de.fallout2d20.charbuilder.catalog.loot.LootRefKind;
import de.fallout2d20.charbuilder.catalog.loot.LootRow;
import de.fallout2d20.charbuilder.catalog.loot.LootTable;
import de.fallout2d20.charbuilder.model.scavenging.Find;
import de.fallout2d20.charbuilder.model.scavenging.FindOrigin;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FindReferencesTest {

  private static Find find(LootRef ref) {
    return new Find(LootCategory.ODDITIES, FindOrigin.BASIC, null, 3, "Result", 1, null, ref, List.of(), false, false, null);
  }

  @Test
  void aFindWithoutACatalogEntryHasNoLink() {
    assertEquals(Optional.empty(), FindReferences.of(find(null)));
  }

  @Test
  void magazinesLinkToTheirCompendiumEntry() {
    Reference reference = FindReferences.of(find(new LootRef(LootRefKind.MAGAZINE, "boxing-times"))).orElseThrow();
    assertEquals(Section.ITEMS, reference.section());
    assertEquals(EquipmentEntries.magazineId("boxing-times"), reference.id());
  }

  /** Every link of every table reaches an entry that the compendium lists with all books. */
  @Test
  void everyRowLinksToAListedEntry() {
    Set<Book> books = EnumSet.allOf(Book.class);
    for (LootTable table : LootCatalog.all()) {
      for (LootRow row : table.rows()) {
        if (row.ref() != null) {
          Reference reference = FindReferences.of(find(row.ref())).orElseThrow();
          assertTrue(CompendiumContent.of(reference.section(), books).entry(reference.id()).isPresent(),
              table.id() + ": " + row.name());
        }
      }
    }
  }

  /** Core tables link only to what the Core Rulebook lists; WG tables are the only ones reaching WG entries. */
  @Test
  void coreTablesLinkToCoreEntries() {
    Set<Book> books = EnumSet.of(Book.CRB);
    for (LootTable table : LootCatalog.all()) {
      if (table.source().book() == Book.CRB) {
        for (LootRow row : table.rows()) {
          if (row.ref() != null) {
            Reference reference = FindReferences.of(find(row.ref())).orElseThrow();
            assertTrue(CompendiumContent.of(reference.section(), books).entry(reference.id()).isPresent(),
                table.id() + ": " + row.name());
          }
        }
      }
    }
  }
}
