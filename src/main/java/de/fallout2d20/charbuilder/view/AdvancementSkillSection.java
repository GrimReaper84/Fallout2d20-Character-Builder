package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.advancement.AdvancementRules;
import javafx.scene.control.Label;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Skills of a complete character with skill ranks to spend (CRB p. 49: one rank per level, up to the
 * rank cap): Tag marker, rank with the staged ranks in brackets, and −/+ buttons that stage a rank or
 * take a staged one back. Every change goes to the model and then to {@code onChanged}.
 */
final class AdvancementSkillSection {

  private AdvancementSkillSection() {}

  /** Adds the skill rows to a four-column overview grid. */
  static void addTo(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onChanged) {
    int left = character.advancementSkillRanksLeft();
    for (Skill skill : Skill.values()) {
      int staged = character.skillState(skill).stagedRanks();
      pane.add(new Label((character.isTagged(skill) ? "● " : "") + skill.displayName()));
      pane.add(new Label("(" + skill.governingAttribute().abbreviation() + ")"));
      pane.add(new Label(character.skillRank(skill) + (staged > 0 ? " (+" + staged + " staged)" : "")));
      boolean canLower = AdvancementRules.canLower(character, skill);
      boolean canRaise = AdvancementRules.canRaise(character, skill);
      pane.add(StepButtons.of(
          StepButtons.minus(canLower ? "Take back a staged rank of " + skill.displayName()
                  : "No staged ranks of " + skill.displayName() + " to take back.",
              canLower, () -> step(character, skill, -1, onChanged)),
          StepButtons.plus(canRaise ? "Stage a rank of " + skill.displayName()
                  : left <= 0 ? "No skill ranks left."
                  : skill.displayName() + " is at its maximum rank of " + character.skillRankCap() + ".",
              canRaise, () -> step(character, skill, 1, onChanged))));
    }
    pane.add(new Label("● = Tag skill"), "span 4");
  }

  /** Stages (+1) or takes back (−1) a rank after checking the current model state again. */
  private static void step(@NonNull Character character, @NonNull Skill skill, int delta,
      @NonNull Runnable onChanged) {
    boolean allowed = delta > 0 ? AdvancementRules.canRaise(character, skill)
        : AdvancementRules.canLower(character, skill);
    if (allowed) {
      character.stageSkillRank(skill, delta);
    }
    onChanged.run();
  }
}
