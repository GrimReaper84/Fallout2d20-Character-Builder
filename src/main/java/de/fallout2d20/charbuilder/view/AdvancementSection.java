package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.advancement.AdvancementRules;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.view.dialog.AdvancementDialogs;
import java.util.List;
import java.util.stream.Collectors;
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
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Level and advancement of a complete character (CRB p. 49): level and XP, a field to add the XP the GM
 * awards, a milestone level button, and – while there are points to spend or staged choices – the skill
 * ranks and perks left, the advancement hints and the Apply and Discard buttons. Gaining levels and
 * applying ask first (user decisions). Every change goes to the model and then to {@code onChanged}.
 */
final class AdvancementSection {

  private static final @NonNull String HINT_STYLE = "-fx-text-fill: #a35200;";
  private static final double FIELD_WIDTH = 80;

  private AdvancementSection() {}

  /** Adds the level, XP and advancement rows to a four-column overview grid. */
  static void addTo(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onChanged) {
    pane.add(new Label("Level"));
    pane.add(new Label(Integer.toString(character.getLevel())));
    pane.add(new Label("XP"));
    pane.add(new Label(Long.toString(character.getXp())));

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
          character.gainMilestoneLevel();
        } catch (IllegalArgumentException e) {
          Notifications.create().owner(milestone).title("Milestone level").text(e.getMessage()).showWarning();
          return;
        }
        onChanged.run();
      }
    });
    pane.add(new Label("Gain XP"));
    pane.add(new HBox(6, amount, add, milestone), "span 3");

    if (!AdvancementRules.isActive(character)) {
      return;
    }
    pane.add(new Label("Skill ranks left"));
    pane.add(new Label(Integer.toString(character.advancementSkillRanksLeft())));
    pane.add(new Label("Perks left"));
    pane.add(new Label(Integer.toString(character.advancementPerksLeft())));
    List<CreationProblem> problems = AdvancementRules.problems(character);
    if (!problems.isEmpty()) {
      Label hints = new Label(problems.stream().map(CreationProblem::message).collect(Collectors.joining("\n")));
      hints.setStyle(HINT_STYLE);
      hints.setWrapText(true);
      pane.add(new Label("Rule hints"), "top");
      pane.add(hints, "span 3");
    }
    if (character.hasStagedAdvancement()) {
      pane.add(new Label("Staged choices"));
      pane.add(new HBox(6, applyButton(character, problems, onChanged), discardButton(character, onChanged)),
          "span 3");
    }
  }

  /** Adds the typed XP amount; a level gain is confirmed first. Invalid input shows a warning. */
  private static void addXp(@NonNull Character character, @NonNull TextField field, @NonNull Runnable onChanged) {
    long amount;
    int levels;
    try {
      amount = Long.parseLong(field.getText().strip());
      levels = character.levelsGainedBy(amount);
    } catch (IllegalArgumentException e) {
      Notifications.create().owner(field).title("Add XP")
          .text("Enter a whole number of XP of at least 1.").showWarning();
      field.clear();
      return;
    }
    if (levels > 0 && !AdvancementDialogs.confirmLevelUp(window(field), levels)) {
      return;
    }
    character.addXp(amount);
    onChanged.run();
  }

  /** Apply, disabled with the reason while advancement hints exist (user decision). */
  private static @NonNull Node applyButton(@NonNull Character character, @NonNull List<CreationProblem> problems,
      @NonNull Runnable onChanged) {
    Button apply = new Button("Apply");
    apply.setGraphic(new FontIcon("fas-check"));
    apply.setDisable(!problems.isEmpty());
    apply.setOnAction(event -> {
      if (AdvancementRules.canApply(character) && AdvancementDialogs.confirmApply(window(apply))) {
        character.applyAdvancement();
        onChanged.run();
      }
    });
    HBox wrapper = new HBox(apply);
    Tooltip.install(wrapper, new Tooltip(problems.isEmpty()
        ? "Make the staged skill ranks and perks permanent."
        : "Resolve the rule hints first, e.g. by taking back staged choices."));
    return wrapper;
  }

  private static @NonNull Node discardButton(@NonNull Character character, @NonNull Runnable onChanged) {
    Button discard = iconButton("Discard", "fas-undo", "Take back all staged skill ranks and perks.");
    discard.setOnAction(event -> {
      character.discardAdvancement();
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
