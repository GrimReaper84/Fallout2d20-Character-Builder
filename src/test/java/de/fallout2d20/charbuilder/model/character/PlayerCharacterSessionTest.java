package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.session.SessionValues;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Session vitals (D5): current HP, rads, Radiation Points, Luck points, injuries, notes. */
class PlayerCharacterSessionTest {

  /** A complete Survivor with END 5 and LCK 5: 10 max HP, 5 Luck points (CRB p. 21, 49). */
  private static PlayerCharacter survivor() {
    return PlayerCharacterAdvancementTest.complete("survivor");
  }

  @Test
  void currentHpIsClampedAndTheMaximumIsStoredAsFull() {
    PlayerCharacter character = survivor();
    character.session().setCurrentHp(4);
    assertEquals(4, SessionValues.of(character).currentHp());
    character.session().setCurrentHp(-3);
    assertEquals(0, SessionValues.of(character).currentHp());
    character.session().setCurrentHp(99);
    assertNull(character.session().getVitals().currentHp());
    assertEquals(10, SessionValues.of(character).currentHp());
    character.session().setCurrentHp(2);
    character.session().healFully();
    assertNull(character.session().getVitals().currentHp());
  }

  /** CRB p. 31: rads lower the maximum; current HP above it fall, lower ones stay. */
  @Test
  void radsLowerTheMaximumHp() {
    PlayerCharacter character = survivor();
    character.session().setRads(3);
    SessionValues values = SessionValues.of(character);
    assertEquals(10, values.maxHp());
    assertEquals(7, values.effectiveMaxHp());
    assertEquals(7, values.currentHp(), "a full character stays at the lower maximum");

    character.session().setCurrentHp(5);
    character.session().setRads(4);
    assertEquals(5, SessionValues.of(character).currentHp(), "6 is still above 5");
    character.session().setRads(7);
    assertEquals(3, SessionValues.of(character).currentHp());
    character.session().setRads(99);
    assertEquals(10, character.session().getVitals().rads(), "rads stop at the maximum HP");
    assertEquals(0, SessionValues.of(character).currentHp());
  }

  /** CRB p. 35: removing rads does not heal HP. */
  @Test
  void clearingRadsDoesNotHeal() {
    PlayerCharacter character = survivor();
    character.session().setRads(4);
    character.session().setRads(0);
    assertEquals(6, SessionValues.of(character).currentHp());
    assertEquals(10, SessionValues.of(character).effectiveMaxHp());
  }

  @Test
  void levelUpAndGmLevelDownRespectRads() {
    PlayerCharacter character = survivor();
    character.session().setRads(2);
    character.session().setCurrentHp(5);
    character.advancement().gainMilestoneLevel();
    assertEquals(6, SessionValues.of(character).currentHp(), "CRB p. 49: +1 HP per level");

    PlayerCharacter full = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor")).level(5).xp(1_000)
        .creationComplete(true).vitals(new Vitals(12, 2, 0)).build();
    full.setGmMode(true);
    full.advancement().setLevelAsGm(3, 300);
    assertNull(full.session().getVitals().currentHp(), "12 HP above the new maximum 12 − 2 rads");
    assertEquals(10, SessionValues.of(full).currentHp());
  }

  /** A level-up only raises stored HP; Luck points, injuries and notes stay. */
  @Test
  void levelUpKeepsTheOtherSessionValues() {
    PlayerCharacter character = survivor();
    character.session().setLuckPoints(2);
    character.session().setInjured(HitLocation.LEFT_LEG, true);
    character.session().setNotes("Addicted to Jet");
    character.advancement().gainMilestoneLevel();
    character.advancement().addXp(300);
    assertEquals(2, SessionValues.of(character).luckPoints());
    assertEquals(Set.of(HitLocation.LEFT_LEG), SessionValues.of(character).injuries());
    assertEquals("Addicted to Jet", character.session().getVitals().notes());
  }

  /** CRB p. 31: rads up to the full maximum leave 0 HP; healing cannot go above 0 then. */
  @Test
  void radsEqualToTheMaximumLeaveNoHp() {
    PlayerCharacter character = survivor();
    character.session().setRads(10);
    character.session().healFully();
    SessionValues values = SessionValues.of(character);
    assertEquals(0, values.effectiveMaxHp());
    assertEquals(0, values.currentHp());
    character.session().setCurrentHp(3);
    assertEquals(0, SessionValues.of(character).currentHp());
  }

  /** Ghouls are immune to radiation damage (CRB p. 47): no rads row unless rads are stored. */
  @Test
  void radsRowOnlyForCharactersThatTakeRads() {
    assertTrue(SessionValues.of(survivor()).showsRads());
    PlayerCharacter ghoul = PlayerCharacterAdvancementTest.complete("ghoul");
    assertFalse(SessionValues.of(ghoul).showsRads());
    ghoul.session().setRads(1);
    assertTrue(SessionValues.of(ghoul).showsRads());
  }

  /** WOA p. 25: a Child of Atom stores up to 5 Radiation Points; other origins none. */
  @Test
  void radiationPointsFollowTheOrigin() {
    PlayerCharacter child = PlayerCharacterAdvancementTest.complete("child-of-atom");
    assertTrue(SessionValues.of(child).showsRadiationPoints());
    child.session().setRadiationPoints(7);
    assertEquals(5, child.session().getVitals().radiationPoints());
    child.session().setRadiationPoints(-1);
    assertEquals(0, child.session().getVitals().radiationPoints());

    PlayerCharacter survivor = survivor();
    survivor.session().setRadiationPoints(3);
    assertEquals(0, SessionValues.of(survivor).radiationPoints());
    assertFalse(SessionValues.of(survivor).showsRadiationPoints());
  }

  /** CRB p. 21: Luck points between 0 and LCK, refreshed by the GM; a lower maximum caps them. */
  @Test
  void luckPointsAreSpentAndRefreshed() {
    PlayerCharacter character = survivor();
    assertEquals(5, SessionValues.of(character).luckPoints());
    character.session().setLuckPoints(2);
    assertEquals(2, SessionValues.of(character).luckPoints());
    character.session().setLuckPoints(9);
    assertNull(character.session().getVitals().luckPoints());
    character.session().setLuckPoints(4);
    character.session().refreshLuck();
    assertNull(character.session().getVitals().luckPoints());

    PlayerCharacter imported = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor"))
        .creationComplete(true).vitals(new Vitals(null, 0, 0, 8, Set.of(), "")).build();
    assertEquals(5, SessionValues.of(imported).luckPoints(), "read as the current maximum");
  }

  /** CRB p. 32: injuries per hit location of the body; locations the body lacks are ignored. */
  @Test
  void injuriesBelongToTheBody() {
    PlayerCharacter character = survivor();
    character.session().setInjured(HitLocation.LEFT_ARM, true);
    character.session().setInjured(HitLocation.HEAD, true);
    character.session().setInjured(HitLocation.HEAD, false);
    assertEquals(Set.of(HitLocation.LEFT_ARM), SessionValues.of(character).injuries());
    assertThrows(IllegalArgumentException.class, () -> character.session().setInjured(HitLocation.THRUSTER, true));

    PlayerCharacter handy = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("mister-handy"))
        .creationComplete(true)
        .vitals(new Vitals(null, 0, 0, null, Set.of(HitLocation.HEAD, HitLocation.THRUSTER), "")).build();
    assertEquals(Set.of(HitLocation.THRUSTER), SessionValues.of(handy).injuries());
  }

  @Test
  void sessionValuesOnlyAfterCreationButNotesAlways() {
    PlayerCharacter character = PlayerCharacter.createNew();
    assertThrows(IllegalStateException.class, () -> character.session().setCurrentHp(3));
    assertThrows(IllegalStateException.class, () -> character.session().setRads(1));
    assertThrows(IllegalStateException.class, () -> character.session().setLuckPoints(1));
    assertThrows(IllegalStateException.class, () -> character.session().setInjured(HitLocation.HEAD, true));
    character.session().setNotes("Med-X, 2 rounds left");
    assertEquals("Med-X, 2 rounds left", character.session().getVitals().notes());
    character.session().setNotes(null);
    assertEquals("", character.session().getVitals().notes());
  }

  @Test
  void vitalsRejectNegativeValues() {
    assertThrows(IllegalArgumentException.class, () -> new Vitals(null, 0, 0, -1, Set.of(), ""));
    assertThrows(IllegalArgumentException.class, () -> new Vitals(-1, 0, 0));
  }
}
