package de.fallout2d20.charbuilder.model.character.creation;

import de.fallout2d20.charbuilder.catalog.origin.ExtraTagSkill;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Checks the rules an origin sets for character creation (CRB p. 51–58, WOA p. 18–25,
 * WG p. 10–15): attribute maximums, skill rank cap, the origin's extra Tag skill, the number of
 * traits and origin-specific choices. Only characters in creation are checked; the free Tag skills,
 * attribute budget and perk budget belong to other creation rules.
 */
public final class OriginRules {

  private static final String ORIGIN = "Origin";

  private OriginRules() {}

  /** All origin rule problems of a character in creation; empty for complete characters. */
  public static @NonNull List<CreationProblem> problems(@NonNull Character character) {
    List<CreationProblem> problems = new ArrayList<>();
    if (character.isCreationComplete()) {
      return problems;
    }
    CatalogRef origin = character.getOrigin();
    Optional<OriginData> data = character.getOriginData();
    if (origin == null) {
      problems.add(new CreationProblem(ORIGIN, "Choose an origin."));
      return problems;
    }
    if (data.isEmpty()) {
      problems.add(new CreationProblem(ORIGIN, "\"" + origin.displayName()
          + "\" is not an origin of the supported books."));
      return problems;
    }
    OriginData originData = data.get();
    if (!originData.isAvailableWith(character.getEnabledBooks())) {
      problems.add(new CreationProblem(ORIGIN, originData.name() + " needs a book that is not enabled."));
    }
    checkAttributes(character, originData, problems);
    checkSkillCap(character, originData, problems);
    checkExtraTagSkill(character, originData, problems);
    checkTraits(character, originData, problems);
    checkChoices(character.getOriginChoices(), originData, problems);
    return problems;
  }

  /**
   * Perks the origin grants on top of one perk per level: the origin's extra perks (Child of Atom,
   * WOA p. 25) plus one when an origin with trait lists takes only one trait (CRB p. 56, WG p. 15).
   */
  public static int extraPerks(@NonNull Character character) {
    Optional<OriginData> data = character.getOriginData();
    if (data.isEmpty()) {
      return 0;
    }
    boolean oneTraitAndPerk = !data.get().traitLists().isEmpty() && character.getTraits().size() == 1;
    return data.get().extraPerks() + (oneTraitAndPerk ? 1 : 0);
  }

  /** CRB p. 58: no attribute above 10, unless the origin changes the maximum (e.g. CRB p. 53). */
  private static void checkAttributes(@NonNull Character character, @NonNull OriginData origin,
      @NonNull List<CreationProblem> problems) {
    for (Attribute attribute : Attribute.values()) {
      int maximum = origin.attributeMaximum(attribute);
      if (character.attributeValue(attribute) > maximum) {
        problems.add(new CreationProblem(attribute.displayName(),
            attribute.displayName() + " may not exceed " + maximum + " (" + origin.name() + ")."));
      }
    }
  }

  /** Super Mutant (CRB p. 53) and Nightkin (WG p. 14): no skill above rank 4. */
  private static void checkSkillCap(@NonNull Character character, @NonNull OriginData origin,
      @NonNull List<CreationProblem> problems) {
    Integer cap = origin.skillRankCap();
    if (cap == null) {
      return;
    }
    for (Skill skill : Skill.values()) {
      if (character.skillRank(skill) > cap) {
        problems.add(new CreationProblem(skill.displayName(),
            skill.displayName() + " may not exceed rank " + cap + " (" + origin.name() + ")."));
      }
    }
  }

  /** The origin's extra Tag skill must be chosen, allowed and not tagged by another rule. */
  private static void checkExtraTagSkill(@NonNull Character character, @NonNull OriginData origin,
      @NonNull List<CreationProblem> problems) {
    ExtraTagSkill extraTag = origin.extraTagSkill();
    Skill chosen = character.getOriginChoices().tagSkill();
    if (extraTag == null) {
      return;
    }
    if (chosen == null) {
      problems.add(new CreationProblem(ORIGIN, "Choose the extra Tag skill granted by " + origin.name() + "."));
    } else if (!extraTag.allows(chosen)) {
      problems.add(new CreationProblem(ORIGIN, chosen.displayName()
          + " cannot be the extra Tag skill granted by " + origin.name() + "."));
    } else if (character.skillState(chosen).tagged() || chosen == origin.fixedTagSkill()) {
      problems.add(new CreationProblem(ORIGIN, chosen.displayName()
          + " is already a Tag skill; choose another extra Tag skill."));
    }
  }

  /**
   * Survivor (CRB p. 56) and Tribal (WG p. 15): two traits, or one trait and an extra perk. Whether
   * the traits come from the right list is checked with the trait catalog (feature B2).
   */
  private static void checkTraits(@NonNull Character character, @NonNull OriginData origin,
      @NonNull List<CreationProblem> problems) {
    int traits = character.getTraits().size();
    if (origin.traitLists().isEmpty()) {
      if (traits > 0) {
        problems.add(new CreationProblem(ORIGIN, origin.name() + " offers no traits."));
      }
    } else if (traits < 1 || traits > 2) {
      problems.add(new CreationProblem(ORIGIN,
          "Choose two traits, or one trait and an extra perk."));
    }
  }

  /** Choices only some origins have (Mister Handy pincer, CRB p. 55; Protectron purpose, WOA p. 21). */
  private static void checkChoices(@NonNull OriginChoices choices, @NonNull OriginData origin,
      @NonNull List<CreationProblem> problems) {
    if (!origin.purposeSuggestions().isEmpty() && choices.purpose() == null) {
      problems.add(new CreationProblem(ORIGIN, "Choose a purpose (" + origin.name() + ")."));
    }
    if (origin.purposeSuggestions().isEmpty() && choices.purpose() != null) {
      problems.add(new CreationProblem(ORIGIN, origin.name() + " has no purpose to choose."));
    }
    if (!origin.pincerOptional() && choices.noPincer()) {
      problems.add(new CreationProblem(ORIGIN, "Only a Mister Handy can go without a pincer."));
    }
    if (origin.extraTagSkill() == null && choices.tagSkill() != null) {
      problems.add(new CreationProblem(ORIGIN, origin.name() + " grants no extra Tag skill."));
    }
  }
}
