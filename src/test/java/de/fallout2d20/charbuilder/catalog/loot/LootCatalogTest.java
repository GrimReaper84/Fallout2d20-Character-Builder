package de.fallout2d20.charbuilder.catalog.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.item.ItemCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Spot checks of the loot tables against CRB pp. 172, 200-208 and WG pp. 108, 120-125; gaps fail on loading. */
class LootCatalogTest {

  private static String name(String table, int roll) {
    return LootCatalog.table(table).row(roll).orElseThrow().name();
  }

  @Test
  void everyCategoryHasItsTable() {
    for (LootCategory category : LootCategory.values()) {
      assertEquals(category == LootCategory.ODDITIES ? 3 : category == LootCategory.JUNK ? 1 : 2,
          LootCatalog.table(category.tableId()).dice(), category.name());
    }
  }

  @Test
  void rowsTheScriptGotWrongComeFromTheBook() {
    assertEquals("Heavy Coat", name("clothing", 28));
    assertEquals("Cage Armor", name("clothing", 39));
    assertEquals("{} Caps", name("oddities", 17));
    assertEquals("4d20", LootCatalog.table("oddities").row(17).orElseThrow().quantity());
    assertEquals("Pre-War Money worth {} Caps", name("oddities", 16));
  }

  /** CRB pp. 200-208: first and last rows of the tables. */
  @Test
  void firstAndLastRows() {
    assertEquals("2mm EC", name("ammunition", 2));
    assertEquals("Mini-Nuke", name("ammunition", 40));
    assertEquals("X-01 Power Armor Piece", name("armor", 2));
    assertEquals("Heavy Synth Armor", name("armor", 40));
    assertEquals("Tarberry", name("food", 40));
    assertEquals("Wine", name("beverages", 40));
    assertEquals("Super Stimpak", name("chems", 2));
    assertEquals("X-Cell", name("chems", 40));
    assertEquals("Fat Man", name("weapons-ranged", 2));
    assertEquals("Gamma Gun", name("weapons-ranged", 40));
    assertEquals("Deathclaw Gauntlet", name("weapons-melee", 2));
    assertEquals("Nuke Mine", name("weapons-thrown", 40));
    assertEquals("Regeneration Field", name("oddities", 3));
    assertEquals("Tesla Coils", name("oddities", 60));
  }

  /** CRB p. 204 prints no rows for 10 and 19. */
  @Test
  void rangedWeaponsLeaveOutWhatTheBookLeavesOut() {
    LootTable table = LootCatalog.table("weapons-ranged");
    assertEquals(List.of(10, 19), table.missingRolls());
    assertTrue(table.row(10).isEmpty());
    assertTrue(table.row(19).isEmpty());
  }

  @Test
  void tablesCiteTheirBooks() {
    assertEquals(new Source(Book.CRB, 172), LootCatalog.table("magazine").source());
    assertEquals(new Source(Book.WG, 108), LootCatalog.table("rare-book").source());
    assertEquals(new Source(Book.WG, 120), LootCatalog.table("junk").source());
  }

  @Test
  void aTableWithAGapIsRejected() {
    LootRow low = new LootRow(1, 5, "Low", null, null, null, null, false, null);
    LootRow high = new LootRow(7, 20, "High", null, null, null, null, false, null);
    assertThrows(IllegalArgumentException.class,
        () -> new LootTable("gap", "Gap", new Source(Book.CRB, 1), 1, null, null, List.of(low, high)));
  }

  @Test
  void everyRefAndPieceExists() {
    for (LootTable table : LootCatalog.all()) {
      for (LootRow row : table.rows()) {
        LootRef ref = row.ref();
        if (ref != null) {
          boolean exists = switch (ref.kind()) {
            case WEAPON -> WeaponCatalog.byId(ref.id()).isPresent();
            case AMMUNITION -> WeaponCatalog.ammunitionById(ref.id()).isPresent();
            case APPAREL -> ApparelCatalog.byId(ref.id()).isPresent();
            case ITEM -> ItemCatalog.byId(ref.id()).isPresent();
            case MAGAZINE -> ItemCatalog.magazineById(ref.id()).isPresent();
          };
          assertTrue(exists, table.id() + ": " + row.name());
        }
        if (row.pieces() != null) {
          assertTrue(ApparelCatalog.byId(row.pieces() + "-chest-piece").isPresent(), row.name());
        }
      }
    }
  }
}
