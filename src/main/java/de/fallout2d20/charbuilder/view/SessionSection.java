package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.session.SessionValues;
import java.util.function.IntConsumer;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Play-session values of a complete character (D5): current HP and rads (CRB p. 31), Rad Sponge Radiation
 * Points (WOA p. 25), Luck points (CRB p. 21), injuries per hit location (CRB p. 32) and session notes.
 * Each row: name, value, −/+ and an optional quick button. Clicks rebuild the overview; typed notes only
 * mark the document edited.
 */
final class SessionSection {

  private SessionSection() {}

  /** Adds the session rows to a four-column overview grid. */
  static void addTo(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onEdited,
      @NonNull Runnable onChanged) {
    SessionValues values = SessionValues.of(character);

    Label hp = new Label(values.currentHp() + " / " + values.effectiveMaxHp());
    if (values.rads() > 0) {
      hp.setTooltip(new Tooltip("Max HP " + values.maxHp() + " − " + values.rads() + " rads (CRB p. 31)"));
    }
    row(pane, "Current HP", hp, values.currentHp(), 0, values.effectiveMaxHp(),
        value -> character.setCurrentHp(value), onChanged,
        quick("fas-heart", "Full HP", "Heal to the maximum HP", values.currentHp() < values.effectiveMaxHp(),
            character::healFully, onChanged));

    if (values.showsRads()) {
      Label rads = new Label(Integer.toString(values.rads()));
      rads.setTooltip(new Tooltip("Each rad lowers the maximum HP by one (CRB p. 31)."));
      row(pane, "Rads", rads, values.rads(), 0, values.maxHp(), value -> character.setRads(value), onChanged,
          quick("fas-radiation", "Clear rads", "Remove all rads; HP do not heal with them (CRB p. 35)",
              values.rads() > 0, () -> character.setRads(0), onChanged));
    }

    if (values.showsRadiationPoints()) {
      row(pane, "Radiation points", new Label(values.radiationPoints() + " / " + values.maxRadiationPoints()),
          values.radiationPoints(), 0, values.maxRadiationPoints(), value -> character.setRadiationPoints(value),
          onChanged, null);
    }

    row(pane, "Luck points", new Label(values.luckPoints() + " / " + values.maxLuckPoints()),
        values.luckPoints(), 0, values.maxLuckPoints(), value -> character.setLuckPoints(value), onChanged,
        quick("fas-redo", "Refresh", "Refill all Luck points (GM: milestone or new quest, CRB p. 21)",
            values.luckPoints() < values.maxLuckPoints(), character::refreshLuck, onChanged));

    FlowPane injuries = new FlowPane(12, 4);
    for (HitLocation location : character.hitLocations()) {
      CheckBox box = new CheckBox(location.displayName());
      box.setSelected(values.injuries().contains(location));
      box.setTooltip(new Tooltip(injuryEffect(location)));
      box.setOnAction(event -> {
        character.setInjured(location, box.isSelected());
        onChanged.run();
      });
      injuries.getChildren().add(box);
    }
    pane.add(new Label("Injuries"), "top");
    pane.add(injuries, "span 3, growx");

    TextArea notes = new TextArea(character.getVitals().notes());
    notes.setPromptText("Conditions, chems and their duration, addictions, …");
    notes.setWrapText(true);
    notes.setPrefRowCount(3);
    notes.textProperty().addListener((obs, old, text) -> {
      character.setSessionNotes(text);
      onEdited.run();
    });
    pane.add(new Label("Session notes"), "top");
    pane.add(notes, "span 3, growx");
  }

  private static void row(@NonNull MigPane pane, @NonNull String name, @NonNull Label value, int current,
      int minimum, int maximum, @NonNull IntConsumer set, @NonNull Runnable onChanged,
      @Nullable Button quick) {
    pane.add(new Label(name));
    pane.add(value);
    pane.add(StepButtons.of(
        StepButtons.minus(current > minimum ? "Lower " + name : name + " is at " + minimum + ".",
            current > minimum, () -> step(set, current - 1, onChanged)),
        StepButtons.plus(current < maximum ? "Raise " + name : name + " is at its maximum of " + maximum + ".",
            current < maximum, () -> step(set, current + 1, onChanged))),
        quick == null ? "span 2" : "");
    if (quick != null) {
      pane.add(quick);
    }
  }

  private static void step(@NonNull IntConsumer set, int value, @NonNull Runnable onChanged) {
    set.accept(value);
    onChanged.run();
  }

  private static @NonNull Button quick(@NonNull String icon, @NonNull String text, @NonNull String tooltip,
      boolean enabled, @NonNull Runnable action, @NonNull Runnable onChanged) {
    Button button = new Button(text);
    button.setGraphic(new FontIcon(icon));
    button.setTooltip(new Tooltip(tooltip));
    button.setDisable(!enabled);
    button.setOnAction(event -> {
      action.run();
      onChanged.run();
    });
    return button;
  }

  /**
   * CRB p. 32: what an untreated injury at the location does, in own words; lasts until treated (p. 34). A
   * Mister Handy's optics count as head, main body as torso, thruster as leg (CRB p. 54).
   */
  private static @NonNull String injuryEffect(@NonNull HitLocation location) {
    String effect = switch (location) {
      case HEAD, OPTICS -> "dazed – lose the next turn's normal actions; +2 difficulty on tests that need sight.";
      case TORSO, MAIN_BODY -> "bleeding – 2 CD physical damage at the end of each of your turns, ignoring DR.";
      case LEFT_ARM, RIGHT_ARM, ARM_1, ARM_2, ARM_3 -> "drops what it holds and cannot be used.";
      case LEFT_LEG, RIGHT_LEG, THRUSTER -> "knocked prone; no Sprint, Move becomes a major action.";
    };
    return location.displayName() + ": " + effect + " Lasts until treated (CRB p. 34).";
  }
}
