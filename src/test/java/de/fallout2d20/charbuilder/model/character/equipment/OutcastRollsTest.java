package de.fallout2d20.charbuilder.model.character.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.FixedDice;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import org.junit.jupiter.api.Test;

/** Rolls on the Outcast Equipment Table (WG p. 19, user decision 2026-10-26: a dice button). */
class OutcastRollsTest {

  /** Ex-Scribe: three rolls; d20 7 adds a Combat Chest Piece and notes the result on the roll line. */
  @Test
  void exScribe() {
    PlayerCharacter outcast = outcast("ex-scribe");
    assertEquals(3, OutcastRolls.left(outcast));
    assertTrue(outcast.gear().getNotes().contains("[Origin] Roll: 1d20 junk"),
        "F3: the origin's rolls are listed once a pack is applied");

    assertEquals("Combat Chest Piece", OutcastRolls.roll(outcast, FixedDice.of(7)).name());
    assertEquals(2, OutcastRolls.left(outcast));
    WornApparel chest = outcast.gear().getApparel().stream()
        .filter(item -> item.displayName().equals("Combat Chest Piece")).findFirst().orElseThrow();
    assertEquals(null, chest.startingSource(), "added by hand, kept on a pack change");
    assertTrue(outcast.gear().getNotes().contains(
        "[Pack] Roll: 3 rolls on the Outcast Equipment Table (WG p. 19) → d20 7: Combat Chest Piece"));

    OutcastRolls.roll(outcast, FixedDice.of(9));
    OutcastRolls.roll(outcast, FixedDice.of(9));
    assertEquals(2, outcast.gear().getItems().stream().filter(item -> item.displayName().equals("Stimpak"))
        .findFirst().orElseThrow().quantity(), "stacked");
    assertEquals(0, OutcastRolls.left(outcast));
    assertThrows(IllegalStateException.class, () -> OutcastRolls.roll(outcast, FixedDice.of(1)));
  }

  /**
   * After a pack change the rolled item and the old line stay, but only the new pack's rolls are open (user decision
   * 2026-10-26: the rest of the old pack lapses).
   */
  @Test
  void packChangeKeepsRolledResultsOnly() {
    PlayerCharacter outcast = outcast("ex-scribe");
    OutcastRolls.roll(outcast, FixedDice.of(7));
    outcast.startingGear().choosePack(CatalogRef.of("ex-knight"));
    outcast.startingGear().apply(outcast.startingGear().getSelection());
    assertTrue(outcast.gear().getApparel().stream().anyMatch(item -> item.displayName().equals("Combat Chest Piece")));
    assertTrue(outcast.gear().getNotes().contains("→ d20 7: Combat Chest Piece"), "the old line stays as text");
    assertEquals(2, OutcastRolls.left(outcast), "the Ex-Knight's two rolls");
  }

  /** Entering the same pack again keeps the rolled line and adds no fresh one. */
  @Test
  void reapplyingThePackAddsNoRolls() {
    PlayerCharacter outcast = outcast("ex-scribe");
    OutcastRolls.roll(outcast, FixedDice.of(7));
    outcast.startingGear().apply(outcast.startingGear().getSelection());
    assertEquals(2, OutcastRolls.left(outcast));
    assertEquals(1, outcast.gear().getNotes().lines().filter(line -> line.contains("Outcast Equipment Table")).count());
  }

  @Test
  void noLineNoRolls() {
    assertEquals(0, OutcastRolls.left(PlayerCharacter.createNew()));
  }

  private static PlayerCharacter outcast(String pack) {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("brotherhood-outcast"));
    character.startingGear().choosePack(CatalogRef.of(pack));
    character.startingGear().apply(character.startingGear().getSelection());
    return character;
  }
}
