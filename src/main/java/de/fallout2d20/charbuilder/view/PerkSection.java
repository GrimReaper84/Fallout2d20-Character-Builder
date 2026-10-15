package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.creation.PerkRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Perk choice of a character in creation as a grid of {@link PerkCard}s (user concept): chosen and
 * available perks first, then the perks whose next rank cannot be taken, greyed out with the reason
 * as tooltip. Every change goes to the model and then to {@code onChanged}, which rebuilds the
 * overview.
 */
final class PerkSection {

  private static final double GAP = 8;

  private PerkSection() {}

  /** Adds the perks left and both grid areas to a four-column overview grid. */
  static void addTo(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onChanged) {
    pane.add(new Label("Perks left"));
    pane.add(new Label(PerkRules.perksLeft(character) + " of " + PerkRules.budget(character)), "span 3");
    List<PerkDefinition> perks = PerkCatalog.available(character.getEnabledBooks());
    Map<String, List<String>> blockers = new HashMap<>();
    List<PerkDefinition> available = new ArrayList<>();
    List<PerkDefinition> unavailable = new ArrayList<>();
    for (PerkDefinition perk : perks) {
      List<String> reasons = PerkRules.blockers(character, perk);
      blockers.put(perk.id(), reasons);
      (PerkRules.rank(character, perk.id()) > 0 || reasons.isEmpty() ? available : unavailable).add(perk);
    }
    addArea(pane, "Chosen and available", available, blockers, true, character, onChanged);
    addArea(pane, "Not available", unavailable, blockers, false, character, onChanged);
  }

  /**
   * One grid area. Available are chosen perks and perks whose next rank could be taken if a perk
   * choice is left.
   */
  private static void addArea(@NonNull MigPane pane, @NonNull String title,
      @NonNull List<PerkDefinition> perks, @NonNull Map<String, List<String>> blockers, boolean available,
      @NonNull Character character, @NonNull Runnable onChanged) {
    if (perks.isEmpty()) {
      return;
    }
    Label heading = new Label(title);
    heading.setStyle("-fx-font-weight: bold;");
    pane.add(heading, "span 4, gaptop 6, left");
    FlowPane grid = new FlowPane(GAP, GAP);
    for (PerkDefinition perk : perks) {
      grid.getChildren().add(PerkCard.of(character, perk, blockers.getOrDefault(perk.id(), List.of()),
          available, onChanged));
    }
    pane.add(grid, "span 4, growx, wmin 0");
  }
}
