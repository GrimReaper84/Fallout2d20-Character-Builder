package de.fallout2d20.charbuilder.model.character.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
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
    PlayerCharacter settler = settler();
    settler.gear().setNotes("Bought a map");
    apply(settler);

    assertEquals(List.of("Pipe Gun", "Pipe Wrench"),
        settler.gear().getWeapons().stream().map(CarriedWeapon::displayName)
        .toList());
    assertTrue(settler.gear().getWeapons().stream().allMatch(weapon -> weapon.startingSource() == StartingSource.PACK));
    assertEquals(1, settler.gear().getApparel().size());
    assertEquals("""
        Bought a map
        [Pack] Roll: 6 + 3 CD rounds of .38 ammunition
        [Pack] Roll: 2 rolls on the random food table (CRB p. 202)""", settler.gear().getNotes());
    assertEquals(45, settler.gear().getCaps());
  }

  /**
   * Another pick replaces only the marked entries and the untouched roll lines; hand-added entries and a line with
   * the result written behind it stay.
   */
  @Test
  void anotherPickReplacesOnlyMarkedEntries() {
    PlayerCharacter settler = settler();
    apply(settler);
    settler.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    settler.gear().addItem(CarriedItem.of(CatalogRef.of("stimpak")));
    settler.gear().setNotes(settler.gear().getNotes().replace("rounds of .38 ammunition",
        "rounds of .38 ammunition = 11"));

    StartingEquipment snapshot = settler.startingGear().getSelection();
    settler.startingGear().choosePack(CatalogRef.of("trader"));
    settler.startingGear().chooseOption(0, "Leather armor chest piece");
    settler.startingGear().apply(snapshot);

    List<String> weapons = settler.gear().getWeapons().stream().map(CarriedWeapon::displayName).toList();
    assertEquals(List.of("10mm Pistol", "Pipe Gun"), weapons.stream().sorted().toList());
    assertTrue(settler.gear().getItems().stream().anyMatch(item -> item.displayName().equals("Stimpak")
        && item.startingSource() == null));
    assertTrue(settler.gear().getNotes().contains("[Pack] Roll: 6 + 3 CD rounds of .38 ammunition = 11"),
        "line with a result stays");
    assertFalse(settler.gear().getNotes().contains("random food table"), "untouched old line replaced");
    assertTrue(settler.gear().getNotes().contains("[Pack] Roll: 8 + 4 CD rounds of .38 ammunition"));
    assertEquals(50, settler.gear().getCaps());
  }

  /** CRB p. 81: a Tag change re-syncs only the Tag skill items; edits of pack entries survive. */
  @Test
  void tagChangeSyncsOnlyTagSkillItems() {
    PlayerCharacter settler = settler();
    apply(settler);
    settler.gear().setWeaponNotes(0, "scratched");
    settler.gear().setCaps(30);

    settler.abilities().setFreeTag(Skill.LOCKPICK, true);
    assertEquals("scratched", settler.gear().getWeapons().getFirst().notes());
    assertTrue(settler.gear().getNotes().endsWith("[Tag skill] Roll: 4 + 2 CD bobby pins"));
    assertEquals(30, settler.gear().getCaps(), "a Tag change keeps typed caps");

    settler.abilities().setFreeTag(Skill.LOCKPICK, false);
    assertFalse(settler.gear().getNotes().contains("[Tag skill]"));
    assertEquals("scratched", settler.gear().getWeapons().getFirst().notes());
  }

  /** CRB p. 81: Barter grants no item, only 2d20 caps – tagging and untagging it still re-syncs. */
  @Test
  void barterTagSyncsItsCaps() {
    PlayerCharacter settler = settler();
    apply(settler);
    settler.abilities().setFreeTag(Skill.BARTER, true);
    assertTrue(settler.gear().getNotes().endsWith("[Tag skill] Roll: 2d20 caps"));
    settler.abilities().setFreeTag(Skill.BARTER, false);
    assertFalse(settler.gear().getNotes().contains("2d20 caps"));
  }

  /** Taking and giving back Tag! re-syncs the Tag skill items too. */
  @Test
  void perkAndTraitChangesSync() {
    PlayerCharacter settler = settler();
    apply(settler);
    settler.addPerkRank(CatalogRef.of("tag"), null, Skill.LOCKPICK);
    assertTrue(settler.gear().getNotes().contains("bobby pins"));
    settler.removePerkRank("tag");
    assertFalse(settler.gear().getNotes().contains("bobby pins"));
  }

  /** Another origin that cannot take the pack drops it: pack and Tag skill entries go, caps fall back. */
  @Test
  void droppedPackRemovesItsEntries() {
    PlayerCharacter settler = settler();
    settler.abilities().setFreeTag(Skill.LOCKPICK, true);
    apply(settler);
    settler.chooseOrigin(CatalogRef.of("vault-dweller"));
    assertTrue(settler.gear().getWeapons().isEmpty());
    assertTrue(settler.gear().getApparel().isEmpty());
    assertTrue(settler.gear().getItems().isEmpty());
    assertEquals("", settler.gear().getNotes());
    assertEquals(0, settler.gear().getCaps());

    settler.abilities().setFreeTag(Skill.SNEAK, true);
    assertEquals("", settler.gear().getNotes(), "no pack, nothing synced");
  }

  /** Tagging before a pack is chosen enters nothing: the starting equipment step has not begun. */
  @Test
  void nothingBeforeAPack() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.abilities().setFreeTag(Skill.LOCKPICK, true);
    assertEquals("", character.gear().getNotes());
    assertEquals(0, character.startingGear().syncs());
  }

  /** CRB p. 81: a higher starting level raises the fixed starting caps. */
  @Test
  void startingLevelUpdatesCaps() {
    PlayerCharacter settler = settler();
    apply(settler);
    settler.advancement().setStartingLevel(3);
    assertEquals(StartingEquipmentItems.caps(settler).fixedTotal(), settler.gear().getCaps());
    assertTrue(settler.gear().getCaps() > 45);
  }

  /** After creation only GM mode re-enters the pack; Tags are frozen then, so a Tag change changes nothing. */
  @Test
  void afterCreationOnlyInGmMode() {
    PlayerCharacter settler = settler();
    apply(settler);
    settler.completeCreation();
    assertFalse(settler.startingGear().canApply());
    assertThrows(IllegalStateException.class, () -> apply(settler));
    settler.setGmMode(true);
    assertTrue(settler.startingGear().canApply());
    assertEquals(List.of("Pipe Gun", "Pipe Wrench", "Tough Clothing", "A dented harmonica"),
        settler.startingGear().entryNames());
    settler.gear().setCaps(7);
    apply(settler);
    assertEquals(7, settler.gear().getCaps(), "caps are only set during creation");
  }

  /** The cancelled dialog restores the selection. */
  @Test
  void restoresASnapshot() {
    PlayerCharacter settler = settler();
    StartingEquipment snapshot = settler.startingGear().getSelection();
    settler.startingGear().choosePack(CatalogRef.of("trader"));
    settler.startingGear().restore(snapshot);
    assertEquals(snapshot, settler.startingGear().getSelection());
  }

  private static void apply(PlayerCharacter character) {
    character.startingGear().apply(character.startingGear().getSelection());
  }

  private static PlayerCharacter settler() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.startingGear().choosePack(CatalogRef.of("settler"));
    character.startingGear().chooseOption(0, "Pipe wrench");
    character.startingGear().setTrinket("A dented harmonica");
    return character;
  }
}
