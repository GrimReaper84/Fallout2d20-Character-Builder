package de.fallout2d20.charbuilder.model.character.advancement;

import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.model.character.creation.PerkRules;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Spending the skill ranks and perks of level-ups after creation (CRB p. 49: one skill rank up to the
 * rank cap and one perk whose requirements are met per new level; Skilled adds two skill ranks per rank,
 * CRB p. 71). Choices are staged first and applied together (user decision); staging and the perk grid
 * use the same requirement checks as creation ({@link PerkRules}). Apply is blocked while problems
 * exist (user decision); unspent points are no problem, they stay pending.
 */
public final class AdvancementRules {

  private static final @NonNull String SKILLS = "Skills";
  private static final @NonNull String PERKS = "Perks";

  private AdvancementRules() {}

  /** Whether the character has points to spend or staged choices, i.e. advancement controls are needed. */
  public static boolean isActive(@NonNull Character character) {
    return choosesSkills(character) || choosesPerks(character);
  }

  /** Whether skill ranks are left to stage or staged, i.e. the skill −/+ buttons are needed. */
  public static boolean choosesSkills(@NonNull Character character) {
    return character.isCreationComplete() && (character.advancementSkillRanksLeft() > 0
        || Arrays.stream(Skill.values()).anyMatch(skill -> character.skillState(skill).stagedRanks() > 0));
  }

  /** Whether perks are left to stage or perk ranks staged, i.e. the perk grid is needed. */
  public static boolean choosesPerks(@NonNull Character character) {
    return character.isCreationComplete() && (character.getProgression().pendingPerks() > 0
        || character.getPerks().stream().anyMatch(perk -> perk.stagedRanks() > 0));
  }

  /** Whether a skill rank can be staged: ranks left and the skill below the rank cap. */
  public static boolean canRaise(@NonNull Character character, @NonNull Skill skill) {
    return character.isCreationComplete() && character.advancementSkillRanksLeft() > 0
        && character.skillRank(skill) < character.skillRankCap();
  }

  /** Whether a staged rank of the skill can be taken back. */
  public static boolean canLower(@NonNull Character character, @NonNull Skill skill) {
    return character.isCreationComplete() && character.skillState(skill).stagedRanks() > 0;
  }

  /**
   * Problems of the staged choices: too many skill ranks or perks staged (e.g. after taking back a
   * staged Skilled) and the perk checks of creation for perks with staged ranks, such as requirements no
   * longer met after taking back a staged Intense Training. Empty for characters in creation.
   */
  public static @NonNull List<CreationProblem> problems(@NonNull Character character) {
    List<CreationProblem> problems = new ArrayList<>();
    if (!character.isCreationComplete()) {
      return problems;
    }
    int ranksLeft = character.advancementSkillRanksLeft();
    if (ranksLeft < 0) {
      problems.add(new CreationProblem(SKILLS, "Too many skill ranks staged (" + -ranksLeft + " too many)."));
    }
    int perksLeft = character.advancementPerksLeft();
    if (perksLeft < 0) {
      problems.add(new CreationProblem(PERKS, "Too many perks staged (" + -perksLeft + " too many)."));
    }
    for (PerkSelection selection : character.getPerks()) {
      Optional<PerkDefinition> perk = PerkCatalog.byId(selection.perk().id());
      if (selection.stagedRanks() == 0 || perk.isEmpty()) {
        continue;
      }
      problems.addAll(PerkRules.selectionProblems(character, selection, perk.get()));
    }
    return problems;
  }

  /** Whether the staged choices can be applied: something is staged and nothing is wrong. */
  public static boolean canApply(@NonNull Character character) {
    return character.isCreationComplete() && character.hasStagedAdvancement() && problems(character).isEmpty();
  }
}
