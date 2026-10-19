package de.fallout2d20.charbuilder.model.character.creation;

import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.catalog.trait.TraitCatalog;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
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
 * decision). No skill may exceed {@link PlayerCharacter#skillRankCap()}. Soft checks only, for characters
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
  public static int freeTagBudget(@NonNull PlayerCharacter character) {
    int budget = FREE_TAG_SKILLS;
    for (TraitSelection selection : character.getTraits()) {
      budget += TraitCatalog.byId(selection.trait().id()).map(TraitDefinition::extraTagSkills).orElse(0);
    }
    return budget;
  }

  /** Free Tags still to choose; negative if too many are chosen. Origin Tags do not count. */
  public static int freeTagsLeft(@NonNull PlayerCharacter character) {
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
  public static int skillPointBudget(@NonNull PlayerCharacter character) {
    return SKILL_POINTS_BASE + character.attributeValue(Attribute.INTELLIGENCE) + character.getLevel() - 1
        + character.perkEffect(PerkDefinition::skillPointsPerRank);
  }

  /** Skill points still to spend; negative if too many are spent. Free Tag ranks cost nothing. */
  public static int skillPointsLeft(@NonNull PlayerCharacter character) {
    int spent = 0;
    for (Skill skill : Skill.values()) {
      spent += character.skillState(skill).creationRanks();
    }
    return skillPointBudget(character) - spent;
  }

  /** Whether one more rank may be bought: points left (not in GM mode, D2) and below the rank cap. */
  public static boolean canRaise(@NonNull PlayerCharacter character, @NonNull Skill skill) {
    if (gmCorrects(character)) {
      return character.skillRank(skill) < character.skillRankCap() && character.gmStepChangesRank(skill, 1);
    }
    return (character.isGmMode() || skillPointsLeft(character) > 0)
        && character.skillRank(skill) < character.skillRankCap();
  }

  /**
   * Whether a rank can be taken back: a bought one during creation; for a complete character in GM mode a
   * step that really lowers the shown rank (a Tag! bonus can hide it).
   */
  public static boolean canLower(@NonNull PlayerCharacter character, @NonNull Skill skill) {
    return gmCorrects(character) ? character.gmStepChangesRank(skill, -1)
        : character.skillState(skill).creationRanks() > 0;
  }

  /**
   * Raises (+1) or lowers (−1) a rank if allowed: bought ranks during creation, a GM correction for a
   * complete character in GM mode (D2: counted at once).
   */
  public static void step(@NonNull PlayerCharacter character, @NonNull Skill skill, int delta) {
    boolean allowed = delta > 0 ? canRaise(character, skill) : canLower(character, skill);
    if (!allowed) {
      return;
    }
    if (gmCorrects(character)) {
      character.adjustSkillRankAsGm(skill, delta);
    } else {
      character.setCreationRanks(skill, character.skillState(skill).creationRanks() + delta);
    }
  }

  /** After creation the GM corrects ranks directly; during creation ranks are bought. */
  private static boolean gmCorrects(@NonNull PlayerCharacter character) {
    return character.isGmMode() && character.isCreationComplete();
  }

  /**
   * Whether the free Tag of the skill may be switched: a free Tag can always be removed; a new one
   * needs free Tags left, and the skill must be neither tagged by the origin or a perk nor forbidden by
   * a trait.
   */
  public static boolean canToggleTag(@NonNull PlayerCharacter character, @NonNull Skill skill) {
    if (character.skillState(skill).tagged()) {
      return true;
    }
    return !character.isTaggedByOrigin(skill) && !character.isTaggedByPerk(skill)
        && character.tagForbiddenBy(skill).isEmpty()
        && (character.isGmMode() || freeTagsLeft(character) > 0);
  }

  /** All Tag and skill point problems of a character in creation; empty for complete characters. */
  public static @NonNull List<CreationProblem> problems(@NonNull PlayerCharacter character) {
    List<CreationProblem> problems = new ArrayList<>();
    if (!CreationRules.isChecked(character)) {
      return problems;
    }
    if (character.isGmMode()) {
      checkRankCap(character, problems);
      return problems;
    }
    int tagsLeft = freeTagsLeft(character);
    if (tagsLeft > 0) {
      problems.add(new CreationProblem(HintArea.STATS, TAGS, "Choose " + tagsLeft + " more Tag skill"
          + (tagsLeft == 1 ? "." : "s.")));
    } else if (tagsLeft < 0) {
      problems.add(new CreationProblem(HintArea.STATS, TAGS,
          "Too many Tag skills chosen (" + -tagsLeft + " too many)."));
    }
    int pointsLeft = skillPointsLeft(character);
    if (pointsLeft > 0) {
      problems.add(new CreationProblem(HintArea.STATS, SKILLS, "Spend all skill points (" + pointsLeft + " left)."));
    } else if (pointsLeft < 0) {
      problems.add(new CreationProblem(HintArea.STATS, SKILLS,
          "Too many skill points spent (" + -pointsLeft + " too many)."));
    }
    checkRankCap(character, problems);
    return problems;
  }

  /** CRB p. 58: rank 3 during creation below level 3, never above 6; origin cap 4 (CRB p. 53, WG p. 14). */
  private static void checkRankCap(@NonNull PlayerCharacter character, @NonNull List<CreationProblem> problems) {
    int cap = character.skillRankCap();
    Optional<String> limitingOrigin = character.getOriginData()
        .filter(origin -> Integer.valueOf(cap).equals(origin.skillRankCap()))
        .map(OriginData::name);
    String reason;
    if (limitingOrigin.isPresent()) {
      reason = " (" + limitingOrigin.get() + ").";
    } else if (cap == PlayerCharacter.CREATION_SKILL_RANK_CAP) {
      reason = " during creation.";
    } else {
      reason = ".";
    }
    for (Skill skill : Skill.values()) {
      if (character.skillRank(skill) > cap) {
        problems.add(new CreationProblem(HintArea.STATS, skill.displayName(),
            skill.displayName() + " may not exceed rank " + cap + reason));
      }
    }
  }
}
