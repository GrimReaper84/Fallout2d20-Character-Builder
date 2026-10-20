package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
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

  static @NonNull Node build(@NonNull PlayerCharacter character, @NonNull ViewCallbacks callbacks) {
    TextArea notes = new TextArea(character.session().getVitals().notes());
    notes.setPromptText("Conditions, chems and their duration, addictions, …");
    notes.setWrapText(true);
    notes.textProperty().addListener((_, _, text) -> {
      character.session().setNotes(text);
      callbacks.onEdited().run();
    });
    BorderPane pane = new BorderPane(notes);
    pane.setPadding(new Insets(16));
    return pane;
  }
}
