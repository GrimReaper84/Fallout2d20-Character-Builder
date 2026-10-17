package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.advancement.AdvancementRules;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Stats tab in two halves of equal width: rule hints, traits (with the origin trait and the origin's
 * options) and S.P.E.C.I.A.L. on the left, skills on the right. Editable during creation and in GM mode;
 * afterwards read-only, except the skill ranks to spend after a level-up. The origin itself is chosen in
 * the header bar.
 */
final class StatsTab {

  private StatsTab() {}

  /** @param hints the hints of this tab */
  static @NonNull Node build(@NonNull Character character, @NonNull List<CreationProblem> hints,
      @NonNull ViewCallbacks callbacks) {
    boolean locked = character.isCreationComplete() && !character.isGmMode();
    Runnable onChanged = callbacks.onChanged();

    MigPane left = OverviewParts.half();
    if (!hints.isEmpty()) {
      OverviewParts.sectionHeading(left, "Rule hints");
      left.add(OverviewParts.wrappingInCell(OverviewParts.hintLabel(hints)), "growx");
    }
    OverviewParts.sectionHeading(left, "Traits");
    left.add(TraitSection.build(character, locked, callbacks.onEdited(), onChanged), "growx");
    OverviewParts.sectionHeading(left, "S.P.E.C.I.A.L.");
    left.add(AttributeSection.build(character, locked, onChanged));

    MigPane right = OverviewParts.half();
    OverviewParts.sectionHeading(right, "Skills");
    if (locked && AdvancementRules.choosesSkills(character)) {
      right.add(AdvancementSkillSection.build(character, onChanged));
    } else {
      right.add(SkillSection.build(character, locked, onChanged));
    }

    MigPane pane = new MigPane("insets 16, gap 32 0", "[sg half, grow, fill][sg half, grow, fill]");
    // Never squeezed below its natural width (that cut the row names to "…"); a too narrow window scrolls.
    pane.setMinWidth(Region.USE_PREF_SIZE);
    pane.add(left, "top");
    pane.add(right, "top");
    return pane;
  }
}
