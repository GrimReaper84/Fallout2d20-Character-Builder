package de.fallout2d20.charbuilder.model.character.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.derived.CarryLoad;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Equipment pack text into inventory entries (user decision 2026-10-16). */
class PackInventoryTest {

  /**
   * CRB p. 79 Settler with pipe wrench, a trinket and the Lockpick Tag items (CRB p. 81): amounts to roll become
   * roll lines, not custom items (user decision 2026-10-19); entries are marked with their source.
   */
  @Test
  void settler() {
    PlayerCharacter settler = character("survivor", "settler");
    settler.startingGear().chooseOption(0, "Pipe wrench");
    settler.startingGear().setTrinket("Medal");
    settler.abilities().setFreeTag(Skill.LOCKPICK, true);

    PackItems pack = PackInventory.resolve(settler, StartingSource.PACK);
    assertEquals(List.of("pipe-gun", "pipe-wrench"), weaponIds(pack));
    assertEquals(List.of("tough-clothing"), apparelIds(pack));
    assertEquals(List.of("Medal"), customNames(pack));
    assertEquals(List.of("6 + 3 CD rounds of .38 ammunition", "2 rolls on the random food table (CRB p. 202)"),
        pack.rolls());
    assertTrue(pack.weapons().stream().allMatch(weapon -> weapon.startingSource() == StartingSource.PACK));
    assertTrue(pack.items().stream().allMatch(item -> item.startingSource() == StartingSource.PACK));

    PackItems tagSkills = PackInventory.resolve(settler, StartingSource.TAG_SKILL);
    assertEquals(List.of("4 + 2 CD bobby pins"), tagSkills.rolls());
    assertTrue(tagSkills.items().isEmpty());
  }

  /** CRB p. 81: the d20 caps of Barter are an amount to roll. */
  @Test
  void barterCapsAreARoll() {
    PlayerCharacter settler = character("survivor", "settler");
    settler.abilities().setFreeTag(Skill.BARTER, true);
    assertEquals(List.of("2d20 caps"), PackInventory.resolve(settler, StartingSource.TAG_SKILL).rolls());
  }

  @Test
  void recognizesRolls() {
    assertTrue(PackInventory.isRoll("10 + 5 CD rounds of 10mm ammunition"));
    assertTrue(PackInventory.isRoll("Wares: 3 rolls each on the ammunition, aid and junk loot tables"));
    assertTrue(PackInventory.isRoll("1 roll on the Oddities and Valuables loot table"));
    assertTrue(PackInventory.isRoll("2d20 caps"));
    assertFalse(PackInventory.isRoll("Rolling pin"));
    assertFalse(PackInventory.isRoll("Vault-Tec canteen with 1 purified water"));
  }

  /** CRB p. 78: "2 stimpaks" becomes Stimpak × 2; "with" splits weapon and ammunition text. */
  @Test
  void countsAndWith() {
    PlayerCharacter resident = character("vault-dweller", "vault-tec-resident");
    PackItems added = PackInventory.resolve(resident, StartingSource.PACK);
    CarriedItem stimpaks = added.items().stream().filter(item -> item.displayName().equals("Stimpak")).findFirst()
        .orElseThrow();
    assertEquals(2, stimpaks.quantity());
    assertTrue(weaponIds(added).containsAll(List.of("switchblade", "10mm-pistol")));
    assertTrue(customNames(added).contains("Vault-Tec canteen with 1 purified water"));
  }

  /** WOA p. 22: built-in weapons; "Two laser guns" counts 2; a pack wording differing from the catalog is kept as note. */
  @Test
  void protectronAndAliases() {
    PlayerCharacter protectron = character("protectron", "protectron");
    protectron.startingGear().apply(protectron.startingGear().getSelection());
    PackItems added = PackInventory.resolve(protectron, StartingSource.PACK);
    assertEquals(List.of("protectron-claws", "laser-gun"), weaponIds(added));
    assertEquals(2, added.weapons().get(1).quantity());
    assertEquals("Pack: Two laser guns built into the claws", added.weapons().get(1).notes());
    assertTrue(added.weapons().get(1).builtIn());
    assertEquals(0, CarryLoad.of(protectron).carried(), "built-in laser guns (2 × 4 lbs.) weigh nothing");
    assertEquals(List.of("standard-plating"), apparelIds(added));
    assertTrue(added.items().stream().anyMatch(item -> item.displayName().equals("Recon Sensors")));
  }

  /** WOA p. 23: a ruling in parentheses stays as note. */
  @Test
  void keepsRulingsInParentheses() {
    PlayerCharacter utility = character("protectron", "utility-protectron");
    utility.startingGear().chooseOption(0, "Railway rifle with 6 + 3 CD railway spikes (Two-Handed ignored)");
    PackItems added = PackInventory.resolve(utility, StartingSource.PACK);
    assertEquals("Pack: Railway rifle with 6 + 3 CD railway spikes (Two-Handed ignored)", added.weapons().stream()
        .filter(weapon -> weapon.displayName().equals("Railway Rifle")).findFirst().orElseThrow().notes());
  }

  /** Lines naming several things are split when every part is known (CRB p. 81 Science: lab coat and Mentats). */
  @Test
  void splitsKnownParts() {
    assertTrue(PackInventory.match("Lab coat").isPresent());
    PlayerCharacter mercenary = character("survivor", "mercenary");
    mercenary.startingGear().chooseOption(0, "Leather armor arm and leather armor leg (choose sides)");
    PackItems added = PackInventory.resolve(mercenary, StartingSource.PACK);
    assertEquals(List.of("tough-clothing", "leather-arm", "leather-leg"), apparelIds(added));
  }

  private static PlayerCharacter character(String origin, String pack) {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of(origin));
    character.startingGear().choosePack(CatalogRef.of(pack));
    return character;
  }

  private static List<String> customNames(PackItems added) {
    return added.items().stream().filter(item -> item.custom() != null).map(CarriedItem::displayName).toList();
  }

  private static List<String> weaponIds(PackItems added) {
    return added.weapons().stream().map(CarriedWeapon::weapon).map(ref -> ref == null ? "custom" : ref.id()).toList();
  }

  private static List<String> apparelIds(PackItems added) {
    return added.apparel().stream().map(WornApparel::apparel).map(ref -> ref == null ? "custom" : ref.id()).toList();
  }
}
