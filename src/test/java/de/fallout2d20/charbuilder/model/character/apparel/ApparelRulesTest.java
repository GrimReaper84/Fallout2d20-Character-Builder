package de.fallout2d20.charbuilder.model.character.apparel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/** Soft apparel hints (CRB p. 130, 146; user decision: never a block). */
class ApparelRulesTest {

  private static Character character(String origin) {
    Character character = Character.createNew();
    character.chooseOrigin(CatalogRef.of(origin));
    return character;
  }

  private static void wear(Character character, String id, @Nullable HitLocation location) {
    character.addApparel(WornApparel.of(CatalogRef.of(id)).withLocation(location));
  }

  private static List<String> hints(Character character) {
    return ApparelRules.problems(character).stream().map(CreationProblem::message).toList();
  }

  @Test
  void conflictsOfAHumanoid() {
    Character survivor = character("survivor");
    wear(survivor, "hazmat-suit", null);
    wear(survivor, "raider-chest-piece", null);
    wear(survivor, "metal-chest-piece", null);
    wear(survivor, "leather-arm", null);
    wear(survivor, "standard-plating", null);
    assertEquals(List.of("Armor cannot be worn over an outfit (CRB p. 130).",
        "Raider Chest Piece and Metal Chest Piece both cover the Torso; the higher values count.",
        "Choose where Leather Arm is worn.",
        "Standard Plating is made for a Mister Handy."), hints(survivor));
  }

  @Test
  void misterHandyWearsOnlyRobotItemsAndHats() {
    Character handy = character("mister-handy");
    wear(handy, "casual-hat", null);
    wear(handy, "leather-chest-piece", null);
    assertEquals(List.of("A Mister Handy cannot wear Leather Chest Piece (CRB p. 146)."), hints(handy));
  }

  /** A limb that does not fit the piece (e.g. from an edited file) counts as not chosen. */
  @Test
  void limbMustFitThePiece() {
    Character survivor = character("survivor");
    wear(survivor, "raider-arm", HitLocation.HEAD);
    assertEquals(List.of("Choose where Raider Arm is worn."), hints(survivor));
  }

  @Test
  void itemsOutsideTheCatalogAreReported() {
    Character survivor = character("survivor");
    survivor.addApparel(WornApparel.of(CatalogRef.fromLegacyName("Spacesuit")));
    assertEquals(List.of("\"Spacesuit\" is not an item of the supported books."), hints(survivor));
  }
}
