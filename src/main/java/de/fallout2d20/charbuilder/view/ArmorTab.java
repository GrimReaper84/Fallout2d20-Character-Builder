package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.apparel.ApparelRules;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.view.dialog.AddApparelDialog;
import de.fallout2d20.charbuilder.view.dialog.ConfirmDialog;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Armor tab, in every mode: on the left the add and remove buttons and the tree of all worn and carried
 * apparel by where it sits ({@link ApparelTree}); on the right the damage resistance per hit location with
 * the tab's hints ({@link ApparelResistances}) and below them the details of the selected piece with its
 * Worn option ({@link ApparelDetails}). Selecting a piece only replaces the details; the selection and both
 * scroll positions survive the rebuild after a change ({@link MasterDetailState}, layout {@link MasterDetail}).
 */
final class ArmorTab {

  private ArmorTab() {}

  static @NonNull Node build(@NonNull PlayerCharacter character, @NonNull List<CreationProblem> tabHints,
      @NonNull MasterDetailState selection, @NonNull ViewCallbacks callbacks) {
    List<ApparelEntry> entries = ApparelTree.entries(character.gear().getApparel(), character.hitLocations());
    Integer selected = selection.current(entries.size());
    ApparelEntry current = selected == null ? null : entries.get(selected);

    // Resistances and hints stay; selecting another piece only replaces the details below them.
    MigPane right = Layouts.column();
    ApparelResistances.addTo(right, character, tabHints);
    BorderPane details = new BorderPane(ApparelDetails.build(character, current, callbacks));
    right.add(details, Layouts.sectionStart());
    ScrollPane rightScroll = MasterDetail.right(right, selection);
    RemoveButton remove = new RemoveButton(() -> {
      Integer index = selection.current(character.gear().getApparel().size());
      if (index == null) {
        return;
      }
      WornApparel item = character.gear().getApparel().get(index);
      if (ConfirmDialog.confirm(window(rightScroll), "Remove apparel", "Remove " + item.displayName() + "?",
          "The item and its note are removed.", "Remove")) {
        character.gear().removeApparel(index);
        selection.clear();
        callbacks.onChanged().run();
      }
    });
    updateRemove(remove, current);
    TreeView<ApparelTreeNode> tree = ApparelTree.build(entries, current, entry -> {
      if (entry == null) {
        selection.clear();
      } else {
        selection.select(entry.index());
      }
      updateRemove(remove, entry);
      details.setCenter(ApparelDetails.build(character, entry, callbacks));
    });

    MigPane left = Layouts.column();
    OverviewParts.sectionHeading(left, "Apparel");
    MigPane buttons = Layouts.row();
    buttons.add(addButton(character, selection, callbacks.onChanged()));
    buttons.add(remove.node());
    left.add(buttons);
    left.add(tree, Layouts.filling());
    return MasterDetail.layout(MasterDetail.left(left, selection), rightScroll);
  }

  /** Opens the catalog of the items offered to the character; the new piece is selected. */
  private static @NonNull Button addButton(@NonNull PlayerCharacter character, @NonNull MasterDetailState selection,
      @NonNull Runnable onChanged) {
    Button add = new Button("Add apparel");
    add.setGraphic(new FontIcon("fas-plus"));
    ApparelKind defaultKind = ApparelRules.isRobot(character) ? ApparelKind.ROBOT_ARMOR : ApparelKind.ARMOR;
    add.setOnAction(_ -> AddApparelDialog.show(window(add), offered(character), character.hitLocations(),
        defaultKind).ifPresent(item -> {
          character.gear().addApparel(item);
          selection.select(character.gear().getApparel().size() - 1);
          onChanged.run();
        }));
    return add;
  }

  /** Catalog items of the enabled books offered to the character ({@link ApparelRules#isOffered}). */
  private static @NonNull List<ApparelData> offered(@NonNull PlayerCharacter character) {
    return ApparelCatalog.available(character.getEnabledBooks()).stream()
        .filter(item -> ApparelRules.isOffered(character, item.kind(), item.isHat())).toList();
  }

  private static void updateRemove(@NonNull RemoveButton remove, @Nullable ApparelEntry entry) {
    remove.update(entry != null,
        entry == null ? "Select a piece to remove it." : "Remove " + entry.item().displayName());
  }

  private static @Nullable Window window(@NonNull Node node) {
    return node.getScene() == null ? null : node.getScene().getWindow();
  }
}
