package de.fallout2d20.charbuilder.model.character.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.StartingEquipmentAllowance;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Inventory items, caps and materials of a character. */
class PlayerCharacterInventoryTest {

  @Test
  void itemsHaveQuantityIssueAndNotes() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.addItem(CarriedItem.of(CatalogRef.of("stimpak")));
    character.addItem(CarriedItem.of(CatalogRef.of("grognak-the-barbarian")));
    character.addItem(CarriedItem.of(new CustomItem("Teddy bear", 1, "")));
    character.setItemQuantity(0, 3);
    character.setItemIssue(1, "Fatherless Cur!");
    character.setItemNotes(2, "Lucky charm");

    List<CarriedItem> items = character.getItems();
    assertEquals("Stimpak", items.getFirst().displayName());
    assertEquals(3, items.getFirst().quantity());
    assertEquals("Heals 4 HP", items.getFirst().effects());
    assertEquals("Grognak the Barbarian", items.get(1).displayName());
    assertEquals("Ignore one complication on a melee attack", items.get(1).effects());
    assertEquals("<1", items.get(1).printedWeight(), "CRB p. 172");
    assertEquals("Lucky charm", items.get(2).notes());
    character.removeItem(0);
    assertEquals(2, character.getItems().size());
    assertThrows(IllegalArgumentException.class, () -> character.setItemQuantity(0, 0));
  }

  /** A magazine without a known issue shows the publication's perk. */
  @Test
  void magazineWithoutIssueShowsItsSummary() {
    CarriedItem fantoma = CarriedItem.of(CatalogRef.of("la-fantoma"));
    assertEquals(fantoma.magazine().orElseThrow().summary(), fantoma.effects());
  }

  /** CRB pp. 76–81: "Create Character" fills the caps with pack caps and the level caps; the d20 part is rolled. */
  @Test
  void completingCreationFillsTheFixedStartingCaps() {
    PlayerCharacter settler = settler();
    settler.completeCreation();
    assertEquals(45, settler.getCaps());

    PlayerCharacter veteran = settler();
    veteran.setStartingLevel(3);
    veteran.completeCreation();
    assertEquals(45 + StartingEquipmentAllowance.caps(3), veteran.getCaps());

    PlayerCharacter entered = settler();
    entered.setCaps(60);
    entered.completeCreation();
    assertEquals(60, entered.getCaps(), "a balance already entered is kept");
    assertThrows(IllegalArgumentException.class, () -> entered.setCaps(-1));
  }

  /** CRB p. 208: junk 2 lbs. each, materials 1 lb. per unit. */
  @Test
  void materialsWeight() {
    assertEquals(2 * 3 + 4 + 2 + 1, new Materials(3, 4, 2, 1).weight());
    assertThrows(IllegalArgumentException.class, () -> new Materials(-1, 0, 0, 0));
  }

  private static PlayerCharacter settler() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.chooseEquipmentPack(CatalogRef.of("settler"));
    return character;
  }
}
