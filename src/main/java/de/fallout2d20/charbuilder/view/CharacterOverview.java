package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.LegacyImport;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import de.fallout2d20.charbuilder.model.character.Vitals;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Read-only overview of a character; only the name can be edited. Rebuilt completely whenever
 * another character is shown.
 */
final class CharacterOverview {

  private static final String LEGACY_BASELINE_TOOLTIP =
      "Imported from a legacy file; used as the base value for further advancement.";
  private static final String IMPORTED_TOOLTIP =
      "Imported from a legacy file; its effect is already included in the imported values.";
  private static final String UNRESOLVED_TOOLTIP =
      "Not yet checked against the rulebooks (shown with its legacy name).";

  private CharacterOverview() {}

  static Node build(Character character, Consumer<String> onNameChanged) {
    MigPane pane = new MigPane("insets 16, gap 8 4, wrap 4", "[right][grow,fill][right][grow,fill]");

    TextField name = new TextField(character.getName());
    name.setPromptText("Name");
    name.textProperty().addListener((obs, old, value) -> onNameChanged.accept(value));
    pane.add(new Label("Name"));
    pane.add(name, "span 3");
    pane.add(new Label("Origin"));
    CatalogRef origin = character.getOrigin();
    pane.add(origin == null ? new Label("Not chosen") : reference(origin), "span 3");
    pane.add(new Label("Level"));
    pane.add(new Label(Integer.toString(character.getLevel())));
    pane.add(new Label("XP"));
    pane.add(new Label(Long.toString(character.getXp())));

    heading(pane, "S.P.E.C.I.A.L.");
    for (Attribute attribute : Attribute.values()) {
      pane.add(new Label(attribute.displayName()));
      pane.add(baselineMarked(Integer.toString(character.attributeValue(attribute)), character.isLegacyImport()),
          "span 3");
    }

    heading(pane, "Skills");
    for (Skill skill : Skill.values()) {
      boolean tagged = character.skillState(skill).tagged();
      pane.add(new Label((tagged ? "● " : "") + skill.displayName()));
      pane.add(new Label("(" + skill.governingAttribute().abbreviation() + ")"));
      pane.add(baselineMarked(Integer.toString(character.skillRank(skill)), character.isLegacyImport()),
          "span 2");
    }
    pane.add(new Label("● = Tag skill"), "span 4");

    heading(pane, "Traits");
    if (character.getTraits().isEmpty()) {
      pane.add(new Label("None"), "span 4");
    }
    for (TraitSelection trait : character.getTraits()) {
      pane.add(importMarked(reference(trait.trait()), trait.imported()), "span 4");
    }

    heading(pane, "Perks");
    if (character.getPerks().isEmpty()) {
      pane.add(new Label("None"), "span 4");
    }
    for (PerkSelection perk : character.getPerks()) {
      String choices = perk.attributeChoices().isEmpty() ? ""
          : " (" + perk.attributeChoices().stream().map(Attribute::abbreviation)
              .collect(Collectors.joining(", ")) + ")";
      pane.add(importMarked(reference(perk.perk()), perk.importedRanks() > 0));
      pane.add(new Label("Rank " + perk.rank() + choices), "span 3");
    }

    heading(pane, "Advancement and session");
    Progression progression = character.getProgression();
    Vitals vitals = character.getVitals();
    pane.add(new Label("Unspent skill ranks"));
    pane.add(new Label(Integer.toString(progression.pendingSkillRanks())));
    pane.add(new Label("Unspent perks"));
    pane.add(new Label(Integer.toString(progression.pendingPerks())));
    pane.add(new Label("Current HP"));
    pane.add(new Label(vitals.currentHp() == null ? "maximum" : Integer.toString(vitals.currentHp())));
    pane.add(new Label("Rads"));
    pane.add(new Label(Integer.toString(vitals.rads())));
    pane.add(new Label("Radiation points"));
    pane.add(new Label(Integer.toString(vitals.radiationPoints())), "span 3");

    LegacyImport legacy = character.getLegacyImport();
    if (legacy != null) {
      heading(pane, "Legacy data (imported " + legacy.importedOn() + ")");
      pane.add(new Label("Weapons, armor values and origin options are kept for later features."), "span 4");
      TextArea equipment = new TextArea(legacy.payload().path("equipment").asText(""));
      equipment.setEditable(false);
      equipment.setPrefRowCount(8);
      pane.add(new Label("Equipment"), "top");
      pane.add(equipment, "span 3, growx");
    }
    return pane;
  }

  private static void heading(MigPane pane, String text) {
    Label label = new Label(text);
    label.setStyle("-fx-font-weight: bold; -fx-font-size: 1.15em;");
    pane.add(label, "span 4, gaptop 12, left");
  }

  private static Label reference(CatalogRef ref) {
    Label label = new Label(ref.displayName());
    if (ref.legacyName() != null) {
      label.setTooltip(new Tooltip(UNRESOLVED_TOOLTIP));
    }
    return label;
  }

  private static Label baselineMarked(String text, boolean legacy) {
    Label label = new Label(legacy ? text + "  (legacy)" : text);
    if (legacy) {
      label.setTooltip(new Tooltip(LEGACY_BASELINE_TOOLTIP));
    }
    return label;
  }

  private static Label importMarked(Label label, boolean imported) {
    if (imported) {
      label.setText(label.getText() + "  (imported)");
      label.setTooltip(new Tooltip(IMPORTED_TOOLTIP));
    }
    return label;
  }
}
