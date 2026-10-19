package de.fallout2d20.charbuilder.model.character.apparel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/** Soft apparel hints (CRB p. 130, 146; user decision: never a block). */
class ApparelRulesTest {

  private static PlayerCharacter character(String origin) {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of(origin));
    return character;
  }

  private static void wear(PlayerCharacter character, String id, @Nullable HitLocation location) {
    character.addApparel(WornApparel.of(CatalogRef.of(id)).withLocation(location));
  }

  private static List<String> hints(PlayerCharacter character) {
    return ApparelRules.problems(character).stream().map(CreationProblem::message).toList();
  }

  @Test
  void conflictsOfAHumanoid() {
    PlayerCharacter survivor = character("survivor");
    wear(survivor, "hazmat-suit", null);
    wear(survivor, "raider-chest-piece", null);
    wear(survivor, "metal-chest-piece", null);
    assertTrue(survivor.getApparel().get(2).carried(), "a clashing piece starts carried");
    wear(survivor, "leather-arm", null);
    wear(survivor, "standard-plating", null);
    for (int i = 0; i < survivor.getApparel().size(); i++) {
      survivor.setApparelCarried(i, false);
    }
    assertEquals(List.of("Armor cannot be worn over an outfit (CRB p. 130).",
        "Raider Chest Piece and Metal Chest Piece both cover the Torso; the higher values count.",
        "Choose where Leather Arm is worn.",
        "Standard Plating is made for robots."), hints(survivor));
  }

  @Test
  void misterHandyWearsOnlyRobotItemsAndHats() {
    PlayerCharacter handy = character("mister-handy");
    wear(handy, "casual-hat", null);
    wear(handy, "leather-chest-piece", null);
    assertEquals(List.of("A Mister Handy cannot wear Leather Chest Piece (CRB p. 146)."), hints(handy));
  }

  /** CRB p. 130: armor added over a worn outfit starts carried, and an outfit added over worn armor too. */
  @Test
  void armorAndOutfitClash() {
    PlayerCharacter survivor = character("survivor");
    wear(survivor, "drifter-outfit", null);
    wear(survivor, "metal-helmet", null);
    assertTrue(survivor.getApparel().get(1).carried());
    PlayerCharacter soldier = character("survivor");
    wear(soldier, "combat-leg", HitLocation.LEFT_LEG);
    wear(soldier, "drifter-outfit", null);
    assertTrue(soldier.getApparel().get(1).carried());
    assertEquals(List.of(), hints(soldier));
  }

  /** Carried pieces take part in no hint except the catalog check (user decision 2026-10-16). */
  @Test
  void carriedPiecesGiveNoHints() {
    PlayerCharacter survivor = character("survivor");
    wear(survivor, "drifter-outfit", null);
    wear(survivor, "leather-arm", null);
    wear(survivor, "standard-plating", null);
    survivor.setApparelCarried(1, true);
    survivor.setApparelCarried(2, true);
    assertEquals(List.of(), hints(survivor));
  }

  /** A limb that does not fit the piece (e.g. from an edited file) counts as not chosen. */
  @Test
  void limbMustFitThePiece() {
    PlayerCharacter survivor = character("survivor");
    wear(survivor, "raider-arm", HitLocation.HEAD);
    assertEquals(List.of("Choose where Raider Arm is worn."), hints(survivor));
  }

  @Test
  void itemsOutsideTheCatalogAreReported() {
    PlayerCharacter survivor = character("survivor");
    survivor.addApparel(WornApparel.of(CatalogRef.fromLegacyName("Spacesuit")));
    assertEquals(List.of("\"Spacesuit\" is not an item of the supported books."), hints(survivor));
  }

  /** House rule (user decision 2026-10-16): human apparel on a Protectron is offered, with a hint; robot armor fits. */
  @Test
  void protectronGetsAHintForHumanApparel() {
    PlayerCharacter protectron = character("protectron");
    wear(protectron, "standard-plating", null);
    wear(protectron, "factory-armor-arms", HitLocation.RIGHT_ARM);
    assertEquals(List.of(), hints(protectron));
    wear(protectron, "leather-chest-piece", null);
    assertEquals(List.of("Robots like the Protectron wear robot plating and armor; Leather Chest Piece only with GM "
        + "permission."), hints(protectron));
    wear(protectron, "factory-armor-arms", HitLocation.ARM_1);
    assertTrue(hints(protectron).contains("Choose where Factory Armor is worn."), "a Mister Handy arm does not fit");
    assertTrue(ApparelRules.isOffered(protectron, ApparelKind.ARMOR, false));
    assertTrue(ApparelRules.isOffered(protectron, ApparelKind.ROBOT_ARMOR, false));
    PlayerCharacter handy = character("mister-handy");
    assertFalse(ApparelRules.isOffered(handy, ApparelKind.ARMOR, false));
    assertFalse(ApparelRules.isOffered(character("survivor"), ApparelKind.ROBOT_PLATING, false));
  }

  /** CRB p. 146: the hat exception belongs to the Mister Handy; on a Protectron a hat is human apparel too. */
  @Test
  void hatExceptionOnlyForAMisterHandy() {
    PlayerCharacter protectron = character("protectron");
    wear(protectron, "casual-hat", null);
    assertEquals(List.of("Robots like the Protectron wear robot plating and armor; Casual Hat only with GM "
        + "permission."), hints(protectron));
  }
}
