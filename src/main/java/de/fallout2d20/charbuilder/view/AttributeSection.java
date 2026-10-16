package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.creation.AttributeRules;
import javafx.scene.control.Label;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * S.P.E.C.I.A.L. allocation of a character in creation (CRB p. 58): value and −/+ buttons per
 * attribute, and the points left. Every change goes to the model and then to {@code onChanged},
 * which rebuilds the overview, so the buttons always reflect the current budget.
 */
final class AttributeSection {

  private AttributeSection() {}

  /** Adds the attribute rows to a four-column overview grid. */
  static void addTo(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onChanged) {
    int left = AttributeRules.pointsLeft(character);
    pane.add(new Label("Points left"));
    pane.add(new Label(Integer.toString(left)), "span 3");
    for (Attribute attribute : Attribute.values()) {
      pane.add(new Label(attribute.displayName()));
      boolean canLower = AttributeRules.canLower(character, attribute);
      boolean canRaise = AttributeRules.canRaise(character, attribute);
      pane.add(StepButtons.withValue(new Label(Integer.toString(character.attributeValue(attribute))),
          StepButtons.NUMBER_WIDTH, StepButtons.of(
          StepButtons.minus(canLower ? "Lower " + attribute.displayName()
                  : attribute.displayName() + " cannot go more than 1 below its starting value.",
              canLower, () -> step(character, attribute, -1, onChanged)),
          StepButtons.plus(canRaise ? "Raise " + attribute.displayName() : raiseBlocked(character, attribute, left),
              canRaise, () -> step(character, attribute, 1, onChanged)))),
          "span 3");
    }
  }

  /** Raises (+1) or lowers (−1) the allocation, checked again against the current model state. */
  private static void step(@NonNull Character character, @NonNull Attribute attribute, int delta,
      @NonNull Runnable onChanged) {
    boolean allowed = delta > 0 ? AttributeRules.canRaise(character, attribute)
        : AttributeRules.canLower(character, attribute);
    if (allowed) {
      character.setAttributeAllocation(attribute, character.attributeAllocation(attribute) + delta);
    }
    onChanged.run();
  }

  private static @NonNull String raiseBlocked(@NonNull Character character, @NonNull Attribute attribute,
      int left) {
    if (left <= 0) {
      return "No S.P.E.C.I.A.L. points left.";
    }
    return attribute.displayName() + " is at its maximum of "
        + AttributeRules.maximum(character, attribute) + ".";
  }
}
