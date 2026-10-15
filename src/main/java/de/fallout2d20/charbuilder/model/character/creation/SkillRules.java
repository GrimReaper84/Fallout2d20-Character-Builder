package de.fallout2d20.charbuilder.model.character.creation;

import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.catalog.trait.TraitCatalog;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Tag skills and skill points during creation (CRB p. 58): three free Tag skills, each starting at
 * rank 2; Educated adds one more (CRB p. 56). Tag skills of the origin (CRB p. 51, 52, 57; WOA p. 19;
 * WG p. 12) come on top and are checked by {@link OriginRules}. Skill points are 9 plus the
 * effective Intelligence, which counts Gifted and Intense Training taken during creation (user
 * decision); one point buys one rank. All free Tags and all skill points must be used (user
 * decision). No skill may exceed {@link Character#skillRankCap()}. Soft checks only, for characters
 * in creation. A higher starting level adds one skill point per level above 1 (CRB p. 49), Skilled 2
 * per rank (CRB p. 71).
 */
public final class SkillRules {

  /** CRB p. 58: free Tag skills of every character. */
  public static final int FREE_TAG_SKILLS = 3;

  /** CRB p. 58: skill points before Intelligence is added. */
  public static final int SKILL_POINTS_BASE = 9;

  private static final String TAGS = "Tag skills";
  private static final String SKILLS = "Skills";

  private SkillRules() {}

  /** Free Tag skills the character may choose: three plus the extra Tags of its traits (Educated). */
  public static int freeTagBudget(@NonNull Character character) {
    int budget = FREE_TAG_SKILLS;
    for (TraitSelection selection : character.getTraits()) {
      budget += TraitCatalog.byId(selection.trait().id()).map(TraitDefinition::extraTagSkills).orElse(0);
    }
    return budget;
  }

  /** Free Tags still to choose; negative if too many are chosen. Origin Tags do not count. */
  public static int freeTagsLeft(@NonNull Character character) {
    int chosen = 0;
    for (Skill skill : Skill.values()) {
      if (character.skillState(skill).tagged()) {
        chosen++;
      }
    }
    return freeTagBudget(character) - chosen;
  }

  /**
   * Skill points of the character: 9 + effective Intelligence (CRB p. 58), +1 per level above 1 (CRB
   * p. 49: one skill rank per level), +2 per rank of Skilled (CRB p. 71).
   */
  public static int skillPointBudget(@NonNull Character character) {
    return SKILL_POINTS_BASE + character.attributeValue(Attribute.INTELLIGENCE) + character.getLevel() - 1
        + character.perkEffect(PerkDefinition::skillPointsPerRank);
  }

  /** Skill points still to spend; negative if too many are spent. Free Tag ranks cost nothing. */
  public static int skillPointsLeft(@NonNull Character character) {
    int spent = 0;
    for (Skill skill : Skill.values()) {
      spent += character.skillState(skill).creationRanks();
    }
    return skillPointBudget(character) - spent;
  }

  /** Whether one more rank may be bought: points left and below the rank cap. */
  public static boolean canRaise(@NonNull Character character, @NonNull Skill skill) {
    return skillPointsLeft(character) > 0 && character.skillRank(skill) < character.skillRankCap();
  }

  /** Whether a bought rank can be taken back. */
  public static boolean canLower(@NonNull Character character, @NonNull Skill skill) {
    return character.skillState(skill).creationRanks() > 0;
  }

  /**
   * Whether the free Tag of the skill may be switched: a free Tag can always be removed; a new one
   * needs free Tags left, and the skill must be neither tagged by the origin or a perk nor forbidden by
   * a trait.
   */
  public static boolean canToggleTag(@NonNull Character character, @NonNull Skill skill) {
    if (character.skillState(skill).tagged()) {
      return true;
    }
    return !character.isTaggedByOrigin(skill) && !character.isTaggedByPerk(skill)
        && character.tagForbiddenBy(skill).isEmpty()
        && freeTagsLeft(character) > 0;
  }

  /** All Tag and skill point problems of a character in creation; empty for complete characters. */
  public static @NonNull List<CreationProblem> problems(@NonNull Character character) {
    List<CreationProblem> problems = new ArrayList<>();
    if (character.isCreationComplete()) {
      return problems;
    }
    int tagsLeft = freeTagsLeft(character);
    if (tagsLeft > 0) {
      problems.add(new CreationProblem(TAGS, "Choose " + tagsLeft + " more Tag skill"
          + (tagsLeft == 1 ? "." : "s.")));
    } else if (tagsLeft < 0) {
      problems.add(new CreationProblem(TAGS, "Too many Tag skills chosen (" + -tagsLeft + " too many)."));
    }
    int pointsLeft = skillPointsLeft(character);
    if (pointsLeft > 0) {
      problems.add(new CreationProblem(SKILLS, "Spend all skill points (" + pointsLeft + " left)."));
    } else if (pointsLeft < 0) {
      problems.add(new CreationProblem(SKILLS, "Too many skill points spent (" + -pointsLeft + " too many)."));
    }
    checkRankCap(character, problems);
    return problems;
  }

  /** CRB p. 58: rank 3 during creation below level 3, never above 6; origin cap 4 (CRB p. 53, WG p. 14). */
  private static void checkRankCap(@NonNull Character character, @NonNull List<CreationProblem> problems) {
    int cap = character.skillRankCap();
    Optional<String> limitingOrigin = character.getOriginData()
        .filter(origin -> Integer.valueOf(cap).equals(origin.skillRankCap()))
        .map(OriginData::name);
    String reason;
    if (limitingOrigin.isPresent()) {
      reason = " (" + limitingOrigin.get() + ").";
    } else if (cap == Character.CREATION_SKILL_RANK_CAP) {
      reason = " during creation.";
    } else {
      reason = ".";
    }
    for (Skill skill : Skill.values()) {
      if (character.skillRank(skill) > cap) {
        problems.add(new CreationProblem(skill.displayName(),
            skill.displayName() + " may not exceed rank " + cap + reason));
      }
    }
  }
}
