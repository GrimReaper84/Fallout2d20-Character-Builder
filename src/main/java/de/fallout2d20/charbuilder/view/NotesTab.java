package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Character;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import org.jspecify.annotations.NonNull;

/**
 * Notes tab: the free-text session notes (conditions, chems and their duration, …) as one field filling the
 * tab; editable in every mode. Typing only marks the character edited, it does not rebuild the view.
 */
final class NotesTab {

  private NotesTab() {}

  static @NonNull Node build(@NonNull Character character, @NonNull ViewCallbacks callbacks) {
    TextArea notes = new TextArea(character.getVitals().notes());
    notes.setPromptText("Conditions, chems and their duration, addictions, …");
    notes.setWrapText(true);
    notes.textProperty().addListener((obs, old, text) -> {
      character.setSessionNotes(text);
      callbacks.onEdited().run();
    });
    BorderPane pane = new BorderPane(notes);
    pane.setPadding(new Insets(16));
    return pane;
  }
}
