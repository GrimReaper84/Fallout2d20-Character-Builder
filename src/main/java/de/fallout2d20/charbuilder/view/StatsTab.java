package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.trait.TraitCatalog;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import de.fallout2d20.charbuilder.model.character.advancement.AdvancementRules;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Stats tab: origin trait and origin options, traits, S.P.E.C.I.A.L. and skills. Editable during
 * creation and in GM mode; afterwards read-only, except the skill ranks to spend after a level-up.
 */
final class StatsTab {

  private StatsTab() {}

  /** @param hints the hints of this tab */
  static @NonNull Node build(@NonNull Character character, @NonNull List<CreationProblem> hints,
      @NonNull ViewCallbacks callbacks) {
    MigPane pane = OverviewParts.grid();
    OverviewParts.addHints(pane, hints);
    boolean locked = character.isCreationComplete() && !character.isGmMode();
    Runnable onChanged = callbacks.onChanged();

    Optional<OriginData> origin = character.getOriginData();
    if (origin.isPresent()) {
      OverviewParts.heading(pane, origin.get().name());
      if (locked) {
        addOriginTrait(pane, origin.get());
        addOriginChoices(pane, character.getOriginChoices());
      } else {
        OriginSection.addOptions(pane, character, callbacks.onEdited(), onChanged);
      }
    }

    OverviewParts.heading(pane, "Traits");
    if (locked) {
      if (character.getTraits().isEmpty()) {
        pane.add(new Label("None"), "span 4");
      }
      for (TraitSelection trait : character.getTraits()) {
        pane.add(OverviewParts.importMarked(trait(trait), trait.imported()), "span 4");
      }
    } else {
      TraitSection.addTo(pane, character, onChanged);
      for (TraitSelection trait : character.getTraits()) {
        if (trait.imported() || !offeredByOrigin(trait, origin)) {
          pane.add(OverviewParts.importMarked(trait(trait), trait.imported()), "span 4");
        }
      }
    }

    OverviewParts.heading(pane, "S.P.E.C.I.A.L.");
    if (locked) {
      for (Attribute attribute : Attribute.values()) {
        pane.add(new Label(attribute.displayName()));
        pane.add(OverviewParts.baselineMarked(Integer.toString(character.attributeValue(attribute)),
            character.isLegacyImport()), "span 3");
      }
    } else {
      AttributeSection.addTo(pane, character, onChanged);
    }

    OverviewParts.heading(pane, "Skills");
    if (locked && AdvancementRules.choosesSkills(character)) {
      AdvancementSkillSection.addTo(pane, character, onChanged);
    } else if (locked) {
      for (Skill skill : Skill.values()) {
        boolean tagged = character.isTagged(skill);
        pane.add(new Label((tagged ? "● " : "") + skill.displayName()));
        pane.add(new Label("(" + skill.governingAttribute().abbreviation() + ")"));
        pane.add(OverviewParts.baselineMarked(Integer.toString(character.skillRank(skill)),
            character.isLegacyImport()), "span 2");
      }
      pane.add(new Label("● = Tag skill"), "span 4");
    } else {
      SkillSection.addTo(pane, character, onChanged);
    }
    return pane;
  }

  private static void addOriginTrait(@NonNull MigPane pane, @NonNull OriginData origin) {
    pane.add(new Label("Origin trait"));
    Label trait = new Label(origin.traitName() == null ? "Trait list" : origin.traitName());
    trait.setTooltip(new Tooltip(OriginSection.describe(origin)));
    pane.add(trait, "span 3");
  }

  /** Origin choices of a complete character, as text. */
  private static void addOriginChoices(@NonNull MigPane pane, @NonNull OriginChoices choices) {
    Skill tagSkill = choices.tagSkill();
    if (tagSkill != null) {
      pane.add(new Label("Extra Tag skill"));
      pane.add(new Label(tagSkill.displayName()), "span 3");
    }
    String purpose = choices.purpose();
    if (purpose != null) {
      pane.add(new Label("Purpose"));
      pane.add(new Label(purpose), "span 3");
    }
  }

  /**
   * Whether the trait has a checkbox in {@link TraitSection}: a catalogued trait of one of the origin's
   * trait lists. Other traits (imported, unresolved, left over after an origin change) are listed as text.
   */
  private static boolean offeredByOrigin(@NonNull TraitSelection selection, @NonNull Optional<OriginData> origin) {
    return origin.isPresent() && origin.get().traitLists().stream().anyMatch(list -> TraitCatalog.inList(list)
        .stream().anyMatch(trait -> trait.id().equals(selection.trait().id())));
  }

  /** Trait resolved from the catalog (name, tooltip, Gifted choices), else the plain reference. */
  private static @NonNull Label trait(@NonNull TraitSelection selection) {
    Optional<TraitDefinition> trait = TraitCatalog.byId(selection.trait().id());
    if (trait.isEmpty()) {
      return OverviewParts.reference(selection.trait());
    }
    String choices = selection.attributeChoices().isEmpty() ? ""
        : " (" + selection.attributeChoices().stream().map(Attribute::abbreviation)
            .collect(Collectors.joining(", ")) + ")";
    Label label = new Label(trait.get().name() + choices);
    label.setTooltip(new Tooltip(TraitSection.describe(trait.get())));
    return label;
  }
}
