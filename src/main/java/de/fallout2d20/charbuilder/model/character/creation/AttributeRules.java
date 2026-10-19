package de.fallout2d20.charbuilder.model.character.creation;

import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * S.P.E.C.I.A.L. allocation during creation (CRB p. 58): 5 points to raise attributes by 1 each; an
 * attribute may be lowered by 1 below its starting value for one more point (CRB p. 50, 58: from 5 to
 * 4; for the raised starting value 7 of Super Mutant and Nightkin STR/END only to 6, user decision).
 * All points must be spent (user decision). The attribute maximums are checked by
 * {@link OriginRules}. Soft checks only, for characters in creation.
 */
public final class AttributeRules {

  /** CRB p. 58: points to spend on attributes during creation. */
  public static final int SPECIAL_POINTS = 5;

  /** Lowest allocation: one rank below the starting value. */
  public static final int MIN_ALLOCATION = -1;

  /** Lowest attribute value in GM mode (D2): a technical floor, the book sets none for corrections. */
  public static final int GM_MINIMUM_VALUE = 1;

  private static final String TOPIC = "S.P.E.C.I.A.L.";

  private AttributeRules() {}

  /** Points still to spend; negative if too many are spent. Points gained by lowering count in. */
  public static int pointsLeft(@NonNull PlayerCharacter character) {
    int spent = 0;
    for (Attribute attribute : Attribute.values()) {
      spent += character.attributeAllocation(attribute);
    }
    return SPECIAL_POINTS - spent;
  }

  /** Highest value of the attribute: 10, or the origin's own maximum (e.g. CRB p. 53). */
  public static int maximum(@NonNull PlayerCharacter character, @NonNull Attribute attribute) {
    return character.getOriginData().map(data -> data.attributeMaximum(attribute))
        .orElse(OriginData.DEFAULT_ATTRIBUTE_MAXIMUM);
  }

  /**
   * Whether one more point may go into the attribute: points left (not in GM mode, D2) and below its
   * maximum.
   */
  public static boolean canRaise(@NonNull PlayerCharacter character, @NonNull Attribute attribute) {
    return (character.isGmMode() || pointsLeft(character) > 0)
        && character.attributeValue(attribute) < maximum(character, attribute);
  }

  /**
   * Whether the attribute may lose one point: not yet lowered below its starting value; in GM mode down to
   * {@link #GM_MINIMUM_VALUE}.
   */
  public static boolean canLower(@NonNull PlayerCharacter character, @NonNull Attribute attribute) {
    return character.isGmMode() ? character.attributeValue(attribute) > GM_MINIMUM_VALUE
        : character.attributeAllocation(attribute) > MIN_ALLOCATION;
  }

  /** All allocation problems of a character in creation; empty for complete characters and in GM mode (D2). */
  public static @NonNull List<CreationProblem> problems(@NonNull PlayerCharacter character) {
    List<CreationProblem> problems = new ArrayList<>();
    if (character.isCreationComplete() || character.isGmMode()) {
      return problems;
    }
    int left = pointsLeft(character);
    if (left > 0) {
      problems.add(new CreationProblem(HintArea.STATS, TOPIC, "Spend all S.P.E.C.I.A.L. points (" + left + " left)."));
    } else if (left < 0) {
      problems.add(new CreationProblem(HintArea.STATS, TOPIC,
          "Too many S.P.E.C.I.A.L. points spent (" + -left + " too many)."));
    }
    for (Attribute attribute : Attribute.values()) {
      if (character.attributeAllocation(attribute) < MIN_ALLOCATION) {
        problems.add(new CreationProblem(HintArea.STATS, attribute.displayName(), attribute.displayName()
            + " may be lowered by at most 1 below its starting value."));
      }
    }
    return problems;
  }
}
