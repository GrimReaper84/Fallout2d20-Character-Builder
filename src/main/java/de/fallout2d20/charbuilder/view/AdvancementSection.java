package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.advancement.AdvancementRules;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.view.dialog.AdvancementDialogs;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import org.controlsfx.control.Notifications;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Advancement of a complete character (CRB p. 49) for the header bar: a field to add the XP the GM awards,
 * a milestone level button, and – while there are points to spend or staged choices – the skill ranks and
 * perks left with the Apply and Discard buttons. The advancement hints are shown in the Stats and Perks
 * tabs. Gaining levels and applying ask first (user decisions). Every change goes to the model and then to
 * {@code onChanged}.
 */
final class AdvancementSection {

  private static final double FIELD_WIDTH = 80;

  private AdvancementSection() {}

  /** XP field, Add XP and Milestone level buttons. */
  static @NonNull Node gainXp(@NonNull PlayerCharacter character, @NonNull Runnable onChanged) {
    TextField amount = new TextField();
    amount.setPromptText("XP");
    amount.setPrefWidth(FIELD_WIDTH);
    Button add = iconButton("Add XP", "fas-plus", "Add the XP awarded by the GM");
    add.setOnAction(event -> addXp(character, amount, onChanged));
    amount.setOnAction(event -> addXp(character, amount, onChanged));
    Button milestone = iconButton("Milestone level", "fas-arrow-up", "The GM grants a level (CRB p. 49)");
    milestone.setOnAction(event -> {
      if (AdvancementDialogs.confirmLevelUp(window(milestone), 1)) {
        try {
          character.advancement().gainMilestoneLevel();
        } catch (IllegalArgumentException e) {
          Notifications.create().owner(milestone).title("Milestone level").text(e.getMessage()).showWarning();
          return;
        }
        onChanged.run();
      }
    });
    return new HBox(6, amount, add, milestone);
  }

  /**
   * While advancement is active: skill ranks and perks left and, with staged choices, Apply and Discard;
   * {@code null} otherwise.
   */
  static @Nullable Node pending(@NonNull PlayerCharacter character, @NonNull Runnable onChanged) {
    if (!AdvancementRules.isActive(character)) {
      return null;
    }
    Label left = new Label(character.advancement().skillRanksLeft() + " skill ranks, "
        + character.advancement().perksLeft() + " perks left");
    left.setTooltip(new Tooltip("Spend them in the Stats and Perks tabs (CRB p. 49)."));
    HBox box = new HBox(6, left);
    box.setAlignment(Pos.CENTER_LEFT);
    if (character.advancement().hasStaged()) {
      List<CreationProblem> problems = AdvancementRules.problems(character);
      box.getChildren().addAll(applyButton(character, problems, onChanged), discardButton(character, onChanged));
    }
    return box;
  }

  /** Adds the typed XP amount; a level gain is confirmed first. Invalid input shows a warning. */
  private static void addXp(@NonNull PlayerCharacter character, @NonNull TextField field, @NonNull Runnable onChanged) {
    long amount;
    int levels;
    try {
      amount = Long.parseLong(field.getText().strip());
      levels = character.advancement().levelsGainedBy(amount);
    } catch (IllegalArgumentException e) {
      Notifications.create().owner(field).title("Add XP")
          .text("Enter a whole number of XP of at least 1.").showWarning();
      field.clear();
      return;
    }
    if (levels > 0 && !AdvancementDialogs.confirmLevelUp(window(field), levels)) {
      return;
    }
    character.advancement().addXp(amount);
    onChanged.run();
  }

  /** Apply, disabled with the reason while advancement hints exist (user decision). */
  private static @NonNull Node applyButton(@NonNull PlayerCharacter character, @NonNull List<CreationProblem> problems,
      @NonNull Runnable onChanged) {
    Button apply = new Button("Apply");
    apply.setGraphic(new FontIcon("fas-check"));
    apply.setDisable(!problems.isEmpty());
    apply.setOnAction(event -> {
      if (AdvancementRules.canApply(character) && AdvancementDialogs.confirmApply(window(apply))) {
        character.advancement().apply();
        onChanged.run();
      }
    });
    HBox wrapper = new HBox(apply);
    Tooltip.install(wrapper, new Tooltip(problems.isEmpty()
        ? "Make the staged skill ranks and perks permanent."
        : "Resolve the rule hints first, e.g. by taking back staged choices."));
    return wrapper;
  }

  private static @NonNull Node discardButton(@NonNull PlayerCharacter character, @NonNull Runnable onChanged) {
    Button discard = iconButton("Discard", "fas-undo", "Take back all staged skill ranks and perks.");
    discard.setOnAction(event -> {
      character.advancement().discard();
      onChanged.run();
    });
    return discard;
  }

  private static @NonNull Button iconButton(@NonNull String text, @NonNull String icon, @NonNull String tooltip) {
    Button button = new Button(text);
    button.setGraphic(new FontIcon(icon));
    button.setTooltip(new Tooltip(tooltip));
    return button;
  }

  private static @Nullable Window window(@NonNull Node node) {
    return node.getScene() == null ? null : node.getScene().getWindow();
  }
}
