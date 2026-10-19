package de.fallout2d20.charbuilder.model.character.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The starting equipment entered into the inventory and kept in sync during creation (user decision 2026-10-19). */
class StartingEquipmentSyncTest {

  /** CRB p. 79 Settler: entries marked, roll lines in the gear notes, caps 45 (pack) at level 1. */
  @Test
  void entersThePack() {
    Character settler = settler();
    settler.setInventoryNotes("Bought a map");
    apply(settler);

    assertEquals(List.of("Pipe Gun", "Pipe Wrench"), settler.getWeapons().stream().map(CarriedWeapon::displayName)
        .toList());
    assertTrue(settler.getWeapons().stream().allMatch(weapon -> weapon.startingSource() == StartingSource.PACK));
    assertEquals(1, settler.getApparel().size());
    assertEquals("""
        Bought a map
        [Pack] Roll: 6 + 3 CD rounds of .38 ammunition
        [Pack] Roll: 2 rolls on the random food table (CRB p. 202)""", settler.getInventoryNotes());
    assertEquals(45, settler.getCaps());
  }

  /**
   * Another pick replaces only the marked entries and the untouched roll lines; hand-added entries and a line with
   * the result written behind it stay.
   */
  @Test
  void anotherPickReplacesOnlyMarkedEntries() {
    Character settler = settler();
    apply(settler);
    settler.addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    settler.addItem(CarriedItem.of(CatalogRef.of("stimpak")));
    settler.setInventoryNotes(settler.getInventoryNotes().replace("rounds of .38 ammunition",
        "rounds of .38 ammunition = 11"));

    StartingEquipment snapshot = settler.getStartingEquipment();
    settler.chooseEquipmentPack(CatalogRef.of("trader"));
    settler.chooseEquipmentOption(0, "Leather armor chest piece");
    settler.applyStartingEquipment(snapshot);

    List<String> weapons = settler.getWeapons().stream().map(CarriedWeapon::displayName).toList();
    assertEquals(List.of("10mm Pistol", "Pipe Gun"), weapons.stream().sorted().toList());
    assertTrue(settler.getItems().stream().anyMatch(item -> item.displayName().equals("Stimpak")
        && item.startingSource() == null));
    assertTrue(settler.getInventoryNotes().contains("[Pack] Roll: 6 + 3 CD rounds of .38 ammunition = 11"),
        "line with a result stays");
    assertFalse(settler.getInventoryNotes().contains("random food table"), "untouched old line replaced");
    assertTrue(settler.getInventoryNotes().contains("[Pack] Roll: 8 + 4 CD rounds of .38 ammunition"));
    assertEquals(50, settler.getCaps());
  }

  /** CRB p. 81: a Tag change re-syncs only the Tag skill items; edits of pack entries survive. */
  @Test
  void tagChangeSyncsOnlyTagSkillItems() {
    Character settler = settler();
    apply(settler);
    settler.setWeaponNotes(0, "scratched");
    settler.setCaps(30);

    settler.setFreeTag(Skill.LOCKPICK, true);
    assertEquals("scratched", settler.getWeapons().getFirst().notes());
    assertTrue(settler.getInventoryNotes().endsWith("[Tag skill] Roll: 4 + 2 CD bobby pins"));
    assertEquals(30, settler.getCaps(), "a Tag change keeps typed caps");

    settler.setFreeTag(Skill.LOCKPICK, false);
    assertFalse(settler.getInventoryNotes().contains("[Tag skill]"));
    assertEquals("scratched", settler.getWeapons().getFirst().notes());
  }

  /** CRB p. 81: Barter grants no item, only 2d20 caps – tagging and untagging it still re-syncs. */
  @Test
  void barterTagSyncsItsCaps() {
    Character settler = settler();
    apply(settler);
    settler.setFreeTag(Skill.BARTER, true);
    assertTrue(settler.getInventoryNotes().endsWith("[Tag skill] Roll: 2d20 caps"));
    settler.setFreeTag(Skill.BARTER, false);
    assertFalse(settler.getInventoryNotes().contains("2d20 caps"));
  }

  /** Taking and giving back Tag! re-syncs the Tag skill items too. */
  @Test
  void perkAndTraitChangesSync() {
    Character settler = settler();
    apply(settler);
    settler.addPerkRank(CatalogRef.of("tag"), null, Skill.LOCKPICK);
    assertTrue(settler.getInventoryNotes().contains("bobby pins"));
    settler.removePerkRank("tag");
    assertFalse(settler.getInventoryNotes().contains("bobby pins"));
  }

  /** Another origin that cannot take the pack drops it: pack and Tag skill entries go, caps fall back. */
  @Test
  void droppedPackRemovesItsEntries() {
    Character settler = settler();
    settler.setFreeTag(Skill.LOCKPICK, true);
    apply(settler);
    settler.chooseOrigin(CatalogRef.of("vault-dweller"));
    assertTrue(settler.getWeapons().isEmpty());
    assertTrue(settler.getApparel().isEmpty());
    assertTrue(settler.getItems().isEmpty());
    assertEquals("", settler.getInventoryNotes());
    assertEquals(0, settler.getCaps());

    settler.setFreeTag(Skill.SNEAK, true);
    assertEquals("", settler.getInventoryNotes(), "no pack, nothing synced");
  }

  /** Tagging before a pack is chosen enters nothing: the starting equipment step has not begun. */
  @Test
  void nothingBeforeAPack() {
    Character character = Character.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.setFreeTag(Skill.LOCKPICK, true);
    assertEquals("", character.getInventoryNotes());
    assertEquals(0, character.startingEquipmentSyncs());
  }

  /** CRB p. 81: a higher starting level raises the fixed starting caps. */
  @Test
  void startingLevelUpdatesCaps() {
    Character settler = settler();
    apply(settler);
    settler.setStartingLevel(3);
    assertEquals(StartingEquipmentItems.caps(settler).fixedTotal(), settler.getCaps());
    assertTrue(settler.getCaps() > 45);
  }

  /** After creation only GM mode re-enters the pack; Tags are frozen then, so a Tag change changes nothing. */
  @Test
  void afterCreationOnlyInGmMode() {
    Character settler = settler();
    apply(settler);
    settler.completeCreation();
    assertFalse(settler.canApplyStartingEquipment());
    assertThrows(IllegalStateException.class, () -> apply(settler));
    settler.setGmMode(true);
    assertTrue(settler.canApplyStartingEquipment());
    assertEquals(List.of("Pipe Gun", "Pipe Wrench", "Tough Clothing", "A dented harmonica"),
        settler.startingEntryNames());
    settler.setCaps(7);
    apply(settler);
    assertEquals(7, settler.getCaps(), "caps are only set during creation");
  }

  /** The cancelled dialog restores the selection. */
  @Test
  void restoresASnapshot() {
    Character settler = settler();
    StartingEquipment snapshot = settler.getStartingEquipment();
    settler.chooseEquipmentPack(CatalogRef.of("trader"));
    settler.restoreStartingEquipment(snapshot);
    assertEquals(snapshot, settler.getStartingEquipment());
  }

  private static void apply(Character character) {
    character.applyStartingEquipment(character.getStartingEquipment());
  }

  private static Character settler() {
    Character character = Character.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.chooseEquipmentPack(CatalogRef.of("settler"));
    character.chooseEquipmentOption(0, "Pipe wrench");
    character.setTrinket("A dented harmonica");
    return character;
  }
}
