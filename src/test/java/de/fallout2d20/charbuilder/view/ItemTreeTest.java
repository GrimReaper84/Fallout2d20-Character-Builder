package de.fallout2d20.charbuilder.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.CustomItem;
import de.fallout2d20.charbuilder.model.character.item.Holotape;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Grouping of the item tree by type and the quantity in brackets (user decision 2026-10-19). */
class ItemTreeTest {

  @Test
  void groupsByType() {
    assertEquals(ItemGroup.CHEM, ItemGroup.of(CarriedItem.of(CatalogRef.of("stimpak"))));
    assertEquals(ItemGroup.BEVERAGE, ItemGroup.of(CarriedItem.of(CatalogRef.of("nuka-cola"))));
    assertEquals(ItemGroup.MAGAZINES, ItemGroup.of(CarriedItem.of(CatalogRef.of("tumblers-today"))));
    assertEquals(ItemGroup.HOLOTAPES, ItemGroup.of(CarriedItem.of(new Holotape("Code", ""))));
    assertEquals(ItemGroup.CUSTOM, ItemGroup.of(CarriedItem.of(new CustomItem("Teddy bear", 1, ""))));
    assertEquals(ItemGroup.CUSTOM, ItemGroup.of(CarriedItem.of(CatalogRef.of("gone"))), "unknown id");
  }

  @Test
  void groupsInOrderWithNamesSortedAndQuantities() {
    List<ItemEntry> entries = ItemTree.entries(List.of(CarriedItem.of(CatalogRef.of("stimpak")).withQuantity(2),
        CarriedItem.of(new Holotape("Code", "")), CarriedItem.of(CatalogRef.of("rad-x")),
        CarriedItem.of(CatalogRef.of("nuka-cola"))));
    Map<ItemGroup, List<ItemEntry>> grouped = ItemTree.grouped(entries);
    assertEquals(List.of(ItemGroup.BEVERAGE, ItemGroup.CHEM, ItemGroup.HOLOTAPES), List.copyOf(grouped.keySet()));
    assertEquals(List.of("Rad-X [1]", "Stimpak [2]"),
        grouped.get(ItemGroup.CHEM).stream().map(ItemEntry::label).toList());
    assertEquals(0, grouped.get(ItemGroup.CHEM).get(1).index());
  }
}
