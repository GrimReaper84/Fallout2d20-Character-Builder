package de.fallout2d20.charbuilder.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class WeaponSelectionTest {

  private final WeaponSelection selection = new WeaponSelection();

  @Test
  void startsOnTheUnarmedStrike() {
    assertEquals(WeaponSelection.UNARMED_STRIKE_INDEX, selection.current(0));
  }

  @Test
  void keepsAnExistingWeapon() {
    selection.select(2);
    assertEquals(2, selection.current(3));
  }

  @Test
  void aRemovedWeaponOrAClearedSelectionSelectsNothing() {
    selection.select(2);
    assertNull(selection.current(2));
    selection.clear();
    assertNull(selection.current(3));
  }
}
