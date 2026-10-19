package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.advancement.AdvancementRules;
import javafx.scene.control.Label;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Skills of a complete character with skill ranks to spend (CRB p. 49: one rank per level, up to the
 * rank cap), in the six-column grid of {@link SkillSection}: Tag marker, skill, attribute, rank with the
 * staged ranks in brackets, and −/+ buttons that stage a rank or take a staged one back. Every change goes
 * to the model and then to {@code onChanged}.
 */
final class AdvancementSkillSection {

  private AdvancementSkillSection() {}

  static @NonNull MigPane build(@NonNull PlayerCharacter character, @NonNull Runnable onChanged) {
    MigPane pane = SkillSection.grid();
    int left = character.advancement().skillRanksLeft();
    pane.add(new Label("[Ranks left: " + left + "]"), "span 6");
    for (Skill skill : Skill.values()) {
      int staged = character.abilities().skillState(skill).stagedRanks();
      pane.add(new Label(character.abilities().isTagged(skill) ? SkillSection.TAG_MARKER : ""));
      SkillSection.addNameCells(pane, skill);
      // Wide enough for "3 (+1)", so the buttons stay put while ranks are staged.
      pane.add(SkillSection.rank(character.abilities().skillRank(skill) + (staged > 0 ? " (+" + staged + ")" : ""),
          StepButtons.FRACTION_WIDTH));
      boolean canLower = AdvancementRules.canLower(character, skill);
      boolean canRaise = AdvancementRules.canRaise(character, skill);
      pane.add(StepButtons.minus(canLower ? "Take back a staged rank of " + skill.displayName()
              : "No staged ranks of " + skill.displayName() + " to take back.",
          canLower, () -> step(character, skill, -1, onChanged)));
      pane.add(StepButtons.plus(canRaise ? "Stage a rank of " + skill.displayName()
              : left <= 0 ? "No skill ranks left."
              : skill.displayName() + " is at its maximum rank of " + character.abilities().skillRankCap() + ".",
          canRaise, () -> step(character, skill, 1, onChanged)));
    }
    pane.add(new Label(SkillSection.TAG_LEGEND), "span 6");
    return pane;
  }

  /** Stages (+1) or takes back (−1) a rank after checking the current model state again. */
  private static void step(@NonNull PlayerCharacter character, @NonNull Skill skill, int delta,
      @NonNull Runnable onChanged) {
    boolean allowed = delta > 0 ? AdvancementRules.canRaise(character, skill)
        : AdvancementRules.canLower(character, skill);
    if (allowed) {
      character.advancement().stageSkillRank(skill, delta);
    }
    onChanged.run();
  }
}
