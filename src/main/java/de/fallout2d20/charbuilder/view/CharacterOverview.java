package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.origin.OriginCatalog;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.trait.TraitCatalog;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.LegacyImport;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import de.fallout2d20.charbuilder.model.character.Vitals;
import de.fallout2d20.charbuilder.model.character.creation.AttributeRules;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.model.character.creation.OriginRules;
import de.fallout2d20.charbuilder.model.character.creation.SkillRules;
import de.fallout2d20.charbuilder.model.character.creation.TraitRules;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import de.fallout2d20.charbuilder.model.character.derived.Resistance;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Overview of a character. Only the name and, during creation, origin, traits, S.P.E.C.I.A.L.
 * allocation and skills can be edited; after every change the overview is rebuilt by the caller, so
 * all values stay current.
 */
final class CharacterOverview {

  private static final String LEGACY_BASELINE_TOOLTIP =
      "Imported from a legacy file; used as the base value for further advancement.";
  private static final String IMPORTED_TOOLTIP =
      "Imported from a legacy file; its effect is already included in the imported values.";
  private static final String UNRESOLVED_TOOLTIP =
      "Not yet checked against the rulebooks (shown with its legacy name).";
  private static final String HINT_STYLE = "-fx-text-fill: #a35200;";
  private static final String RESISTANCE_TOOLTIP = "Innate resistance from origin and perks; armor is not included.";
  private static final String PHYSICAL_RESISTANCE_TOOLTIP =
      RESISTANCE_TOOLTIP + " A Barbarian bonus does not apply while wearing Power Armor.";

  private CharacterOverview() {}

  /**
   * @param onEdited called after an edit that needs no rebuild (typed text such as the name)
   * @param onChanged called after an edit that changes other values; the caller rebuilds the overview
   */
  static @NonNull Node build(@NonNull Character character, @NonNull Runnable onEdited,
      @NonNull Runnable onChanged) {
    MigPane pane = new MigPane("insets 16, gap 8 4, wrap 4", "[right][grow,fill][right][grow,fill]");

    TextField name = new TextField(character.getName());
    name.setPromptText("Name");
    name.textProperty().addListener((obs, old, value) -> {
      character.setName(value);
      onEdited.run();
    });
    pane.add(new Label("Name"));
    pane.add(name, "span 3");
    if (character.isCreationComplete()) {
      pane.add(new Label("Origin"));
      CatalogRef origin = character.getOrigin();
      pane.add(origin == null ? new Label("Not chosen") : origin(origin), "span 3");
      addOriginChoices(pane, character.getOriginChoices());
    } else {
      OriginSection.addTo(pane, character, onEdited, onChanged);
      TraitSection.addTo(pane, character, onChanged);
      List<CreationProblem> problems = new ArrayList<>(OriginRules.problems(character));
      problems.addAll(TraitRules.problems(character));
      problems.addAll(AttributeRules.problems(character));
      problems.addAll(SkillRules.problems(character));
      addHints(pane, problems);
    }
    pane.add(new Label("Level"));
    pane.add(new Label(Integer.toString(character.getLevel())));
    pane.add(new Label("XP"));
    pane.add(new Label(Long.toString(character.getXp())));
    pane.add(new Label("Books"));
    pane.add(new Label(character.getEnabledBooks().stream().map(Book::name)
        .collect(Collectors.joining(", "))), "span 3");

    heading(pane, "S.P.E.C.I.A.L.");
    if (character.isCreationComplete()) {
      for (Attribute attribute : Attribute.values()) {
        pane.add(new Label(attribute.displayName()));
        pane.add(baselineMarked(Integer.toString(character.attributeValue(attribute)), character.isLegacyImport()),
            "span 3");
      }
    } else {
      AttributeSection.addTo(pane, character, onChanged);
    }

    DerivedStats derived = DerivedStats.of(character);
    heading(pane, "Derived statistics");
    pane.add(new Label("Max HP"));
    pane.add(new Label(Integer.toString(derived.maxHp())));
    pane.add(new Label("Initiative"));
    pane.add(new Label(Integer.toString(derived.initiative())));
    pane.add(new Label("Defense"));
    pane.add(new Label(Integer.toString(derived.defense())));
    pane.add(new Label("Carry weight"));
    pane.add(new Label(derived.carryWeight() + " lbs."));
    pane.add(new Label("Melee damage"));
    pane.add(new Label(derived.meleeBonusDice() == 0 ? "+0" : "+" + derived.meleeBonusDice() + " CD"));
    pane.add(new Label("Luck points"));
    pane.add(new Label(Integer.toString(derived.maxLuckPoints())));
    resistance(pane, "Physical DR", derived.physical(), PHYSICAL_RESISTANCE_TOOLTIP);
    resistance(pane, "Energy DR", derived.energy(), RESISTANCE_TOOLTIP);
    resistance(pane, "Radiation DR", derived.radiation(), RESISTANCE_TOOLTIP);
    resistance(pane, "Poison DR", derived.poison(), RESISTANCE_TOOLTIP);

    heading(pane, "Skills");
    if (character.isCreationComplete()) {
      for (Skill skill : Skill.values()) {
        boolean tagged = character.isTagged(skill);
        pane.add(new Label((tagged ? "● " : "") + skill.displayName()));
        pane.add(new Label("(" + skill.governingAttribute().abbreviation() + ")"));
        pane.add(baselineMarked(Integer.toString(character.skillRank(skill)), character.isLegacyImport()),
            "span 2");
      }
      pane.add(new Label("● = Tag skill"), "span 4");
    } else {
      SkillSection.addTo(pane, character, onChanged);
    }

    heading(pane, "Traits");
    if (character.getTraits().isEmpty()) {
      pane.add(new Label("None"), "span 4");
    }
    for (TraitSelection trait : character.getTraits()) {
      pane.add(importMarked(trait(trait), trait.imported()), "span 4");
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
    int currentHp = vitals.currentHp() == null ? derived.maxHp() : vitals.currentHp();
    pane.add(new Label(currentHp + " / " + derived.maxHp()));
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

  private static void resistance(@NonNull MigPane pane, @NonNull String text,
      @NonNull Resistance resistance, @NonNull String tooltip) {
    Label label = new Label(text);
    label.setTooltip(new Tooltip(tooltip));
    pane.add(label);
    pane.add(new Label(resistance.toString()));
  }

  /** Rule hints of the creation rules; soft, they never block editing or saving. */
  private static void addHints(@NonNull MigPane pane, @NonNull List<CreationProblem> problems) {
    pane.add(new Label("Rule hints"), "top");
    if (problems.isEmpty()) {
      pane.add(new Label("No problems with the creation rules so far."), "span 3");
      return;
    }
    Label hints = new Label(problems.stream().map(CreationProblem::message)
        .collect(Collectors.joining("\n")));
    hints.setStyle(HINT_STYLE);
    hints.setWrapText(true);
    pane.add(hints, "span 3");
  }

  /** Trait resolved from the catalog (name, tooltip, Gifted choices), else the plain reference. */
  private static @NonNull Label trait(@NonNull TraitSelection selection) {
    Optional<TraitDefinition> trait = TraitCatalog.byId(selection.trait().id());
    if (trait.isEmpty()) {
      return reference(selection.trait());
    }
    String choices = selection.attributeChoices().isEmpty() ? ""
        : " (" + selection.attributeChoices().stream().map(Attribute::abbreviation)
            .collect(Collectors.joining(", ")) + ")";
    Label label = new Label(trait.get().name() + choices);
    label.setTooltip(new Tooltip(TraitSection.describe(trait.get())));
    return label;
  }

  /** Origin choices of a complete character, as text. */
  private static void addOriginChoices(@NonNull MigPane pane, @NonNull OriginChoices choices) {
    Skill tagSkill = choices.tagSkill();
    if (tagSkill != null) {
      pane.add(new Label("Extra Tag skill"));
      pane.add(new Label(tagSkill.displayName()), "span 3");
    }
    if (choices.noPincer()) {
      pane.add(new Label("Pincer"));
      pane.add(new Label("No pincer"), "span 3");
    }
    String purpose = choices.purpose();
    if (purpose != null) {
      pane.add(new Label("Purpose"));
      pane.add(new Label(purpose), "span 3");
    }
  }

  /** Origin resolved from the catalog (name, sources and trait summary), else the plain reference. */
  private static @NonNull Label origin(@NonNull CatalogRef ref) {
    Optional<OriginData> origin = OriginCatalog.byId(ref.id());
    if (origin.isEmpty()) {
      return reference(ref);
    }
    Label label = new Label(origin.get().name());
    label.setTooltip(new Tooltip(OriginSection.describe(origin.get())));
    return label;
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
