package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.creation.SkillRules;
import java.util.Optional;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Tag skills and skill ranks of a character in creation (CRB p. 58): a Tag checkbox, the rank and
 * −/+ buttons per skill, and the free Tags and skill points left. Tags of the origin are shown
 * checked and disabled. Every change goes to the model and then to {@code onChanged}, which
 * rebuilds the overview.
 */
final class SkillSection {

  private static final String FULL_TOOLTIP = "All free Tag skills are chosen; uncheck one first.";
  private static final String TAG_TOOLTIP = "Tag skill: starts at rank 2 without costing skill points.";

  private SkillSection() {}

  /** Adds the skill rows to a four-column overview grid. */
  static void addTo(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onChanged) {
    int pointsLeft = SkillRules.skillPointsLeft(character);
    // D2: for a complete character the GM corrects ranks; budgets and bought ranks do not matter there.
    boolean gmCorrects = character.isGmMode() && character.isCreationComplete();
    if (gmCorrects) {
      addGmRows(pane, character, onChanged);
      return;
    }
    pane.add(new Label("Tags left"));
    pane.add(new Label(Integer.toString(SkillRules.freeTagsLeft(character))));
    pane.add(new Label("Points left"));
    pane.add(new Label(pointsLeft + " of " + SkillRules.skillPointBudget(character) + " (" + SkillRules.SKILL_POINTS_BASE + " + "
        + Attribute.INTELLIGENCE.abbreviation() + ")"));
    for (Skill skill : Skill.values()) {
      pane.add(tagBox(character, skill, onChanged));
      pane.add(new Label("(" + skill.governingAttribute().abbreviation() + ")"));
      boolean canLower = SkillRules.canLower(character, skill);
      boolean canRaise = SkillRules.canRaise(character, skill);
      pane.add(StepButtons.withValue(new Label(Integer.toString(character.skillRank(skill))),
          StepButtons.NUMBER_WIDTH, StepButtons.of(
          StepButtons.minus(canLower ? "Take back a rank of " + skill.displayName()
                  : "No bought ranks of " + skill.displayName() + " to take back.",
              canLower, () -> step(character, skill, -1, onChanged)),
          StepButtons.plus(canRaise ? "Buy a rank of " + skill.displayName()
                  : pointsLeft <= 0 ? "No skill points left."
                  : skill.displayName() + " is at its maximum rank of " + character.skillRankCap() + ".",
              canRaise, () -> step(character, skill, 1, onChanged)))), "span 2");
    }
  }

  /** GM corrections (D2): −/+ change the rank directly, up to the rank cap. */
  private static void addGmRows(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onChanged) {
    for (Skill skill : Skill.values()) {
      pane.add(tagBox(character, skill, onChanged));
      pane.add(new Label("(" + skill.governingAttribute().abbreviation() + ")"));
      boolean canLower = SkillRules.canLower(character, skill);
      boolean canRaise = SkillRules.canRaise(character, skill);
      pane.add(StepButtons.withValue(new Label(Integer.toString(character.skillRank(skill))),
          StepButtons.NUMBER_WIDTH, StepButtons.of(
          StepButtons.minus(canLower ? "Lower " + skill.displayName() + " by one rank (GM correction)"
                  : skill.displayName() + " cannot go lower (a Tag! bonus or rank 0).",
              canLower, () -> step(character, skill, -1, onChanged)),
          StepButtons.plus(canRaise ? "Raise " + skill.displayName() + " by one rank (GM correction)"
                  : skill.displayName() + " is at its maximum rank of " + character.skillRankCap() + ".",
              canRaise, () -> step(character, skill, 1, onChanged)))), "span 2");
    }
  }

  /** The Tag checkbox with the skill name; disabled with the reason when it cannot be switched. */
  private static @NonNull HBox tagBox(@NonNull Character character, @NonNull Skill skill,
      @NonNull Runnable onChanged) {
    CheckBox box = new CheckBox(skill.displayName());
    box.setSelected(character.isTagged(skill));
    box.setDisable(!SkillRules.canToggleTag(character, skill));
    box.selectedProperty().addListener((obs, old, value) -> {
      character.setFreeTag(skill, value);
      onChanged.run();
    });
    HBox wrapper = new HBox(box);
    Tooltip.install(wrapper, new Tooltip(tagTooltip(character, skill)));
    return wrapper;
  }

  private static @NonNull String tagTooltip(@NonNull Character character, @NonNull Skill skill) {
    String freeTag = character.skillState(skill).tagged() ? " Uncheck to free this Tag." : "";
    if (character.isTaggedByOrigin(skill)) {
      return "Tag skill from " + character.getOriginData().map(OriginData::name).orElse("the origin")
          + "." + freeTag;
    }
    if (character.isTaggedByPerk(skill)) {
      return "Tag skill from the Tag! perk." + freeTag;
    }
    Optional<TraitDefinition> forbidding = character.tagForbiddenBy(skill);
    if (forbidding.isPresent()) {
      return skill.displayName() + " cannot be a Tag skill with " + forbidding.get().name() + "." + freeTag;
    }
    return SkillRules.canToggleTag(character, skill) ? TAG_TOOLTIP : FULL_TOOLTIP;
  }

  /**
   * Buys (+1) or gives back (−1) a rank. The model may have changed since the view was built (e.g. a
   * level typed into the level field is taken when the button gets the focus), so the handler works on
   * the current ranks and checks again; the overview is rebuilt either way.
   */
  private static void step(@NonNull Character character, @NonNull Skill skill, int delta,
      @NonNull Runnable onChanged) {
    SkillRules.step(character, skill, delta);
    onChanged.run();
  }
}
