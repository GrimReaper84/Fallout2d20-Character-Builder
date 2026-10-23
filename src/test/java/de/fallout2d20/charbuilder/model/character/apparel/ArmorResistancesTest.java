package de.fallout2d20.charbuilder.model.character.apparel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModSlot;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.derived.CarryLoad;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import de.fallout2d20.charbuilder.model.character.derived.Resistance;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.Materials;
import java.util.List;
import java.util.Map;
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
    character.gear().addApparel(WornApparel.of(CatalogRef.of(id)).withLocation(location));
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
    assertEquals(List.of(false, false, false, true),
        survivor.gear().getApparel().stream().map(WornApparel::carried).toList(),
        "clothing and armor are different layers; the second chest piece clashes");
    assertEquals(Resistance.of(1), at(survivor, HitLocation.TORSO).physical(), "Raider Chest Piece");
    int carried = CarryLoad.of(survivor).carried();
    survivor.gear().setApparelCarried(2, true);
    survivor.gear().setApparelCarried(3, false);
    assertEquals(Resistance.of(2), at(survivor, HitLocation.TORSO).physical(), "Metal Chest Piece");
    assertEquals(carried, CarryLoad.of(survivor).carried(), "worn or carried, the weight counts");
  }

  /** CRB p. 146 (robot armor changes the carry weight) + user decision 2026-10-16: only while worn. */
  @Test
  void carriedRobotArmorKeepsTheCarryWeight() {
    PlayerCharacter protectron = character("protectron");
    wear(protectron, "actuated-frame-main-body", null);
    assertEquals(245, DerivedStats.of(protectron).carryWeight());
    protectron.gear().setApparelCarried(0, true);
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
    survivor.gear().addApparel(WornApparel.of(new CustomApparel("Legacy armor (Head)", ApparelKind.ARMOR,
        Set.of(HitLocation.HEAD), 3, 2, 1, false, 0)));
    assertEquals(Resistance.of(3), at(survivor, HitLocation.HEAD).physical());
    survivor.gear().setApparelNotes(0, "Ballistic weave");
    assertEquals("Ballistic weave", survivor.gear().getApparel().getFirst().notes());
    survivor.gear().removeApparel(0);
    assertTrue(survivor.gear().getApparel().isEmpty());
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
    protectron.gear().setApparelLocation(1, HitLocation.RIGHT_ARM);
    assertEquals(255, DerivedStats.of(protectron).carryWeight());
  }

  /**
   * CRB p. 130 with mods: Casual Clothing with Ballistic Weave Mk V (6/6, CRB p. 126) under a Metal Chest Piece
   * (2/1) – the higher modded value counts per type.
   */
  @Test
  void moddedClothingUnderArmor() {
    PlayerCharacter survivor = character("survivor");
    wear(survivor, "casual-clothing", null);
    wear(survivor, "metal-chest-piece", null);
    survivor.gear().setApparelMod(0, ApparelModSlot.BALLISTIC_WEAVE,
        ApparelCatalog.modById("ballistic-weave-mk-v").orElseThrow());
    survivor.gear().setApparelMod(1, ApparelModSlot.MATERIAL, ApparelCatalog.modById("metal-polished-metal").orElseThrow());
    LocationResistance torso = at(survivor, HitLocation.TORSO);
    assertEquals(Resistance.of(6), torso.physical(), "Metal Chest Piece 2 + 4 = 6, weave 6");
    assertEquals(Resistance.of(6), torso.energy(), "weave 6 beats 1 + 4");
    assertEquals(Resistance.of(6), at(survivor, HitLocation.LEFT_LEG).physical(), "only the clothing there");
  }

  private static PlayerCharacter withPerk(String perk, int rank, int agilityAllocation) {
    return PlayerCharacter.builder().origin(CatalogRef.of("survivor"))
        .attributeAllocation(Map.of(Attribute.AGILITY, agilityAllocation))
        .perks(List.of(new PerkSelection(CatalogRef.of(perk), rank, 0, List.of(), List.of()))).build();
  }

  /** WG p. 51 Evasive: +1 at AGI 7–8, +2 at 9–10, nothing while over-encumbered. */
  @Test
  void evasiveByAgility() {
    assertEquals(Resistance.of(0), at(withPerk("evasive", 1, 1), HitLocation.HEAD).physical(), "AGI 6");
    PlayerCharacter agile = withPerk("evasive", 1, 3);
    assertEquals(Resistance.of(1), at(agile, HitLocation.HEAD).physical(), "AGI 8");
    assertEquals(Resistance.of(1), at(agile, HitLocation.LEFT_LEG).energy());
    PlayerCharacter nimble = withPerk("evasive", 1, 5);
    assertEquals(Resistance.of(2), at(nimble, HitLocation.TORSO).physical(), "AGI 10");
    assertEquals(Resistance.of(3), at(withPerk("evasive", 1, 6), HitLocation.TORSO).physical(), "AGI 11");
    nimble.gear().setMaterials(new Materials(200, 0, 0, 0));
    assertEquals(Resistance.of(0), at(nimble, HitLocation.TORSO).physical(), "400 lbs. of junk: over-encumbered");
  }

  /** WG p. 48 Ironclad: +rank while armor is worn; clothing does not count. */
  @Test
  void ironcladWhileWearingArmor() {
    PlayerCharacter character = withPerk("ironclad", 2, 0);
    wear(character, "tough-clothing", null);
    assertEquals(Resistance.of(0), at(character, HitLocation.HEAD).physical(), "clothing is no armor");
    wear(character, "leather-arm", HitLocation.LEFT_ARM);
    assertEquals(Resistance.of(2), at(character, HitLocation.HEAD).physical());
    assertEquals(Resistance.of(2), at(character, HitLocation.HEAD).energy());
  }

  /** User decision 2026-10-22: Cage Armor and Spike Armor count as armor for Ironclad although they are outfits. */
  @Test
  void ironcladCountsArmorOutfits() {
    PlayerCharacter character = withPerk("ironclad", 1, 0);
    wear(character, "spike-armor", null);
    int spike = ApparelCatalog.byId("spike-armor").orElseThrow().physical();
    assertEquals(Resistance.of(spike + 1), at(character, HitLocation.TORSO).physical());
  }

  /** The perk DR adds up with the apparel and the CRB perks: Leather Arm + Ironclad 1 + Junk Shield 1 + Toughness 1. */
  @Test
  void perkResistancesAddUp() {
    PlayerCharacter character = PlayerCharacter.builder().origin(CatalogRef.of("survivor"))
        .attributeAllocation(Map.of(Attribute.ENDURANCE, 1, Attribute.LUCK, 2))
        .perks(List.of(new PerkSelection(CatalogRef.of("ironclad"), 1, 0, List.of(), List.of()),
            new PerkSelection(CatalogRef.of("junk-shield"), 1, 0, List.of(), List.of()),
            new PerkSelection(CatalogRef.of("toughness"), 1, 0, List.of(), List.of()))).build();
    character.gear().setMaterials(new Materials(5, 0, 0, 0));
    wear(character, "leather-arm", HitLocation.LEFT_ARM);
    int leather = ApparelCatalog.byId("leather-arm").orElseThrow().physical();
    assertEquals(Resistance.of(leather + 3), at(character, HitLocation.LEFT_ARM).physical());
    assertEquals(Resistance.of(3), at(character, HitLocation.HEAD).physical());
  }

  /** WG p. 52 Junk Shield: +1 per 5 junk, at most the rank. */
  @Test
  void junkShield() {
    PlayerCharacter character = withPerk("junk-shield", 2, 0);
    character.gear().setMaterials(new Materials(7, 0, 0, 0));
    assertEquals(Resistance.of(1), at(character, HitLocation.TORSO).physical());
    character.gear().setMaterials(new Materials(12, 0, 0, 0));
    assertEquals(Resistance.of(2), at(character, HitLocation.TORSO).energy());
    character.gear().setMaterials(new Materials(30, 0, 0, 0));
    assertEquals(Resistance.of(2), at(character, HitLocation.TORSO).physical(), "capped at rank 2");
  }

  /** WG p. 52 Junk Shield counts the junk items of WG pp. 120–125 with the junk counter (user decision). */
  @Test
  void junkShieldCountsJunkItems() {
    PlayerCharacter character = withPerk("junk-shield", 2, 0);
    character.gear().setMaterials(new Materials(3, 0, 0, 0));
    character.gear().addItem(CarriedItem.of(CatalogRef.of("duct-tape")));
    character.gear().setItemQuantity(0, 2);
    assertEquals(5, character.gear().junkCount());
    assertEquals(Resistance.of(1), at(character, HitLocation.TORSO).physical());
  }
}
