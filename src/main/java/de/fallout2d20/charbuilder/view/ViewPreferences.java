package de.fallout2d20.charbuilder.view;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.jspecify.annotations.NonNull;

/**
 * UI state that outlives the rebuilt view and app restarts, e.g. which perk groups are collapsed. Kept in
 * the user's Java preferences, never in the character file. A failing preferences backend only loses the
 * state: reads fall back to the defaults, writes are dropped.
 */
final class ViewPreferences {

  private final @NonNull Preferences node;

  ViewPreferences(@NonNull Preferences node) {
    this.node = node;
  }

  /** The preferences of the current user; looked up on each call, so merely loading the class writes nothing. */
  static @NonNull ViewPreferences user() {
    return new ViewPreferences(Preferences.userNodeForPackage(ViewPreferences.class));
  }

  boolean getBoolean(@NonNull String key, boolean defaultValue) {
    try {
      return node.getBoolean(key, defaultValue);
    } catch (IllegalStateException | SecurityException e) {
      return defaultValue;
    }
  }

  void putBoolean(@NonNull String key, boolean value) {
    try {
      node.putBoolean(key, value);
      node.flush();
    } catch (BackingStoreException | IllegalStateException | SecurityException e) {
      // UI state only; losing it is harmless.
    }
  }
}
