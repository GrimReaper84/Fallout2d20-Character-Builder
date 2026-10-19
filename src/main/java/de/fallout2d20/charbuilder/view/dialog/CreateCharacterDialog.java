package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Asks before completing character creation, which cannot be undone (user decision). Open creation
 * hints are listed; they do not block (soft hints, the player or GM may finish anyway). Cancel is the
 * default button, so pressing Enter does not end creation by accident.
 */
public final class CreateCharacterDialog {

  private CreateCharacterDialog() {}

  /** Returns whether the player chose to create the character. */
  public static boolean confirm(@Nullable Window owner, @NonNull List<CreationProblem> problems) {
    boolean hints = !problems.isEmpty();
    ButtonType create = new ButtonType(hints ? "Create anyway" : "Create", ButtonBar.ButtonData.OK_DONE);
    Alert alert = new Alert(hints ? Alert.AlertType.WARNING : Alert.AlertType.CONFIRMATION, "", create,
        ButtonType.CANCEL);
    alert.initOwner(owner);
    alert.setTitle("Create Character");
    alert.setHeaderText(hints ? "Create the character anyway?" : "Create the character?");
    String openHints = "";
    if (hints) {
      openHints = problems.size() == 1 ? "1 creation hint is still open. "
          : problems.size() + " creation hints are still open. ";
    }
    alert.setContentText(openHints + "After creating, the creation choices can no longer be changed.");
    if (hints) {
      TextArea details = new TextArea(problems.stream()
          .map(problem -> "• " + problem.topic() + ": " + problem.message())
          .collect(Collectors.joining("\n")));
      details.setEditable(false);
      details.setWrapText(true);
      details.setPrefRowCount(Math.min(12, problems.size() + 1));
      alert.getDialogPane().setExpandableContent(details);
      alert.getDialogPane().setExpanded(true);
    }
    ((Button) alert.getDialogPane().lookupButton(create)).setDefaultButton(false);
    ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);
    Optional<ButtonType> answer = alert.showAndWait();
    return answer.isPresent() && answer.get() == create;
  }
}
