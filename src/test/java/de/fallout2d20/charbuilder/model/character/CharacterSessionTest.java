package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.session.SessionValues;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Session vitals (D5): current HP, rads, Radiation Points, Luck points, injuries, notes. */
class CharacterSessionTest {

  /** A complete Survivor with END 5 and LCK 5: 10 max HP, 5 Luck points (CRB p. 21, 49). */
  private static Character survivor() {
    return CharacterAdvancementTest.complete("survivor");
  }

  @Test
  void currentHpIsClampedAndTheMaximumIsStoredAsFull() {
    Character character = survivor();
    character.setCurrentHp(4);
    assertEquals(4, SessionValues.of(character).currentHp());
    character.setCurrentHp(-3);
    assertEquals(0, SessionValues.of(character).currentHp());
    character.setCurrentHp(99);
    assertNull(character.getVitals().currentHp());
    assertEquals(10, SessionValues.of(character).currentHp());
    character.setCurrentHp(2);
    character.healFully();
    assertNull(character.getVitals().currentHp());
  }

  /** CRB p. 31: rads lower the maximum; current HP above it fall, lower ones stay. */
  @Test
  void radsLowerTheMaximumHp() {
    Character character = survivor();
    character.setRads(3);
    SessionValues values = SessionValues.of(character);
    assertEquals(10, values.maxHp());
    assertEquals(7, values.effectiveMaxHp());
    assertEquals(7, values.currentHp(), "a full character stays at the lower maximum");

    character.setCurrentHp(5);
    character.setRads(4);
    assertEquals(5, SessionValues.of(character).currentHp(), "6 is still above 5");
    character.setRads(7);
    assertEquals(3, SessionValues.of(character).currentHp());
    character.setRads(99);
    assertEquals(10, character.getVitals().rads(), "rads stop at the maximum HP");
    assertEquals(0, SessionValues.of(character).currentHp());
  }

  /** CRB p. 35: removing rads does not heal HP. */
  @Test
  void clearingRadsDoesNotHeal() {
    Character character = survivor();
    character.setRads(4);
    character.setRads(0);
    assertEquals(6, SessionValues.of(character).currentHp());
    assertEquals(10, SessionValues.of(character).effectiveMaxHp());
  }

  @Test
  void levelUpAndGmLevelDownRespectRads() {
    Character character = survivor();
    character.setRads(2);
    character.setCurrentHp(5);
    character.gainMilestoneLevel();
    assertEquals(6, SessionValues.of(character).currentHp(), "CRB p. 49: +1 HP per level");

    Character full = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 5, 1_000, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(), Progression.NONE,
        new Vitals(12, 2, 0), null, null);
    full.setGmMode(true);
    full.setLevelAsGm(3, 300);
    assertNull(full.getVitals().currentHp(), "12 HP above the new maximum 12 − 2 rads");
    assertEquals(10, SessionValues.of(full).currentHp());
  }

  /** A level-up only raises stored HP; Luck points, injuries and notes stay. */
  @Test
  void levelUpKeepsTheOtherSessionValues() {
    Character character = survivor();
    character.setLuckPoints(2);
    character.setInjured(HitLocation.LEFT_LEG, true);
    character.setSessionNotes("Addicted to Jet");
    character.gainMilestoneLevel();
    character.addXp(300);
    assertEquals(2, SessionValues.of(character).luckPoints());
    assertEquals(Set.of(HitLocation.LEFT_LEG), SessionValues.of(character).injuries());
    assertEquals("Addicted to Jet", character.getVitals().notes());
  }

  /** CRB p. 31: rads up to the full maximum leave 0 HP; healing cannot go above 0 then. */
  @Test
  void radsEqualToTheMaximumLeaveNoHp() {
    Character character = survivor();
    character.setRads(10);
    character.healFully();
    SessionValues values = SessionValues.of(character);
    assertEquals(0, values.effectiveMaxHp());
    assertEquals(0, values.currentHp());
    character.setCurrentHp(3);
    assertEquals(0, SessionValues.of(character).currentHp());
  }

  /** Ghouls are immune to radiation damage (CRB p. 47): no rads row unless rads are stored. */
  @Test
  void radsRowOnlyForCharactersThatTakeRads() {
    assertTrue(SessionValues.of(survivor()).showsRads());
    Character ghoul = CharacterAdvancementTest.complete("ghoul");
    assertFalse(SessionValues.of(ghoul).showsRads());
    ghoul.setRads(1);
    assertTrue(SessionValues.of(ghoul).showsRads());
  }

  /** WOA p. 25: a Child of Atom stores up to 5 Radiation Points; other origins none. */
  @Test
  void radiationPointsFollowTheOrigin() {
    Character child = CharacterAdvancementTest.complete("child-of-atom");
    assertTrue(SessionValues.of(child).showsRadiationPoints());
    child.setRadiationPoints(7);
    assertEquals(5, child.getVitals().radiationPoints());
    child.setRadiationPoints(-1);
    assertEquals(0, child.getVitals().radiationPoints());

    Character survivor = survivor();
    survivor.setRadiationPoints(3);
    assertEquals(0, SessionValues.of(survivor).radiationPoints());
    assertFalse(SessionValues.of(survivor).showsRadiationPoints());
  }

  /** CRB p. 21: Luck points between 0 and LCK, refreshed by the GM; a lower maximum caps them. */
  @Test
  void luckPointsAreSpentAndRefreshed() {
    Character character = survivor();
    assertEquals(5, SessionValues.of(character).luckPoints());
    character.setLuckPoints(2);
    assertEquals(2, SessionValues.of(character).luckPoints());
    character.setLuckPoints(9);
    assertNull(character.getVitals().luckPoints());
    character.setLuckPoints(4);
    character.refreshLuck();
    assertNull(character.getVitals().luckPoints());

    Character imported = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 1, 0, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(), Progression.NONE,
        new Vitals(null, 0, 0, 8, Set.of(), ""), null, null);
    assertEquals(5, SessionValues.of(imported).luckPoints(), "read as the current maximum");
  }

  /** CRB p. 32: injuries per hit location of the body; locations the body lacks are ignored. */
  @Test
  void injuriesBelongToTheBody() {
    Character character = survivor();
    character.setInjured(HitLocation.LEFT_ARM, true);
    character.setInjured(HitLocation.HEAD, true);
    character.setInjured(HitLocation.HEAD, false);
    assertEquals(Set.of(HitLocation.LEFT_ARM), SessionValues.of(character).injuries());
    assertThrows(IllegalArgumentException.class, () -> character.setInjured(HitLocation.THRUSTER, true));

    Character handy = new Character("Test", CatalogRef.of("mister-handy"), OriginChoices.NONE, 1, 0, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(), Progression.NONE,
        new Vitals(null, 0, 0, null, Set.of(HitLocation.HEAD, HitLocation.THRUSTER), ""), null, null);
    assertEquals(Set.of(HitLocation.THRUSTER), SessionValues.of(handy).injuries());
  }

  @Test
  void sessionValuesOnlyAfterCreationButNotesAlways() {
    Character character = Character.createNew();
    assertThrows(IllegalStateException.class, () -> character.setCurrentHp(3));
    assertThrows(IllegalStateException.class, () -> character.setRads(1));
    assertThrows(IllegalStateException.class, () -> character.setLuckPoints(1));
    assertThrows(IllegalStateException.class, () -> character.setInjured(HitLocation.HEAD, true));
    character.setSessionNotes("Med-X, 2 rounds left");
    assertEquals("Med-X, 2 rounds left", character.getVitals().notes());
    character.setSessionNotes(null);
    assertEquals("", character.getVitals().notes());
  }

  @Test
  void vitalsRejectNegativeValues() {
    assertThrows(IllegalArgumentException.class, () -> new Vitals(null, 0, 0, -1, Set.of(), ""));
    assertThrows(IllegalArgumentException.class, () -> new Vitals(-1, 0, 0));
  }
}
