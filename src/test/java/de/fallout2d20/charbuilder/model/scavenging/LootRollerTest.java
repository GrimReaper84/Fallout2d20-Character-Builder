package de.fallout2d20.charbuilder.model.scavenging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.FixedDice;
import de.fallout2d20.charbuilder.catalog.loot.LootCategory;
import de.fallout2d20.charbuilder.catalog.loot.LootRef;
import de.fallout2d20.charbuilder.catalog.loot.LootRefKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class LootRollerTest {

  private static LootRoller roller(Integer... results) {
    return new LootRoller(FixedDice.of(results));
  }

  private static Find roll(LootCategory category, Integer... results) {
    return roller(results).roll(category, FindOrigin.BASIC, JunkMode.COUNT);
  }

  @Test
  void tableLookup() {
    Find find = roll(LootCategory.CLOTHING, 14, 14);
    assertEquals("Heavy Coat", find.text());
    assertEquals(28, find.roll());
    assertEquals(new LootRef(LootRefKind.APPAREL, "heavy-coat"), find.ref());
  }

  /** CRB p. 200: 19 is 10mm with 8+4 CD, the table row prints the formula. */
  @Test
  void quantitiesAreRolled() {
    Find find = roll(LootCategory.AMMUNITION, 10, 9, 1, 2, 3, 5);
    assertEquals(12, find.quantity());
    assertEquals("12 × 10mm", find.text());
    assertEquals(List.of("2d20 19", "8+4 CD = 12"), find.steps());
  }

  /** CRB p. 200: the footnote stays with the row; the Scrounger remark of the table is added. */
  @Test
  void ammunitionFootnotes() {
    Find missile = roll(LootCategory.AMMUNITION, 3, 3, 1);
    assertTrue(missile.note().startsWith("Scrounger: only +1 CD per rank."));
    assertTrue(missile.note().contains("CRB p. 71"));
  }

  /** CRB p. 207: the name holds the rolled caps. */
  @Test
  void capsAreNamedWithTheirAmount() {
    Find caps = roll(LootCategory.ODDITIES, 5, 6, 6, 2, 3, 4, 5);
    assertEquals(17, caps.roll());
    assertEquals("14 Caps", caps.text());
  }

  /** CRB p. 204: the book has no row for 10 and 19, so these rolls are rolled again. */
  @Test
  void aRollWithoutARowIsRolledAgain() {
    Find find = roll(LootCategory.WEAPONS_RANGED, 5, 5, 1, 1, 3);
    assertEquals("Fat Man", find.name());
    assertEquals("2d20 10: no row in the book, rolled again", find.steps().getFirst());
  }

  /** CRB p. 204: a ranged weapon brings its standard ammunition, in the amount of the loot table. */
  @Test
  void aRangedWeaponComesWithAmmunition() {
    Find find = roll(LootCategory.WEAPONS_RANGED, 6, 7, 1, 1, 1);
    assertEquals("Hunting Rifle", find.text());
    assertEquals(new FoundAmmunition(9, ".308 Ammo", new LootRef(LootRefKind.AMMUNITION, "308")), find.ammunition());
    assertEquals("Hunting Rifle, with 9 × .308 Ammo", find.fullText());
  }

  /** The Submachine Gun's .45 rolls 9+4 CD as on the loot table (CRB p. 200), not the catalog's 8+4 CD. */
  @Test
  void weaponAmmunitionUsesTheLootTableAmount() {
    Find find = roll(LootCategory.WEAPONS_RANGED, 8, 8, 3, 3, 3, 3);
    assertEquals("Submachine Gun", find.name());
    assertEquals(9, find.ammunition().quantity());
  }

  /** The Junk Jet fires anything; its ammunition is not on the loot table. */
  @Test
  void aWeaponWithoutATableAmmunitionComesWithout() {
    Find find = roll(LootCategory.WEAPONS_RANGED, 3, 4);
    assertEquals("Junk Jet", find.name());
    assertNull(find.ammunition());
  }

  /** CRB p. 201: Raider armor has no helmet, so a head result is rolled again. */
  @Test
  void armorRerollsLocationsWithoutAPiece() {
    Find find = roll(LootCategory.ARMOR, 10, 11, 1, 9);
    assertEquals("Raider Arm (Left Arm)", find.name());
    assertEquals(new LootRef(LootRefKind.APPAREL, "raider-arm"), find.ref());
    assertEquals(List.of("2d20 21", "location d20 1: no Head piece, rolled again", "location d20 9: Left Arm"),
        find.steps());
  }

  @Test
  void powerArmorPiecesHaveEveryLocationAndNoLinkYet() {
    Find find = roll(LootCategory.ARMOR, 1, 1, 2);
    assertEquals("X-01 Power Armor Piece (Head)", find.name());
    assertNull(find.ref());
  }

  /** CRB p. 201: frames ask for no location. */
  @Test
  void framesRollNoLocation() {
    Find find = roll(LootCategory.ARMOR, 2, 3);
    assertEquals("Power Armor Frame", find.name());
    assertEquals(List.of("2d20 5"), find.steps());
  }

  /** CRB p. 172: a magazine rolls its publication; WG p. 108: 1 Luck makes it a Rare Book. */
  @Test
  void magazineAndRareBook() {
    LootRoller roller = roller(5, 5, 5, 3, 1);
    Find magazine = roller.roll(LootCategory.ODDITIES, FindOrigin.BASIC, JunkMode.COUNT);
    assertEquals("Backwoodsman", magazine.name());
    assertTrue(magazine.magazine());
    Find book = roller.rareBook(magazine);
    assertEquals("101 Heroic Last Stands", book.name());
    assertEquals(LootRefKind.ITEM, book.ref().kind());
    assertTrue(book.luckUsed());
    assertFalse(book.magazine());
  }

  /** CRB p. 208: 2d20 is the number of junk items; 1 Luck adds the LCK score. */
  @Test
  void junkCount() {
    LootRoller roller = roller(7, 9);
    Find junk = roller.roll(LootCategory.JUNK, FindOrigin.BASIC, JunkMode.COUNT);
    assertEquals("Junk items: 16", junk.text());
    assertNull(junk.tableId());
    Find lucky = roller.addLuck(junk, 5);
    assertEquals("Junk items: 21", lucky.text());
    assertTrue(lucky.luckUsed());
  }

  /** WG p. 120: a d20 selects the table, a d20 on it the item. */
  @Test
  void detailedJunk() {
    Find find = roller(5, 3).roll(LootCategory.JUNK, FindOrigin.BASIC, JunkMode.DETAILED);
    assertEquals("Ashtray", find.name());
    assertEquals("junk-household-1", find.tableId());
    assertEquals(3, find.roll());
    assertEquals(LootRefKind.ITEM, find.ref().kind());
  }

  /** WG p. 121: 12-20 of the animal table leads to food; WG p. 123: 19-20 of Household 4 to the workplace table. */
  @Test
  void junkRowsCanSendToOtherTables() {
    assertEquals("Carrot", roller(1, 12, 7, 7).roll(LootCategory.JUNK, FindOrigin.BASIC, JunkMode.DETAILED).name());
    assertEquals("Telephone", roller(8, 19, 20).roll(LootCategory.JUNK, FindOrigin.BASIC, JunkMode.DETAILED).name());
  }

  /** WG p. 120: Adjust works on the junk table, also when its row sent the roll on to another table. */
  @Test
  void adjustingAJunkFindMovesOnTheJunkTable() {
    LootRoller roller = roller(1, 12, 7, 7);
    Find food = roller.roll(LootCategory.JUNK, FindOrigin.BASIC, JunkMode.DETAILED);
    assertEquals("junk-animal", food.tableId());
    assertEquals(12, food.roll());
    assertEquals(List.of(11), roller.options(food, 1).stream().map(LootOption::roll).toList());
    assertEquals("Radscorpion Stinger", roller.shift(food, 11).name());
  }

  /** CRB p. 200: Luck reaches rows within the level, within the table, each row once at its nearest roll. */
  @Test
  void luckOptions() {
    LootRoller roller = roller(20, 20);
    Find find = roller.roll(LootCategory.CHEMS, FindOrigin.BASIC, JunkMode.COUNT);
    assertEquals(40, find.roll());
    List<LootOption> options = roller.options(find, 3);
    assertEquals(List.of(37, 38, 39), options.stream().map(LootOption::roll).toList());
    assertEquals("Antibiotics", options.getFirst().row().name());
  }

  @Test
  void luckOptionsSkipTheCurrentRowAndMissingRolls() {
    LootRoller roller = roller(4, 5, 1, 1, 1, 1, 1);
    Find find = roller.roll(LootCategory.WEAPONS_RANGED, FindOrigin.BASIC, JunkMode.COUNT);
    assertEquals(9, find.roll());
    assertEquals(List.of(7, 8, 11), roller.options(find, 2).stream().map(LootOption::roll).toList());
  }

  @Test
  void shiftReplacesTheNestedResult() {
    LootRoller roller = roller(5, 5, 5, 3);
    Find magazine = roller.roll(LootCategory.ODDITIES, FindOrigin.AP, JunkMode.COUNT);
    Find shifted = roller.shift(magazine, 14);
    assertEquals("Doctor’s Bag", shifted.name());
    assertEquals(FindOrigin.AP, shifted.origin());
    assertEquals("3d20 15, shifted to 14", shifted.steps().getFirst());
    assertTrue(shifted.luckUsed());
    assertFalse(shifted.magazine());
  }
}
