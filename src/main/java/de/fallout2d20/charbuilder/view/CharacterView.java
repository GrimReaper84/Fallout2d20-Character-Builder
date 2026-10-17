package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.advancement.AdvancementRules;
import de.fallout2d20.charbuilder.model.character.apparel.ApparelRules;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.model.character.creation.CreationRules;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import org.jspecify.annotations.NonNull;

/**
 * The view of one character: the header bar above the tabs (Stats, Status, Perks, Weapons, Armor, Gear, Notes).
 * Empty until the first {@link #refresh()}, which rebuilds the header and every tab's content after a change but keeps the tab pane, so
 * the selected tab and each tab's scroll position stay. A tab with rule hints shows a warning icon, with
 * the hints as tooltip.
 */
final class CharacterView {

  private final @NonNull Character character;
  private final @NonNull ViewCallbacks callbacks;
  private final @NonNull BorderPane root = new BorderPane();
  /** The tabs and their scroll panes, in the order of {@link CharacterTab#values()}. */
  private final @NonNull List<Tab> tabs = new ArrayList<>();
  private final @NonNull List<ScrollPane> scrollPanes = new ArrayList<>();

  CharacterView(@NonNull Character character, @NonNull ViewCallbacks callbacks) {
    this.character = character;
    this.callbacks = callbacks;
    TabPane tabPane = new TabPane();
    tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    for (CharacterTab kind : CharacterTab.values()) {
      ScrollPane scrollPane = new ScrollPane();
      scrollPane.setFitToWidth(true);
      // The notes field fills its whole tab and scrolls itself.
      scrollPane.setFitToHeight(kind == CharacterTab.NOTES);
      Tab tab = new Tab(kind.title(), scrollPane);
      tabs.add(tab);
      scrollPanes.add(scrollPane);
      tabPane.getTabs().add(tab);
    }
    root.setCenter(tabPane);
  }

  @NonNull Node node() {
    return root;
  }

  /** Rebuilds header and tab contents from the character; keeps the selected tab and scroll positions. */
  void refresh() {
    List<CreationProblem> hints = hints(character);
    root.setTop(CharacterHeader.build(character, hints, callbacks));
    CharacterTab[] kinds = CharacterTab.values();
    for (int i = 0; i < kinds.length; i++) {
      CharacterTab kind = kinds[i];
      List<CreationProblem> tabHints = hints.stream().filter(hint -> hint.area() == kind.area()).toList();
      ScrollPane scrollPane = scrollPanes.get(i);
      double position = scrollPane.getVvalue();
      scrollPane.setContent(content(kind, tabHints));
      Platform.runLater(() -> scrollPane.setVvalue(position));
      mark(tabs.get(i), tabHints);
    }
  }

  private @NonNull Node content(@NonNull CharacterTab kind, @NonNull List<CreationProblem> tabHints) {
    return switch (kind) {
      case STATS -> StatsTab.build(character, tabHints, callbacks);
      case STATUS -> StatusTab.build(character, callbacks);
      case PERKS -> PerksTab.build(character, tabHints, callbacks);
      case WEAPONS -> WeaponsTab.build(character, callbacks);
      case ARMOR -> ArmorTab.build(character, callbacks);
      case GEAR -> GearTab.build(character, tabHints, callbacks);
      case NOTES -> NotesTab.build(character, callbacks);
    };
  }

  private static void mark(@NonNull Tab tab, @NonNull List<CreationProblem> tabHints) {
    if (tabHints.isEmpty()) {
      tab.setGraphic(null);
      tab.setTooltip(null);
    } else {
      tab.setGraphic(CharacterHeader.warningIcon());
      tab.setTooltip(new Tooltip(tabHints.stream().map(CreationProblem::message)
          .collect(Collectors.joining("\n"))));
    }
  }

  /**
   * All hints of the character with their areas: the creation rules during creation and in GM mode (D2:
   * content hints only), the advancement rules while level-up points are spent, and the apparel rules always.
   */
  static @NonNull List<CreationProblem> hints(@NonNull Character character) {
    List<CreationProblem> hints = new ArrayList<>();
    if (!character.isCreationComplete() || character.isGmMode()) {
      hints.addAll(CreationRules.problems(character));
    } else if (AdvancementRules.isActive(character)) {
      hints.addAll(AdvancementRules.problems(character));
    }
    hints.addAll(ApparelRules.problems(character));
    return hints;
  }
}
