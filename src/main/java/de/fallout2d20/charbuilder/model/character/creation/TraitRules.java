package de.fallout2d20.charbuilder.model.character.creation;

import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.trait.TraitCatalog;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * Checks the traits of a character in creation: Survivor (CRB p. 56) and Tribal (WG p. 15) take two
 * traits from one of their lists, or one trait and an extra perk; no trait twice; Gifted needs two
 * different attributes (CRB p. 56); Nomad forbids Science as a Tag skill (WG p. 15). Soft checks
 * only. Characters without a catalogued origin are left to {@link OriginRules}.
 */
public final class TraitRules {

  /** CRB p. 56, WG p. 15: at most two traits. */
  public static final int MAX_TRAITS = 2;

  private static final String TRAITS = "Traits";

  private TraitRules() {}

  /** All trait problems of a character in creation; empty for complete characters. */
  public static @NonNull List<CreationProblem> problems(@NonNull Character character) {
    List<CreationProblem> problems = new ArrayList<>();
    Optional<OriginData> origin = character.getOriginData();
    if (!CreationRules.isChecked(character) || origin.isEmpty()) {
      return problems;
    }
    List<TraitSelection> traits = character.getTraits();
    checkCount(traits.size(), origin.get(), problems);
    Set<String> seen = new HashSet<>();
    Set<String> lists = new HashSet<>();
    for (TraitSelection selection : traits) {
      Optional<TraitDefinition> trait = TraitCatalog.byId(selection.trait().id());
      String name = trait.map(TraitDefinition::name).orElse(selection.trait().displayName());
      if (!seen.add(selection.trait().id())) {
        problems.add(new CreationProblem(HintArea.STATS, TRAITS, name + " is chosen twice."));
      }
      if (trait.isEmpty()) {
        problems.add(new CreationProblem(HintArea.STATS, TRAITS, name + " is not a trait of the supported books."));
        continue;
      }
      lists.add(trait.get().list());
      List<String> allowedLists = origin.get().traitLists();
      if (!allowedLists.isEmpty() && !allowedLists.contains(trait.get().list())) {
        problems.add(new CreationProblem(HintArea.STATS, TRAITS,
            name + " is not on the trait lists of " + origin.get().name() + "."));
      }
      checkAttributeChoices(selection, trait.get(), problems);
      checkForbiddenTags(character, trait.get(), problems);
    }
    if (lists.size() > 1) {
      problems.add(new CreationProblem(HintArea.STATS, TRAITS, "Both traits must come from the same list."));
    }
    return problems;
  }

  /**
   * Whether the character takes one trait and an extra perk instead of a second trait (CRB p. 56,
   * WG p. 15): an origin with trait lists and exactly one trait chosen.
   */
  public static boolean takesExtraPerkInsteadOfTrait(@NonNull Character character) {
    return character.getOriginData().map(origin -> !origin.traitLists().isEmpty()).orElse(false)
        && character.getTraits().size() == 1;
  }

  /** Two traits, or one trait and an extra perk; none for origins without trait lists. */
  private static void checkCount(int traits, @NonNull OriginData origin,
      @NonNull List<CreationProblem> problems) {
    if (origin.traitLists().isEmpty()) {
      if (traits > 0) {
        problems.add(new CreationProblem(HintArea.STATS, TRAITS, origin.name() + " offers no traits."));
      }
    } else if (traits < 1 || traits > MAX_TRAITS) {
      problems.add(new CreationProblem(HintArea.STATS, TRAITS, "Choose two traits, or one trait and an extra perk."));
    }
  }

  /** Gifted (CRB p. 56): exactly that many different attributes. Imported traits have no choices. */
  private static void checkAttributeChoices(@NonNull TraitSelection selection,
      @NonNull TraitDefinition trait, @NonNull List<CreationProblem> problems) {
    int required = trait.attributeChoices();
    List<Attribute> choices = selection.attributeChoices();
    if (selection.choosesAttributes()
        && (choices.size() != required || Set.copyOf(choices).size() != required)) {
      problems.add(new CreationProblem(HintArea.STATS, TRAITS,
          "Choose " + required + " different attributes for " + trait.name() + "."));
    }
  }

  /** Nomad (WG p. 15): Science cannot be a Tag skill, whatever made it one. */
  private static void checkForbiddenTags(@NonNull Character character, @NonNull TraitDefinition trait,
      @NonNull List<CreationProblem> problems) {
    for (Skill skill : trait.forbiddenTagSkills()) {
      if (character.isTagged(skill)) {
        problems.add(new CreationProblem(HintArea.STATS, TRAITS,
            skill.displayName() + " cannot be a Tag skill with " + trait.name() + "."));
      }
    }
  }
}
