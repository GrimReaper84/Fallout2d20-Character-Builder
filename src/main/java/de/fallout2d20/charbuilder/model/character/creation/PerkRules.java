package de.fallout2d20.charbuilder.model.character.creation;

import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkChoice;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.catalog.perk.PerkRequirements;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * Perk choice during creation. A character chooses one perk rank per level plus the extra perks of
 * its origin or trait choice (CRB p. 49, 59; {@link OriginRules#extraPerks}); all of them must be
 * chosen (user decision). A rank can only be taken if the character meets its requirements (CRB
 * p. 59–73): attribute minimums against the effective values, which include Gifted and Intense
 * Training taken in the same creation (user decision); the level for that rank (base level, 1 if
 * none, plus the level step per rank taken, CRB p. 59); no robot where excluded; no perk it excludes
 * (CRB p. 62); not above its ranks. Perks that become invalid later stay and get a hint (user
 * decision). Soft checks only, for characters in creation.
 *
 * <p>After creation the perk grid and these checks are reused for advancement (CRB p. 49: one perk
 * per new level): the budget is then the pending perks, and only staged ranks can be given back. The
 * advancement problems are collected by {@code advancement.AdvancementRules}.
 */
public final class PerkRules {

  /** CRB p. 66: Intense Training cannot raise an attribute above 10. */
  public static final int INTENSE_TRAINING_MAXIMUM = 10;

  private static final @NonNull String TOPIC = "Perks";

  private PerkRules() {}

  /**
   * Perk ranks to choose: in creation one per level plus the extra perks of origin and traits; after
   * creation the pending perks of level-ups.
   */
  public static int budget(@NonNull Character character) {
    if (character.isCreationComplete()) {
      return character.getProgression().pendingPerks();
    }
    return character.getLevel() + OriginRules.extraPerks(character);
  }

  /** Perk ranks still to choose (after creation: to stage); negative if too many are chosen. */
  public static int perksLeft(@NonNull Character character) {
    if (character.isCreationComplete()) {
      return character.advancementPerksLeft();
    }
    return budget(character) - character.getPerks().stream().mapToInt(PerkSelection::rank).sum();
  }

  /** Ranks of the perk the character has. */
  public static int rank(@NonNull Character character, @NonNull String perkId) {
    return character.getPerks().stream().filter(perk -> perk.perk().id().equals(perkId))
        .mapToInt(PerkSelection::rank).sum();
  }

  /**
   * Whether the next rank of the perk can be taken: perk ranks left and nothing blocks it. In GM mode
   * (D2) no budget applies and unmet requirements are only hints; the maximum rank and a choice perk
   * without options still block.
   */
  public static boolean canAdd(@NonNull Character character, @NonNull PerkDefinition perk) {
    if (character.isGmMode()) {
      return structuralBlockers(character, perk).isEmpty();
    }
    return perksLeft(character) > 0 && blockers(character, perk).isEmpty();
  }

  /**
   * Whether a rank of the perk can be given back: in creation a rank taken in this creation, after
   * creation a staged rank; in GM mode any rank, imported ones too (D2).
   */
  public static boolean canRemove(@NonNull Character character, @NonNull String perkId) {
    return character.getPerks().stream().anyMatch(perk -> perk.perk().id().equals(perkId)
        && (character.isGmMode()
            || (character.isCreationComplete() ? perk.stagedRanks() > 0 : perk.rank() > perk.importedRanks())));
  }

  /**
   * Why the next rank of the perk cannot be taken (budget aside): maximum rank, unmet requirements for
   * that rank, or nothing left to choose for a choice perk. Empty if it can be taken.
   */
  public static @NonNull List<String> blockers(@NonNull Character character, @NonNull PerkDefinition perk) {
    int next = rank(character, perk.id()) + 1;
    if (next > perk.ranks()) {
      return List.of("Maximum rank reached.");
    }
    List<String> blockers = new ArrayList<>(unmetRequirements(character, perk, next));
    blockers.addAll(structuralBlockers(character, perk));
    return blockers;
  }

  /**
   * What blocks the next rank even in GM mode (D2): the maximum rank, or nothing left to choose for a
   * choice perk. Requirements are not part of it.
   */
  private static @NonNull List<String> structuralBlockers(@NonNull Character character,
      @NonNull PerkDefinition perk) {
    if (rank(character, perk.id()) + 1 > perk.ranks()) {
      return List.of("Maximum rank reached.");
    }
    List<String> blockers = new ArrayList<>();
    if (perk.choice() == PerkChoice.ATTRIBUTE && attributeOptions(character).isEmpty()) {
      blockers.add("No attribute below " + INTENSE_TRAINING_MAXIMUM + " left.");
    }
    if (perk.choice() == PerkChoice.SKILL && skillOptions(character).isEmpty()) {
      blockers.add("No skill left to tag.");
    }
    return blockers;
  }

  /** Attributes Intense Training may raise: those below 10 (CRB p. 66). */
  public static @NonNull List<Attribute> attributeOptions(@NonNull Character character) {
    return Arrays.stream(Attribute.values())
        .filter(attribute -> character.attributeValue(attribute) < INTENSE_TRAINING_MAXIMUM).toList();
  }

  /** Skills Tag! may tag: not tagged yet and not forbidden by a trait (CRB p. 73, WG p. 15). */
  public static @NonNull List<Skill> skillOptions(@NonNull Character character) {
    return Arrays.stream(Skill.values())
        .filter(skill -> !character.isTagged(skill) && character.tagForbiddenBy(skill).isEmpty()).toList();
  }

  /**
   * The requirements of the given rank in display order, each with whether the character meets it:
   * attribute minimums ("STR 7"), the level for that rank if above 1 ("level 10"), the robot exclusion
   * ("no robot") and excluded perks ("not with Daring Nature"). The one place requirements are listed,
   * for the rules and for the perk grid.
   */
  public static @NonNull Map<String, Boolean> requirementStatus(@NonNull Character character,
      @NonNull PerkDefinition perk, int rank) {
    PerkRequirements requirements = perk.requirements();
    Map<String, Boolean> status = new LinkedHashMap<>();
    for (Attribute attribute : Attribute.values()) {
      Integer minimum = requirements.attributes().get(attribute);
      if (minimum != null) {
        status.put(attribute.abbreviation() + " " + minimum, character.attributeValue(attribute) >= minimum);
      }
    }
    int level = requirements.levelForRank(rank);
    if (level > 1) {
      status.put("level " + level, character.getLevel() >= level);
    }
    if (requirements.notRobot()) {
      status.put("no robot", !character.getOriginData().map(OriginData::robot).orElse(false));
    }
    for (String excluded : requirements.excludedPerks()) {
      status.put("not with " + PerkCatalog.byId(excluded).map(PerkDefinition::name).orElse(excluded),
          rank(character, excluded) == 0);
    }
    return status;
  }

  /** The requirements of the given rank the character does not meet, e.g. "Requirement not met: STR 7.". */
  public static @NonNull List<String> unmetRequirements(@NonNull Character character,
      @NonNull PerkDefinition perk, int rank) {
    return requirementStatus(character, perk, rank).entrySet().stream()
        .filter(entry -> !entry.getValue())
        .map(entry -> "Requirement not met: " + entry.getKey() + ".")
        .toList();
  }

  /** Unmet requirements of the highest rank the character has taken; empty without a rank. */
  public static @NonNull List<String> unmetForTakenRank(@NonNull Character character,
      @NonNull PerkDefinition perk) {
    int rank = rank(character, perk.id());
    return rank == 0 ? List.of() : unmetRequirements(character, perk, Math.min(rank, perk.ranks()));
  }

  /**
   * All perk problems of a character in creation (or in GM mode, without the budget); empty for complete
   * characters.
   */
  public static @NonNull List<CreationProblem> problems(@NonNull Character character) {
    List<CreationProblem> problems = new ArrayList<>();
    if (!CreationRules.isChecked(character)) {
      return problems;
    }
    int left = character.isGmMode() ? 0 : perksLeft(character);
    if (left > 0) {
      problems.add(new CreationProblem(TOPIC, "Choose " + left + " more perk" + (left == 1 ? "." : "s.")));
    } else if (left < 0) {
      problems.add(new CreationProblem(TOPIC, "Too many perks chosen (" + -left + " too many)."));
    }
    for (PerkSelection selection : character.getPerks()) {
      Optional<PerkDefinition> perk = PerkCatalog.byId(selection.perk().id());
      if (perk.isEmpty()) {
        problems.add(new CreationProblem(TOPIC,
            "\"" + selection.perk().displayName() + "\" is not a perk of the supported books."));
        continue;
      }
      problems.addAll(selectionProblems(character, selection, perk.get()));
    }
    return problems;
  }

  /**
   * Problems of one perk selection: requirements of the highest rank taken, maximum rank and the choices
   * of choice perks. Used for creation and for staged perks after creation; imported ranks may lack
   * their choices.
   */
  public static @NonNull List<CreationProblem> selectionProblems(@NonNull Character character,
      @NonNull PerkSelection selection, @NonNull PerkDefinition perk) {
    List<CreationProblem> problems = new ArrayList<>();
    String name = perk.name();
    if (selection.rank() > perk.ranks()) {
      problems.add(new CreationProblem(name, name + " has at most " + perk.ranks() + " rank(s)."));
    }
    for (String unmet : unmetForTakenRank(character, perk)) {
      problems.add(new CreationProblem(name, name + ": " + unmet));
    }
    if (perk.choice() == PerkChoice.ATTRIBUTE) {
      if (!choicesComplete(selection, selection.attributeChoices())) {
        problems.add(new CreationProblem(name, "Choose an attribute for every rank of " + name + "."));
      }
      Set<Attribute> chosen = EnumSet.noneOf(Attribute.class);
      chosen.addAll(selection.attributeChoices());
      for (Attribute attribute : chosen) {
        if (character.attributeValue(attribute) > INTENSE_TRAINING_MAXIMUM) {
          problems.add(new CreationProblem(name, name + " cannot raise " + attribute.displayName()
              + " above " + INTENSE_TRAINING_MAXIMUM + "."));
        }
      }
    }
    if (perk.choice() == PerkChoice.SKILL) {
      if (!choicesComplete(selection, selection.skillChoices())) {
        problems.add(new CreationProblem(name, "Choose a skill for every rank of " + name + "."));
      }
      checkTaggedSkills(character, selection, name, problems);
    }
    return problems;
  }

  /** One choice per rank; imported ranks may lack theirs. */
  private static boolean choicesComplete(@NonNull PerkSelection selection, @NonNull List<?> choices) {
    return choices.size() >= selection.rank() - selection.importedRanks() && choices.size() <= selection.rank();
  }

  /**
   * A Tag! skill that the origin tags anyway wastes the perk, and one a trait forbids breaks that trait
   * (WG p. 15); both can arise when origin or traits change after the choice (perk stays, user decision).
   */
  private static void checkTaggedSkills(@NonNull Character character, @NonNull PerkSelection selection,
      @NonNull String name, @NonNull List<CreationProblem> problems) {
    for (Skill skill : selection.skillChoices()) {
      if (character.isTaggedByOrigin(skill)) {
        problems.add(new CreationProblem(name, name + ": " + skill.displayName()
            + " is already a Tag skill of the origin."));
      }
      Optional<TraitDefinition> forbidding = character.tagForbiddenBy(skill);
      if (forbidding.isPresent()) {
        problems.add(new CreationProblem(name, name + ": " + skill.displayName()
            + " cannot be a Tag skill with " + forbidding.get().name() + "."));
      }
    }
  }
}
