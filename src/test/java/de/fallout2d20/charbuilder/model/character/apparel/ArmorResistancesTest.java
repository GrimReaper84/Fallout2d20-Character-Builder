package de.fallout2d20.charbuilder.model.character.apparel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.derived.CarryLoad;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import de.fallout2d20.charbuilder.model.character.derived.Resistance;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Damage resistance per hit location (CRB p. 124, 130, 146). */
class ArmorResistancesTest {

  private static PlayerCharacter character(String origin) {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of(origin));
    return character;
  }

  private static LocationResistance at(PlayerCharacter character, HitLocation location) {
    return ArmorResistances.of(character).stream().filter(entry -> entry.location() == location).findFirst()
        .orElseThrow();
  }

  private static void wear(PlayerCharacter character, String id, HitLocation location) {
    character.addApparel(WornApparel.of(CatalogRef.of(id)).withLocation(location));
  }

  /** CRB p. 130: over clothing the higher value of each type counts (Vault Jumpsuit 0/1/2, Metal Chest Piece 2/1/0). */
  @Test
  void armorOverClothingUsesTheHigherValuePerType() {
    PlayerCharacter survivor = character("survivor");
    wear(survivor, "vault-jumpsuit", null);
    wear(survivor, "metal-chest-piece", null);
    LocationResistance torso = at(survivor, HitLocation.TORSO);
    assertEquals(Resistance.of(2), torso.physical());
    assertEquals(Resistance.of(1), torso.energy());
    assertEquals(Resistance.of(2), torso.radiation());
    assertEquals(Resistance.of(0), at(survivor, HitLocation.HEAD).physical(), "the jumpsuit does not cover the head");
  }

  /** User decision 2026-10-16: new pieces start worn unless they clash; carried pieces give no DR but weigh. */
  @Test
  void carriedPiecesDoNotProtect() {
    PlayerCharacter survivor = character("survivor");
    wear(survivor, "tough-clothing", null);
    wear(survivor, "metal-helmet", null);
    wear(survivor, "raider-chest-piece", null);
    wear(survivor, "metal-chest-piece", null);
    assertEquals(List.of(false, false, false, true), survivor.getApparel().stream().map(WornApparel::carried).toList(),
        "clothing and armor are different layers; the second chest piece clashes");
    assertEquals(Resistance.of(1), at(survivor, HitLocation.TORSO).physical(), "Raider Chest Piece");
    int carried = CarryLoad.of(survivor).carried();
    survivor.setApparelCarried(2, true);
    survivor.setApparelCarried(3, false);
    assertEquals(Resistance.of(2), at(survivor, HitLocation.TORSO).physical(), "Metal Chest Piece");
    assertEquals(carried, CarryLoad.of(survivor).carried(), "worn or carried, the weight counts");
  }

  /** CRB p. 146 (robot armor changes the carry weight) + user decision 2026-10-16: only while worn. */
  @Test
  void carriedRobotArmorKeepsTheCarryWeight() {
    PlayerCharacter protectron = character("protectron");
    wear(protectron, "actuated-frame-main-body", null);
    assertEquals(245, DerivedStats.of(protectron).carryWeight());
    protectron.setApparelCarried(0, true);
    assertEquals(225, DerivedStats.of(protectron).carryWeight());
  }

  /** A single-limb piece covers only the chosen side. */
  @Test
  void legPieceCoversOnlyItsSide() {
    PlayerCharacter survivor = character("survivor");
    wear(survivor, "combat-leg", HitLocation.LEFT_LEG);
    assertEquals(Resistance.of(2), at(survivor, HitLocation.LEFT_LEG).physical());
    assertEquals(Resistance.of(0), at(survivor, HitLocation.RIGHT_LEG).physical());
  }

  /** Hazmat Suit: radiation immune where it covers (CRB p. 124); Toughness adds +1 physical everywhere (CRB p. 73). */
  @Test
  void hazmatImmunityAndInnateResistanceOnEveryLocation() {
    PlayerCharacter survivor = character("survivor");
    survivor.addPerkRank(CatalogRef.of("toughness"), null, null);
    wear(survivor, "hazmat-suit", null);
    for (LocationResistance location : ArmorResistances.of(survivor)) {
      assertEquals(Resistance.IMMUNITY, location.radiation(), location.location().name());
      assertEquals(Resistance.of(1), location.physical(), location.location().name());
    }
  }

  /** CRB p. 53: a Super Mutant stays immune to radiation and poison, whatever it wears. */
  @Test
  void originImmunityStays() {
    PlayerCharacter mutant = character("super-mutant");
    wear(mutant, "raider-chest-piece", null);
    LocationResistance torso = at(mutant, HitLocation.TORSO);
    assertEquals(Resistance.IMMUNITY, torso.radiation());
    assertEquals(Resistance.IMMUNITY, torso.poison());
    assertEquals(Resistance.of(1), torso.physical());
  }

  /** CRB p. 146: Standard Plating 2/0 on all locations, Factory Armor adds +1/+1 to the optics. */
  @Test
  void misterHandyPlatingPlusRobotArmor() {
    PlayerCharacter handy = character("mister-handy");
    assertEquals(HitLocation.misterHandy(), handy.hitLocations());
    wear(handy, "standard-plating", null);
    wear(handy, "factory-armor-optics", null);
    assertEquals(Resistance.of(3), at(handy, HitLocation.OPTICS).physical());
    assertEquals(Resistance.of(1), at(handy, HitLocation.OPTICS).energy());
    assertEquals(Resistance.of(2), at(handy, HitLocation.THRUSTER).physical());
    assertEquals(Resistance.of(0), at(handy, HitLocation.THRUSTER).energy());
  }

  @Test
  void customItemsCoverTheirLocations() {
    PlayerCharacter survivor = character("survivor");
    survivor.addApparel(WornApparel.of(new CustomApparel("Legacy armor (Head)", ApparelKind.ARMOR,
        Set.of(HitLocation.HEAD), 3, 2, 1, false, 0)));
    assertEquals(Resistance.of(3), at(survivor, HitLocation.HEAD).physical());
    survivor.setApparelNotes(0, "Ballistic weave");
    assertEquals("Ballistic weave", survivor.getApparel().getFirst().notes());
    survivor.removeApparel(0);
    assertTrue(survivor.getApparel().isEmpty());
  }

  /** House rule (user decision 2026-10-16): a Protectron has humanoid locations and wears robot armor mapped onto them. */
  @Test
  void protectronWearsRobotArmorOnHumanoidLocations() {
    PlayerCharacter protectron = character("protectron");
    assertEquals(HitLocation.humanoid(), protectron.hitLocations());
    wear(protectron, "standard-plating", null);
    for (LocationResistance location : ArmorResistances.of(protectron)) {
      assertEquals(Resistance.of(2), location.physical(), location.location().name());
    }
    wear(protectron, "factory-armor-main-body", null);
    wear(protectron, "factory-armor-arms", HitLocation.LEFT_ARM);
    wear(protectron, "factory-armor-optics", null);
    wear(protectron, "factory-armor-thruster", null);
    assertEquals(Resistance.of(3), at(protectron, HitLocation.TORSO).physical());
    assertEquals(Resistance.of(3), at(protectron, HitLocation.HEAD).physical());
    assertEquals(Resistance.of(3), at(protectron, HitLocation.LEFT_ARM).physical());
    assertEquals(Resistance.of(2), at(protectron, HitLocation.RIGHT_ARM).physical(), "one arm per piece");
    assertEquals(Resistance.of(3), at(protectron, HitLocation.LEFT_LEG).physical());
    assertEquals(Resistance.of(3), at(protectron, HitLocation.RIGHT_LEG).physical());
    assertEquals(Resistance.IMMUNITY, at(protectron, HitLocation.TORSO).radiation(), "WOA p. 21");
  }

  /** WOA p. 21: 225 lbs., raised only by modified (robot) armor; the frame counts once its arm is chosen. */
  @Test
  void protectronCarryWeightFollowsRobotArmor() {
    PlayerCharacter protectron = character("protectron");
    assertEquals(225, DerivedStats.of(protectron).carryWeight());
    wear(protectron, "actuated-frame-main-body", null);
    wear(protectron, "actuated-frame-arms", null);
    assertEquals(245, DerivedStats.of(protectron).carryWeight());
    protectron.setApparelLocation(1, HitLocation.RIGHT_ARM);
    assertEquals(255, DerivedStats.of(protectron).carryWeight());
  }
}
