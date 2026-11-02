package de.fallout2d20.charbuilder.model.character.apparel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.HouseRule;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import java.util.List;
import java.util.Set;
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
    character.gear().addApparel(WornApparel.of(CatalogRef.of(id)).withLocation(location));
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
    assertTrue(survivor.gear().getApparel().get(2).carried(), "a clashing piece starts carried");
    wear(survivor, "leather-arm", null);
    wear(survivor, "standard-plating", null);
    for (int i = 0; i < survivor.gear().getApparel().size(); i++) {
      survivor.gear().setApparelCarried(i, false);
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
    assertTrue(survivor.gear().getApparel().get(1).carried());
    PlayerCharacter soldier = character("survivor");
    wear(soldier, "combat-leg", HitLocation.LEFT_LEG);
    wear(soldier, "drifter-outfit", null);
    assertTrue(soldier.gear().getApparel().get(1).carried());
    assertEquals(List.of(), hints(soldier));
  }

  /** Carried pieces take part in no hint except the catalog check (user decision 2026-10-16). */
  @Test
  void carriedPiecesGiveNoHints() {
    PlayerCharacter survivor = character("survivor");
    wear(survivor, "drifter-outfit", null);
    wear(survivor, "leather-arm", null);
    wear(survivor, "standard-plating", null);
    survivor.gear().setApparelCarried(1, true);
    survivor.gear().setApparelCarried(2, true);
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
    survivor.gear().addApparel(WornApparel.of(CatalogRef.fromLegacyName("Spacesuit")));
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

  /** CRB p. 122, WG p. 14: Super Mutants and Nightkin only in armor made for super mutants; clothing is fine. */
  @Test
  void mutantsOnlyInRaiderArmor() {
    for (String origin : List.of("super-mutant", "nightkin")) {
      PlayerCharacter mutant = character(origin);
      wear(mutant, "raider-arm", HitLocation.LEFT_ARM);
      wear(mutant, "tough-clothing", null);
      assertTrue(hints(mutant).stream().noneMatch(hint -> hint.contains("super mutant")), origin);
      wear(mutant, "metal-arm", HitLocation.RIGHT_ARM);
      mutant.gear().addApparel(WornApparel.of(new CustomApparel("Scrap plate", ApparelKind.ARMOR,
          Set.of(HitLocation.TORSO), 1, 1, 0, false, 0)));
      assertEquals(1, hints(mutant).stream().filter(hint -> hint.contains("super mutant")).count(), origin);
    }
    PlayerCharacter survivor = character("survivor");
    wear(survivor, "metal-arm", HitLocation.RIGHT_ARM);
    assertTrue(hints(survivor).stream().noneMatch(hint -> hint.contains("super mutant")));
  }

  private static PlayerCharacter withHelmetRule() {
    PlayerCharacter survivor = character("survivor");
    survivor.setHouseRule(HouseRule.ARMOR_HELMETS_AS_HEADGEAR, true);
    return survivor;
  }

  /** Wears every piece, also those that started carried. */
  private static void wearAll(PlayerCharacter character) {
    for (int i = 0; i < character.gear().getApparel().size(); i++) {
      character.gear().setApparelCarried(i, false);
    }
  }

  /** CRB p. 130: as written, an armor helmet is armor and cannot be worn with an outfit. */
  @Test
  void helmetWithOutfitAsWritten() {
    PlayerCharacter survivor = character("survivor");
    wear(survivor, "drifter-outfit", null);
    wear(survivor, "combat-helmet", null);
    assertTrue(survivor.gear().getApparel().get(1).carried());
    wearAll(survivor);
    assertEquals(List.of("Armor cannot be worn over an outfit (CRB p. 130)."), hints(survivor));
  }

  /** House rule: the helmet is worn like headgear, so the outfit allows it (user decision 2026-10-22). */
  @Test
  void helmetWithOutfitHouseRule() {
    PlayerCharacter survivor = withHelmetRule();
    wear(survivor, "drifter-outfit", null);
    wear(survivor, "combat-helmet", null);
    assertFalse(survivor.gear().getApparel().get(1).carried());
    assertEquals(List.of(), hints(survivor));
    PlayerCharacter dressedLater = withHelmetRule();
    wear(dressedLater, "combat-helmet", null);
    wear(dressedLater, "drifter-outfit", null);
    assertFalse(dressedLater.gear().getApparel().get(1).carried(), "an outfit added to a worn helmet");
  }

  /** CRB p. 122: one headgear item; under the house rule the helmet is it (user decision 2026-11-02). */
  @Test
  void helmetAndHatHouseRule() {
    PlayerCharacter asWritten = character("survivor");
    wear(asWritten, "casual-hat", null);
    wear(asWritten, "combat-helmet", null);
    assertFalse(asWritten.gear().getApparel().get(1).carried());
    assertEquals(List.of(), hints(asWritten));
    PlayerCharacter survivor = withHelmetRule();
    wear(survivor, "casual-hat", null);
    wear(survivor, "combat-helmet", null);
    assertTrue(survivor.gear().getApparel().get(1).carried());
    wearAll(survivor);
    assertEquals(List.of("Casual Hat and Combat Helmet both cover the Head; the higher values count."),
        hints(survivor));
  }

  /** Full body armor that includes the head is no helmet, so it still clashes with an outfit. */
  @Test
  void fullBodyArmorIsNoHelmet() {
    PlayerCharacter survivor = withHelmetRule();
    wear(survivor, "drifter-outfit", null);
    wear(survivor, "recon-armor", null);
    assertTrue(survivor.gear().getApparel().get(1).carried());
  }

  /** CRB p. 122: an outfit that covers the head is its headgear item, with or without the house rule. */
  @Test
  void headgearOnHazmatSuit() {
    for (PlayerCharacter survivor : List.of(character("survivor"), withHelmetRule())) {
      wear(survivor, "hazmat-suit", null);
      wear(survivor, "casual-hat", null);
      assertTrue(survivor.gear().getApparel().get(1).carried());
      wearAll(survivor);
      assertEquals(List.of("Hazmat Suit and Casual Hat both cover the Head; the higher values count."),
          hints(survivor));
    }
    PlayerCharacter helmeted = withHelmetRule();
    wear(helmeted, "hazmat-suit", null);
    wear(helmeted, "combat-helmet", null);
    assertTrue(helmeted.gear().getApparel().get(1).carried(), "the helmet is headgear too");
  }

  /** An outfit without the head leaves it to a hat. */
  @Test
  void hatWithDrifterOutfit() {
    PlayerCharacter survivor = character("survivor");
    wear(survivor, "drifter-outfit", null);
    wear(survivor, "casual-hat", null);
    assertFalse(survivor.gear().getApparel().get(1).carried());
    assertEquals(List.of(), hints(survivor));
  }
}
