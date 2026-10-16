package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.advancement.AdvancementRules;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Perks tab: the perk grid during creation, after a level-up and in GM mode; otherwise the perks as a
 * read-only list.
 */
final class PerksTab {

  private PerksTab() {}

  /** @param hints the hints of this tab */
  static @NonNull Node build(@NonNull Character character, @NonNull List<CreationProblem> hints,
      @NonNull ViewCallbacks callbacks) {
    MigPane pane = OverviewParts.grid();
    OverviewParts.addHints(pane, hints);
    boolean locked = character.isCreationComplete() && !character.isGmMode();
    if (locked && !AdvancementRules.choosesPerks(character)) {
      addPerkList(pane, character);
    } else {
      PerkSection.addTo(pane, character, callbacks.onChanged());
    }
    return pane;
  }

  /** Perks of a complete character, read-only: name, rank and choices. */
  private static void addPerkList(@NonNull MigPane pane, @NonNull Character character) {
    if (character.getPerks().isEmpty()) {
      pane.add(new Label("None"), "span 4");
    }
    for (PerkSelection perk : character.getPerks()) {
      String chosen = PerkCard.choiceText(perk);
      String choices = chosen.isEmpty() ? "" : " (" + chosen + ")";
      pane.add(OverviewParts.importMarked(perk(perk), perk.importedRanks() > 0));
      pane.add(new Label("Rank " + perk.rank() + choices), "span 3");
    }
  }

  /** Perk resolved from the catalog (name, tooltip with summary and source), else the plain reference. */
  private static @NonNull Label perk(@NonNull PerkSelection selection) {
    Optional<PerkDefinition> perk = PerkCatalog.byId(selection.perk().id());
    if (perk.isEmpty()) {
      return OverviewParts.reference(selection.perk());
    }
    Label label = new Label(perk.get().name());
    label.setTooltip(new Tooltip(perk.get().summary() + "\n" + perk.get().sources().stream()
        .map(Object::toString).collect(Collectors.joining(", "))));
    return label;
  }
}
