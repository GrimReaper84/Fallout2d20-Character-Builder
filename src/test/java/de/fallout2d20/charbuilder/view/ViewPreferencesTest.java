package de.fallout2d20.charbuilder.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ViewPreferencesTest {

  private final Preferences node = Preferences.userRoot().node("de/fallout2d20/charbuilder/test/view-preferences");

  @AfterEach
  void removeNode() throws Exception {
    node.removeNode();
    node.flush();
  }

  @Test
  void returnsTheDefaultUntilAValueIsStored() {
    ViewPreferences preferences = new ViewPreferences(node);
    assertTrue(preferences.getBoolean("perks.available.expanded", true));

    preferences.putBoolean("perks.available.expanded", false);

    assertFalse(new ViewPreferences(node).getBoolean("perks.available.expanded", true));
  }
}
