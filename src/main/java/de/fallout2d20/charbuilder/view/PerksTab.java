package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.advancement.AdvancementRules;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import java.util.List;
import javafx.scene.Node;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Perks tab: the perk grid in every mode. Perks can be changed during creation, in GM mode and while
 * level-up perks are pending; otherwise the grid is read-only.
 */
final class PerksTab {

  private PerksTab() {}

  /** @param hints the hints of this tab */
  static @NonNull Node build(@NonNull PlayerCharacter character, @NonNull List<CreationProblem> hints,
      @NonNull ViewCallbacks callbacks) {
    // The perk cards flow over the whole width (user: Fallout 4 style grid).
    MigPane pane = Layouts.fillingPage();
    OverviewParts.addHints(pane, hints);
    boolean editable = !character.isCreationComplete() || character.isGmMode()
        || AdvancementRules.choosesPerks(character);
    PerkSection.addTo(pane, character, editable, callbacks.onChanged());
    return pane;
  }
}
