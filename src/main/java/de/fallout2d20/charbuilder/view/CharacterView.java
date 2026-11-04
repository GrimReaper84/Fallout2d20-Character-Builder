package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.advancement.AdvancementRules;
import de.fallout2d20.charbuilder.model.character.apparel.ApparelRules;
import de.fallout2d20.charbuilder.model.character.creation.BookRules;
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
 * The view of one character: the header bar above the tabs (Stats, Status, Perks, Weapons, Armor, Gear,
 * Notes). Empty until the first {@link #refresh()}, which rebuilds the header and every tab's content after a
 * change but keeps the tab pane, so the selected tab, each tab's scroll position and the selection of the
 * Weapons, Armor and Gear tabs ({@link MasterDetailState}) stay – except after the starting equipment was
 * synced into the inventory, which moves entries. A tab with rule hints shows a warning icon, with
 * the hints as tooltip. The status is on a {@link SlidePanel} or, by choice in the Settings menu, a tab of its own.
 */
final class CharacterView {

  private final @NonNull PlayerCharacter character;
  private final @NonNull ViewCallbacks callbacks;
  private final @NonNull BorderPane root = new BorderPane();
  private final @NonNull TabPane tabPane = new TabPane();
  private final @NonNull SlidePanel statusPanel;
  /** The tabs and their scroll panes, in the order of {@link CharacterTab#values()}. */
  private final @NonNull List<Tab> tabs = new ArrayList<>();
  private final @NonNull List<ScrollPane> scrollPanes = new ArrayList<>();
  private final @NonNull MasterDetailState weaponSelection = new MasterDetailState(WeaponsTab.UNARMED_STRIKE_INDEX);
  private final @NonNull MasterDetailState apparelSelection = new MasterDetailState(null);
  private final @NonNull MasterDetailState itemSelection = new MasterDetailState(null);
  /** The character's sync count at the last refresh: a sync moves entries, so the selections no longer fit. */
  private int startingEquipmentSyncs;
  private boolean statusAsTab;

  /**
   * Creates the empty view.
   *
   * @param statusAsTab whether the status is a tab instead of the slide panel
   * @param statusButtonPosition where the slide panel's button sits, shared by all characters
   */
  CharacterView(@NonNull PlayerCharacter character, @NonNull ViewCallbacks callbacks, boolean statusAsTab,
      @NonNull FloatingButtonPosition statusButtonPosition) {
    statusPanel = new SlidePanel(root, "fas-heartbeat", "Status (drag to move the button)", statusButtonPosition);
    this.character = character;
    this.callbacks = callbacks;
    this.statusAsTab = statusAsTab;
    tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    for (CharacterTab kind : CharacterTab.values()) {
      ScrollPane scrollPane = new ScrollPane();
      scrollPane.setFitToWidth(true);
      // The notes field and the weapon, apparel and item trees fill their whole tab and scroll themselves.
      scrollPane.setFitToHeight(kind == CharacterTab.NOTES || kind == CharacterTab.WEAPONS
          || kind == CharacterTab.ARMOR || kind == CharacterTab.GEAR);
      Tab tab = new Tab(kind.title(), scrollPane);
      tabs.add(tab);
      scrollPanes.add(scrollPane);
      if (kind != CharacterTab.STATUS || statusAsTab) {
        tabPane.getTabs().add(tab);
      }
    }
    root.setCenter(tabPane);
    statusPanel.setAvailable(!statusAsTab);
  }

  @NonNull Node node() {
    return statusPanel.node();
  }

  /** Shows the status as a tab at its usual place or on the slide panel; takes effect at once. */
  void setStatusAsTab(boolean asTab) {
    if (asTab == statusAsTab) {
      return;
    }
    statusAsTab = asTab;
    int position = List.of(CharacterTab.values()).indexOf(CharacterTab.STATUS);
    Tab status = tabs.get(position);
    if (asTab) {
      tabPane.getTabs().add(position, status);
    } else {
      tabPane.getTabs().remove(status);
    }
    statusPanel.setAvailable(!asTab);
    refresh();
  }

  /** Rebuilds header and tab contents from the character; keeps the selected tab and scroll positions. */
  void refresh() {
    if (character.startingGear().syncs() != startingEquipmentSyncs) {
      startingEquipmentSyncs = character.startingGear().syncs();
      weaponSelection.select(WeaponsTab.UNARMED_STRIKE_INDEX);
      apparelSelection.clear();
      itemSelection.clear();
    }
    List<CreationProblem> hints = hints(character);
    root.setTop(CharacterHeader.build(character, hints, callbacks));
    if (!statusAsTab) {
      statusPanel.setContent(StatusTab.buildStacked(character, callbacks));
    }
    CharacterTab[] kinds = CharacterTab.values();
    for (int i = 0; i < kinds.length; i++) {
      CharacterTab kind = kinds[i];
      if (kind == CharacterTab.STATUS && !statusAsTab) {
        continue;
      }
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
      case WEAPONS -> WeaponsTab.build(character, weaponSelection, callbacks);
      case ARMOR -> ArmorTab.build(character, tabHints, apparelSelection, callbacks);
      case GEAR -> GearTab.build(character, tabHints, itemSelection, callbacks);
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
   * All hints of the character with their areas: the creation rules during creation and in GM free edit (D2:
   * content hints only), the advancement rules while level-up points are spent, and the apparel and book rules
   * always (book hints of weapons appear with the gear, the Weapons tab shows none).
   */
  static @NonNull List<CreationProblem> hints(@NonNull PlayerCharacter character) {
    List<CreationProblem> hints = new ArrayList<>();
    if (!character.isCreationComplete() || character.isGmMode()) {
      hints.addAll(CreationRules.problems(character));
    } else if (AdvancementRules.isActive(character)) {
      hints.addAll(AdvancementRules.problems(character));
    }
    hints.addAll(ApparelRules.problems(character));
    hints.addAll(BookRules.problems(character));
    return hints;
  }
}
