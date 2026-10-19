package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.creation.AttributeRules;
import javafx.scene.control.Label;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * S.P.E.C.I.A.L. of a character (CRB p. 58) as a four-column grid: name, value, − and +. During
 * creation and in GM mode the buttons change the allocation and the points left are shown above;
 * afterwards the values are read-only. Every change goes to the model and then to {@code onChanged},
 * which rebuilds the view, so the buttons always reflect the current budget.
 */
final class AttributeSection {

  private AttributeSection() {}

  /** @param locked whether the character is complete and not in GM mode (values read-only) */
  static @NonNull MigPane build(@NonNull PlayerCharacter character, boolean locked, @NonNull Runnable onChanged) {
    MigPane pane = new MigPane("insets 0, gap 8 4, wrap 4", "[left][left][left][left]");
    if (locked) {
      for (Attribute attribute : Attribute.values()) {
        pane.add(new Label(attribute.displayName()));
        pane.add(OverviewParts.baselineMarked(Integer.toString(character.attributeValue(attribute)),
            character.isLegacyImport()), "span 3");
      }
      return pane;
    }
    int left = AttributeRules.pointsLeft(character);
    // D2: GM corrections of a complete character have no budget.
    if (!character.isCreationComplete()) {
      pane.add(new Label("[Points left: " + left + "]"), "span 4");
    }
    for (Attribute attribute : Attribute.values()) {
      pane.add(new Label(attribute.displayName()));
      Label value = new Label(Integer.toString(character.attributeValue(attribute)));
      value.setMinWidth(StepButtons.NUMBER_WIDTH);
      pane.add(value);
      boolean canLower = AttributeRules.canLower(character, attribute);
      boolean canRaise = AttributeRules.canRaise(character, attribute);
      pane.add(StepButtons.minus(canLower ? "Lower " + attribute.displayName()
              : lowerBlocked(character, attribute),
          canLower, () -> step(character, attribute, -1, onChanged)));
      pane.add(StepButtons.plus(canRaise ? "Raise " + attribute.displayName() : raiseBlocked(character, attribute, left),
          canRaise, () -> step(character, attribute, 1, onChanged)));
    }
    return pane;
  }

  /** Raises (+1) or lowers (−1) the allocation, checked again against the current model state. */
  private static void step(@NonNull PlayerCharacter character, @NonNull Attribute attribute, int delta,
      @NonNull Runnable onChanged) {
    boolean allowed = delta > 0 ? AttributeRules.canRaise(character, attribute)
        : AttributeRules.canLower(character, attribute);
    if (allowed) {
      character.setAttributeAllocation(attribute, character.attributeAllocation(attribute) + delta);
    }
    onChanged.run();
  }

  private static @NonNull String lowerBlocked(@NonNull PlayerCharacter character, @NonNull Attribute attribute) {
    return character.isGmMode()
        ? attribute.displayName() + " is at the lowest value of " + AttributeRules.GM_MINIMUM_VALUE + "."
        : attribute.displayName() + " cannot go more than 1 below its starting value.";
  }

  private static @NonNull String raiseBlocked(@NonNull PlayerCharacter character, @NonNull Attribute attribute,
      int left) {
    if (left <= 0 && !character.isGmMode()) {
      return "No S.P.E.C.I.A.L. points left.";
    }
    return attribute.displayName() + " is at its maximum of "
        + AttributeRules.maximum(character, attribute) + ".";
  }
}
