package de.fallout2d20.charbuilder.model.character.creation;

import de.fallout2d20.charbuilder.model.character.Character;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * All hints for a character in creation: the name hint first (an app hint, not a book rule), then the
 * creation steps in book order (CRB p. 50): origin and traits, S.P.E.C.I.A.L., Tag skills and skill
 * ranks, perks, starting equipment. The one place that collects them, for the overview and the
 * confirmation before completing creation. Like the single rules classes, it returns nothing for a
 * complete character.
 */
public final class CreationRules {

  private static final String NAME = "Name";

  private CreationRules() {}

  /** All creation problems of a character in creation; empty for complete characters. */
  public static @NonNull List<CreationProblem> problems(@NonNull Character character) {
    List<CreationProblem> problems = new ArrayList<>();
    if (!isChecked(character)) {
      return problems;
    }
    if (character.getName().isBlank()) {
      problems.add(new CreationProblem(HintArea.HEADER, NAME, "Give the character a name."));
    }
    problems.addAll(OriginRules.problems(character));
    problems.addAll(TraitRules.problems(character));
    problems.addAll(AttributeRules.problems(character));
    problems.addAll(SkillRules.problems(character));
    problems.addAll(PerkRules.problems(character));
    problems.addAll(EquipmentRules.problems(character));
    return problems;
  }

  /**
   * Whether the creation rules look at the character: during creation, and in GM mode also afterwards
   * (D2: content hints such as requirements and open choices; budget hints are left out in GM mode).
   */
  public static boolean isChecked(@NonNull Character character) {
    return !character.isCreationComplete() || character.isGmMode();
  }
}
