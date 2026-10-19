package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.creation.PerkRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The perk grid of {@link PerkCard}s (user concept) in four groups: chosen perks, available perks (the next
 * rank could be taken), perks that are not available (greyed, the reasons in the card's info panel) and perks
 * that are not possible for a robot or because a chosen perk excludes them ({@link PerkRules#isImpossible}; user
 * requests 2026-10-19). The last three can be collapsed; the state is kept in the {@link ViewPreferences}. While perks
 * can be changed (creation, GM mode, pending level-up perks) the cards have −/+ buttons and the perks left
 * are shown; every change goes to the model and then to {@code onChanged}, which rebuilds the view.
 */
final class PerkSection {

  private static final double GAP = 8;
  private static final @NonNull String AVAILABLE_EXPANDED = "perks.available.expanded";
  private static final @NonNull String UNAVAILABLE_EXPANDED = "perks.unavailable.expanded";
  private static final @NonNull String EXCLUDED_EXPANDED = "perks.excluded.expanded";

  private PerkSection() {}

  /** @param editable whether perks can be changed now */
  static void addTo(@NonNull MigPane pane, @NonNull PlayerCharacter character, boolean editable,
      @NonNull Runnable onChanged) {
    // D2: GM changes to a complete character have no budget.
    if (editable && !(character.isGmMode() && character.isCreationComplete())) {
      pane.add(new Label("[Perks left: " + PerkRules.perksLeft(character) + "]"), "span 4, left");
    }

    FlowPane chosen = grid();
    for (PerkSelection selection : character.getPerks()) {
      Optional<PerkDefinition> perk = PerkCatalog.byId(selection.perk().id());
      chosen.getChildren().add(perk.isPresent()
          ? PerkCard.of(character, perk.get(), PerkRules.blockers(character, perk.get()), editable, onChanged)
          : PerkCard.unresolved(selection));
    }
    List<Node> available = new ArrayList<>();
    List<Node> unavailable = new ArrayList<>();
    List<Node> excluded = new ArrayList<>();
    for (PerkDefinition perk : PerkCatalog.available(character.getEnabledBooks())) {
      if (PerkRules.rank(character, perk.id()) > 0) {
        continue;
      }
      List<String> blockers = PerkRules.blockers(character, perk);
      List<Node> group = PerkRules.isImpossible(character, perk) ? excluded
          : blockers.isEmpty() ? available : unavailable;
      group.add(PerkCard.of(character, perk, blockers, editable, onChanged));
    }

    Label heading = new Label("Chosen");
    heading.setStyle("-fx-font-weight: bold;");
    pane.add(heading, "span 4, gaptop 6, left");
    if (chosen.getChildren().isEmpty()) {
      pane.add(new Label("None"), "span 4, left");
    } else {
      pane.add(chosen, "span 4, growx, wmin 0");
    }
    addGroup(pane, "Available", available, AVAILABLE_EXPANDED, true);
    addGroup(pane, "Not available", unavailable, UNAVAILABLE_EXPANDED, false);
    addGroup(pane, "Not possible", excluded, EXCLUDED_EXPANDED, false);
  }

  /** A collapsible group titled with its count; nothing if it is empty. */
  private static void addGroup(@NonNull MigPane pane, @NonNull String title, @NonNull List<Node> cards,
      @NonNull String key, boolean expandedByDefault) {
    if (cards.isEmpty()) {
      return;
    }
    FlowPane grid = grid();
    grid.getChildren().addAll(cards);
    TitledPane group = new TitledPane(title + " (" + cards.size() + ")", grid);
    // No animation: the view is rebuilt after every change and must not replay it.
    group.setAnimated(false);
    group.setExpanded(ViewPreferences.user().getBoolean(key, expandedByDefault));
    group.expandedProperty().addListener((obs, old, expanded) -> ViewPreferences.user().putBoolean(key, expanded));
    pane.add(group, "span 4, growx, wmin 0, gaptop 6");
  }

  private static @NonNull FlowPane grid() {
    return new FlowPane(GAP, GAP);
  }
}
