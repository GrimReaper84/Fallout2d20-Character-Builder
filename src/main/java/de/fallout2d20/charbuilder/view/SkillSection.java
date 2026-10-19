package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
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
 * Tag skills and skill ranks (CRB p. 58) as a six-column grid: Tag, skill, governing attribute, rank,
 * − and +. During creation a budget row shows the free Tags and skill points left; Tags of the origin
 * are shown checked and disabled. In GM mode for a complete character −/+ correct the rank directly (D2).
 * Afterwards the skills are read-only. Every change goes to the model and then to {@code onChanged},
 * which rebuilds the view.
 */
final class SkillSection {

  static final @NonNull String TAG_MARKER = "●";
  static final @NonNull String TAG_LEGEND = TAG_MARKER + " = Tag skill";

  private static final String FULL_TOOLTIP = "All free Tag skills are chosen; uncheck one first.";
  private static final String TAG_TOOLTIP = "Tag skill: starts at rank 2 without costing skill points.";

  private SkillSection() {}

  /** @param locked whether the character is complete and not in GM mode (ranks read-only) */
  static @NonNull MigPane build(@NonNull PlayerCharacter character, boolean locked, @NonNull Runnable onChanged) {
    MigPane pane = grid();
    if (locked) {
      addLockedRows(pane, character);
      return pane;
    }
    // D2: for a complete character the GM corrects ranks; budgets and bought ranks do not matter there.
    boolean gmCorrects = character.isGmMode() && character.isCreationComplete();
    if (!gmCorrects) {
      pane.add(budget(character), "span 6");
    }
    for (Skill skill : Skill.values()) {
      CheckBox box = tagBox(character, skill, onChanged);
      pane.add(withTooltip(box, tagTooltip(character, skill)));
      Label name = new Label(skill.displayName());
      // Clicking the name switches the Tag, as when the name was the checkbox's own text.
      name.setOnMouseClicked(event -> {
        if (!box.isDisabled()) {
          box.fire();
        }
      });
      pane.add(name);
      pane.add(attribute(skill));
      pane.add(rank(Integer.toString(character.skillRank(skill)), StepButtons.NUMBER_WIDTH));
      addSteppers(pane, character, skill, gmCorrects, onChanged);
    }
    return pane;
  }

  /** An empty six-column skill grid, everything left-aligned: Tag, skill, attribute, rank, −, +. */
  static @NonNull MigPane grid() {
    return new MigPane("insets 0, gap 8 4, wrap 6", "[left][left][left][left][left][left]");
  }

  /** Skill name and governing attribute, e.g. {@code Athletics} and {@code (STR)}. */
  static void addNameCells(@NonNull MigPane pane, @NonNull Skill skill) {
    pane.add(new Label(skill.displayName()));
    pane.add(attribute(skill));
  }

  /** The rank cell; keeps a minimum width so the −/+ columns do not move when the text grows. */
  static @NonNull Label rank(@NonNull String text, double minWidth) {
    Label rank = new Label(text);
    rank.setMinWidth(minWidth);
    return rank;
  }

  private static @NonNull Label attribute(@NonNull Skill skill) {
    return new Label("(" + skill.governingAttribute().abbreviation() + ")");
  }

  /** Read-only rows of a complete character: Tag marker, skill, attribute and rank. */
  private static void addLockedRows(@NonNull MigPane pane, @NonNull PlayerCharacter character) {
    for (Skill skill : Skill.values()) {
      pane.add(new Label(character.isTagged(skill) ? TAG_MARKER : ""));
      addNameCells(pane, skill);
      pane.add(OverviewParts.baselineMarked(Integer.toString(character.skillRank(skill)),
          character.isLegacyImport()), "span 3");
    }
    pane.add(new Label(TAG_LEGEND), "span 6");
  }

  /** {@code [Tags left: x | Points left: y]}, with the skill point budget as tooltip. */
  private static @NonNull Label budget(@NonNull PlayerCharacter character) {
    int pointsLeft = SkillRules.skillPointsLeft(character);
    Label budget = new Label("[Tags left: " + SkillRules.freeTagsLeft(character) + " | Points left: "
        + pointsLeft + "]");
    budget.setTooltip(new Tooltip(pointsLeft + " of " + SkillRules.skillPointBudget(character)
        + " skill points left (" + SkillRules.SKILL_POINTS_BASE + " + "
        + Attribute.INTELLIGENCE.abbreviation() + ")"));
    return budget;
  }

  /** The − and + cells: buying ranks during creation, direct rank corrections by the GM (D2). */
  private static void addSteppers(@NonNull MigPane pane, @NonNull PlayerCharacter character, @NonNull Skill skill,
      boolean gmCorrects, @NonNull Runnable onChanged) {
    String name = skill.displayName();
    boolean canLower = SkillRules.canLower(character, skill);
    boolean canRaise = SkillRules.canRaise(character, skill);
    String lowerTooltip;
    String raiseTooltip;
    if (gmCorrects) {
      lowerTooltip = canLower ? "Lower " + name + " by one rank (GM correction)"
          : name + " cannot go lower (a Tag! bonus or rank 0).";
      raiseTooltip = canRaise ? "Raise " + name + " by one rank (GM correction)" : atCap(character, skill);
    } else {
      lowerTooltip = canLower ? "Take back a rank of " + name : "No bought ranks of " + name + " to take back.";
      raiseTooltip = canRaise ? "Buy a rank of " + name
          : SkillRules.skillPointsLeft(character) <= 0 ? "No skill points left." : atCap(character, skill);
    }
    pane.add(StepButtons.minus(lowerTooltip, canLower, () -> step(character, skill, -1, onChanged)));
    pane.add(StepButtons.plus(raiseTooltip, canRaise, () -> step(character, skill, 1, onChanged)));
  }

  private static @NonNull String atCap(@NonNull PlayerCharacter character, @NonNull Skill skill) {
    return skill.displayName() + " is at its maximum rank of " + character.skillRankCap() + ".";
  }

  /** The Tag checkbox (without text); disabled when it cannot be switched. */
  private static @NonNull CheckBox tagBox(@NonNull PlayerCharacter character, @NonNull Skill skill,
      @NonNull Runnable onChanged) {
    CheckBox box = new CheckBox();
    box.setSelected(character.isTagged(skill));
    box.setDisable(!SkillRules.canToggleTag(character, skill));
    box.selectedProperty().addListener((obs, old, value) -> {
      character.setFreeTag(skill, value);
      onChanged.run();
    });
    return box;
  }

  /** The tooltip sits on a wrapper because disabled controls show none; it names the reason. */
  private static @NonNull HBox withTooltip(@NonNull CheckBox box, @NonNull String tooltip) {
    HBox wrapper = new HBox(box);
    Tooltip.install(wrapper, new Tooltip(tooltip));
    return wrapper;
  }

  private static @NonNull String tagTooltip(@NonNull PlayerCharacter character, @NonNull Skill skill) {
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
   * the current ranks and checks again; the view is rebuilt either way.
   */
  private static void step(@NonNull PlayerCharacter character, @NonNull Skill skill, int delta,
      @NonNull Runnable onChanged) {
    SkillRules.step(character, skill, delta);
    onChanged.run();
  }
}
