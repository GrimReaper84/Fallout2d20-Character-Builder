package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.origin.ExtraTagSkill;
import de.fallout2d20.charbuilder.catalog.origin.OriginCatalog;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Origin of a character: the origin choice (header bar) and the origin's own options below the origin
 * trait (Stats tab). Every change goes to the model and then to {@code onChanged}, which rebuilds the view.
 */
final class OriginSection {

  private OriginSection() {}

  /** The origin choice (limited to the enabled books). */
  static @NonNull ComboBox<OriginData> choice(@NonNull PlayerCharacter character, @NonNull Runnable onChanged) {
    Optional<OriginData> selected = character.getOriginData();
    ComboBox<OriginData> origins = new ComboBox<>();
    origins.getItems().setAll(OriginCatalog.available(character.getEnabledBooks()));
    origins.setConverter(new DisplayConverter<>(OriginData::name));
    origins.setPromptText("Choose an origin");
    selected.ifPresent(origin -> {
      origins.setValue(origin);
      origins.setTooltip(new Tooltip(describe(origin)));
    });
    origins.valueProperty().addListener((obs, old, origin) -> {
      if (origin != null) {
        character.chooseOrigin(CatalogRef.of(origin.id()));
        onChanged.run();
      }
    });
    return origins;
  }

  /**
   * Adds the options of the chosen origin (extra Tag skill, purpose) as label + control rows to a
   * two-column grid; nothing while no catalogued origin is chosen. A fixed Tag skill (Ghoul) needs no
   * row: the origin trait's summary names it, and its Tag checkbox is checked and disabled.
   *
   * @param onEdited called after an edit that needs no rebuild (typed text)
   * @param onChanged called after an edit that changes other values; rebuilds the view
   */
  static void addOptions(@NonNull MigPane pane, @NonNull PlayerCharacter character, @NonNull Runnable onEdited,
      @NonNull Runnable onChanged) {
    Optional<OriginData> selected = character.getOriginData();
    if (selected.isEmpty()) {
      return;
    }
    OriginData origin = selected.get();
    OriginChoices choices = character.getOriginChoices();
    ExtraTagSkill extraTag = origin.extraTagSkill();
    if (extraTag != null) {
      ComboBox<Skill> tagSkill = new ComboBox<>();
      tagSkill.getItems().setAll(extraTag.options().isEmpty()
          ? Arrays.asList(Skill.values()) : extraTag.options());
      tagSkill.setConverter(new DisplayConverter<>(Skill::displayName));
      tagSkill.setPromptText("Choose a skill");
      tagSkill.setValue(choices.tagSkill());
      tagSkill.valueProperty().addListener((obs, old, skill) -> {
        character.chooseOriginTagSkill(skill);
        onChanged.run();
      });
      pane.add(new Label("Extra Tag skill"));
      pane.add(tagSkill);
    }
    if (!origin.purposeSuggestions().isEmpty()) {
      ComboBox<String> purpose = new ComboBox<>();
      purpose.setEditable(true);
      purpose.getItems().setAll(origin.purposeSuggestions());
      purpose.setPromptText("Choose or type a purpose");
      purpose.setValue(choices.purpose());
      // Typed text goes to the model at once (like the name), so saving right away keeps it; only
      // picking a suggestion rebuilds the overview, which would otherwise steal the focus.
      purpose.getEditor().textProperty().addListener((obs, old, text) -> {
        character.setPurpose(text);
        onEdited.run();
      });
      purpose.getSelectionModel().selectedIndexProperty().addListener((obs, old, index) -> {
        if (index.intValue() >= 0) {
          onChanged.run();
        }
      });
      pane.add(new Label("Purpose"));
      pane.add(purpose);
    }
  }

  /** The origin choices of a complete character as label + text rows of a two-column grid. */
  static void addChoices(@NonNull MigPane pane, @NonNull OriginChoices choices) {
    Skill tagSkill = choices.tagSkill();
    if (tagSkill != null) {
      pane.add(new Label("Extra Tag skill"));
      pane.add(new Label(tagSkill.displayName()));
    }
    String purpose = choices.purpose();
    if (purpose != null) {
      pane.add(new Label("Purpose"));
      pane.add(new Label(purpose));
    }
  }

  /** Sources and own summary of the origin trait, e.g. {@code CRB p. 52 – Immune to …}. */
  static @NonNull String describe(@NonNull OriginData origin) {
    return origin.sources().stream().map(Source::toString).collect(Collectors.joining(", "))
        + " – " + origin.traitSummary();
  }
}
