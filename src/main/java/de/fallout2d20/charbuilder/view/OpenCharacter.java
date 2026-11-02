package de.fallout2d20.charbuilder.view;

import java.util.Optional;
import javafx.scene.control.Tab;
import org.jspecify.annotations.NonNull;

/**
 * A character tab of the main window (H1): the character's state, its view and its tab. It is kept as the tab's user
 * data, see {@link #of(Tab)}.
 */
record OpenCharacter(@NonNull CharacterDocument document, @NonNull CharacterView view, @NonNull Tab tab) {

  /** Creates the open character and attaches it to its tab. */
  static @NonNull OpenCharacter attach(@NonNull CharacterDocument document, @NonNull CharacterView view,
      @NonNull Tab tab) {
    OpenCharacter character = new OpenCharacter(document, view, tab);
    tab.setUserData(character);
    return character;
  }

  /** The character of a tab. */
  static @NonNull Optional<OpenCharacter> of(@NonNull Tab tab) {
    return tab.getUserData() instanceof OpenCharacter character ? Optional.of(character) : Optional.empty();
  }
}
