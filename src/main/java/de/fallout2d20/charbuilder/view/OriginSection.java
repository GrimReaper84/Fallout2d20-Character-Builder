package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.origin.ExtraTagSkill;
import de.fallout2d20.charbuilder.catalog.origin.OriginCatalog;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
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
 * Origin choice of a character in creation: the origin (limited to the enabled books) and the
 * origin's own options. Every change goes to the model and then to
 * {@code onChanged}, which rebuilds the overview.
 */
final class OriginSection {

  private OriginSection() {}

  /**
   * Adds the origin rows to a four-column overview grid.
   *
   * @param onEdited called after an edit that needs no rebuild (typed text)
   * @param onChanged called after an edit that changes other values; rebuilds the overview
   */
  static void addTo(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onEdited,
      @NonNull Runnable onChanged) {
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
    pane.add(new Label("Origin"));
    pane.add(origins, "span 3");
    selected.ifPresent(origin -> addOptions(pane, character, origin, onEdited, onChanged));
  }

  private static void addOptions(@NonNull MigPane pane, @NonNull Character character,
      @NonNull OriginData origin, @NonNull Runnable onEdited, @NonNull Runnable onChanged) {
    pane.add(new Label("Origin trait"));
    Label trait = new Label(origin.traitName() == null ? "Trait list" : origin.traitName());
    trait.setTooltip(new Tooltip(describe(origin)));
    pane.add(trait, "span 3");
    OriginChoices choices = character.getOriginChoices();
    Skill fixedTag = origin.fixedTagSkill();
    if (fixedTag != null) {
      pane.add(new Label("Fixed Tag skill"));
      pane.add(new Label(fixedTag.displayName() + " (in addition to the three free Tag skills)"), "span 3");
    }
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
      pane.add(tagSkill, "span 3");
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
      pane.add(purpose, "span 3");
    }
  }

  /** Sources and own summary of the origin trait, e.g. {@code CRB p. 52 – Immune to …}. */
  static @NonNull String describe(@NonNull OriginData origin) {
    return origin.sources().stream().map(Source::toString).collect(Collectors.joining(", "))
        + " – " + origin.traitSummary();
  }
}
