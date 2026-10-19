package de.fallout2d20.charbuilder.model.character.creation;

import de.fallout2d20.charbuilder.catalog.origin.ExtraTagSkill;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Checks the rules an origin sets for character creation (CRB p. 51–58, WOA p. 18–25,
 * WG p. 10–15): attribute maximums, the origin's extra Tag skill and origin-specific choices. Only
 * characters in creation are checked; traits are checked by {@link TraitRules}, the attribute budget
 * by {@link AttributeRules}, free Tag skills, skill points and all skill rank caps (including the
 * origin's) by {@link SkillRules}, the perk budget by later creation rules.
 */
public final class OriginRules {

  private static final String ORIGIN = "Origin";

  private OriginRules() {}

  /** All origin rule problems of a character in creation; empty for complete characters. */
  public static @NonNull List<CreationProblem> problems(@NonNull PlayerCharacter character) {
    List<CreationProblem> problems = new ArrayList<>();
    if (!CreationRules.isChecked(character)) {
      return problems;
    }
    CatalogRef origin = character.getOrigin();
    Optional<OriginData> data = character.getOriginData();
    if (origin == null) {
      problems.add(new CreationProblem(HintArea.HEADER, ORIGIN, "Choose an origin."));
      return problems;
    }
    if (data.isEmpty()) {
      problems.add(new CreationProblem(HintArea.HEADER, ORIGIN, "\"" + origin.displayName()
          + "\" is not an origin of the supported books."));
      return problems;
    }
    OriginData originData = data.get();
    if (!originData.isAvailableWith(character.getEnabledBooks())) {
      problems.add(new CreationProblem(HintArea.HEADER, ORIGIN,
          originData.name() + " needs a book that is not enabled."));
    }
    checkAttributes(character, originData, problems);
    checkExtraTagSkill(character, originData, problems);
    checkChoices(character.getOriginChoices(), originData, problems);
    return problems;
  }

  /**
   * Perks the origin grants on top of one perk per level: the origin's extra perks (Child of Atom,
   * WOA p. 25) plus one when an origin with trait lists takes only one trait (CRB p. 56, WG p. 15).
   */
  public static int extraPerks(@NonNull PlayerCharacter character) {
    Optional<OriginData> data = character.getOriginData();
    if (data.isEmpty()) {
      return 0;
    }
    return data.get().extraPerks() + (TraitRules.takesExtraPerkInsteadOfTrait(character) ? 1 : 0);
  }

  /** CRB p. 58: no attribute above 10, unless the origin changes the maximum (e.g. CRB p. 53). */
  private static void checkAttributes(@NonNull PlayerCharacter character, @NonNull OriginData origin,
      @NonNull List<CreationProblem> problems) {
    for (Attribute attribute : Attribute.values()) {
      int maximum = origin.attributeMaximum(attribute);
      if (character.abilities().attributeValue(attribute) > maximum) {
        problems.add(new CreationProblem(HintArea.STATS, attribute.displayName(),
            attribute.displayName() + " may not exceed " + maximum + " (" + origin.name() + ")."));
      }
    }
  }

  /** The origin's extra Tag skill must be chosen, allowed and not tagged by another rule. */
  private static void checkExtraTagSkill(@NonNull PlayerCharacter character, @NonNull OriginData origin,
      @NonNull List<CreationProblem> problems) {
    ExtraTagSkill extraTag = origin.extraTagSkill();
    Skill chosen = character.getOriginChoices().tagSkill();
    if (extraTag == null) {
      return;
    }
    if (chosen == null) {
      problems.add(new CreationProblem(HintArea.STATS, ORIGIN,
          "Choose the extra Tag skill granted by " + origin.name() + "."));
    } else if (!extraTag.allows(chosen)) {
      problems.add(new CreationProblem(HintArea.STATS, ORIGIN, chosen.displayName()
          + " cannot be the extra Tag skill granted by " + origin.name() + "."));
    } else if (character.abilities().skillState(chosen).tagged() || chosen == origin.fixedTagSkill()) {
      problems.add(new CreationProblem(HintArea.STATS, ORIGIN, chosen.displayName()
          + " is already a Tag skill; choose another extra Tag skill."));
    }
  }

  /**
   * Choices only some origins have: the Protectron's purpose (WOA p. 21) and an extra Tag skill (e.g.
   * Vault Kid, CRB p. 57).
   */
  private static void checkChoices(@NonNull OriginChoices choices, @NonNull OriginData origin,
      @NonNull List<CreationProblem> problems) {
    if (!origin.purposeSuggestions().isEmpty() && choices.purpose() == null) {
      problems.add(new CreationProblem(HintArea.STATS, ORIGIN, "Choose a purpose (" + origin.name() + ")."));
    }
    if (origin.purposeSuggestions().isEmpty() && choices.purpose() != null) {
      problems.add(new CreationProblem(HintArea.STATS, ORIGIN, origin.name() + " has no purpose to choose."));
    }
    if (origin.extraTagSkill() == null && choices.tagSkill() != null) {
      problems.add(new CreationProblem(HintArea.STATS, ORIGIN, origin.name() + " grants no extra Tag skill."));
    }
  }
}
