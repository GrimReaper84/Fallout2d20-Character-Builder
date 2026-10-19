package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.StartingEquipmentAllowance;
import java.util.OptionalInt;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Starting level of a character in creation (CRB p. 49): a number field and −/+ buttons, the XP that
 * follows from the level, and from level 2 the extra caps and maximum rarity for equipment (CRB p. 81).
 * The field takes its value on Enter or when it loses focus; anything but a whole number of at least 1
 * resets it. Every change goes to the model and then to {@code onChanged}, which rebuilds the overview.
 */
final class LevelSection {

  private static final @NonNull String CAPS_TOOLTIP =
      "Extra caps for buying equipment at a higher starting level (CRB p. 81).";
  private static final double FIELD_WIDTH = 60;

  private LevelSection() {}

  /** The level field with −/+ buttons. */
  static @NonNull Node control(@NonNull PlayerCharacter character, @NonNull Runnable onChanged) {
    int level = character.advancement().getLevel();
    TextField field = new TextField(Integer.toString(level));
    field.setPrefWidth(FIELD_WIDTH);
    field.setOnAction(event -> commit(character, field, onChanged));
    field.focusedProperty().addListener((obs, old, focused) -> {
      if (!focused) {
        commit(character, field, onChanged);
      }
    });
    return new HBox(6, field, StepButtons.of(
        StepButtons.minus(level > 1 ? "Lower the starting level" : "Level 1 is the lowest level.", level > 1,
            () -> step(character, -1, onChanged)),
        StepButtons.plus("Raise the starting level", true, () -> step(character, 1, onChanged))));
  }

  /** From level 2: the extra starting caps and the maximum rarity (CRB p. 81); {@code null} at level 1. */
  static @Nullable Label startingCaps(@NonNull PlayerCharacter character) {
    int level = character.advancement().getLevel();
    if (level <= 1) {
      return null;
    }
    OptionalInt rarity = StartingEquipmentAllowance.maxRarity(level);
    Label caps = new Label("Starting caps: " + StartingEquipmentAllowance.caps(level) + " caps, "
        + (rarity.isPresent() ? "max rarity " + rarity.getAsInt() : "any rarity"));
    caps.setTooltip(new Tooltip(CAPS_TOOLTIP));
    return caps;
  }

  /**
   * Takes the typed level if it is a whole number of at least 1 the model accepts, else shows the
   * current level again.
   */
  private static void commit(@NonNull PlayerCharacter character, @NonNull TextField field,
      @NonNull Runnable onChanged) {
    int typed;
    try {
      typed = Integer.parseInt(field.getText().strip());
    } catch (NumberFormatException e) {
      typed = 0;
    }
    if (typed != character.advancement().getLevel()) {
      change(character, typed, onChanged);
    }
  }

  /**
   * One level up or down from the current level: a typed level is taken when the button gets the
   * focus, so the captured level of the last rebuild may be outdated.
   */
  private static void step(@NonNull PlayerCharacter character, int delta, @NonNull Runnable onChanged) {
    change(character, Math.max(1, character.advancement().getLevel() + delta), onChanged);
  }

  /** Sets the level if the model accepts it; the rebuild shows the current level either way. */
  private static void change(@NonNull PlayerCharacter character, int level, @NonNull Runnable onChanged) {
    try {
      character.advancement().setStartingLevel(level);
    } catch (IllegalArgumentException e) {
      // below 1 or too high to compute: keep the current level
    }
    onChanged.run();
  }
}
