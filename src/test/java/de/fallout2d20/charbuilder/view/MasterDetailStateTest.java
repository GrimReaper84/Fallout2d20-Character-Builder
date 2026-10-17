package de.fallout2d20.charbuilder.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MasterDetailStateTest {

  @Test
  void startsOnTheGivenEntry() {
    assertEquals(WeaponsTab.UNARMED_STRIKE_INDEX, new MasterDetailState(WeaponsTab.UNARMED_STRIKE_INDEX).current(0));
    assertNull(new MasterDetailState(null).current(3));
  }

  @Test
  void keepsAnExistingEntry() {
    MasterDetailState state = new MasterDetailState(null);
    state.select(2);
    assertEquals(2, state.current(3));
  }

  @Test
  void aRemovedEntryOrAClearedSelectionSelectsNothing() {
    MasterDetailState state = new MasterDetailState(null);
    state.select(2);
    assertNull(state.current(2));
    state.clear();
    assertNull(state.current(3));
  }
}
