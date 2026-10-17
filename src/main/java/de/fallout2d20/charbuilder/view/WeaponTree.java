package de.fallout2d20.charbuilder.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The tree of the Weapons tab: the weapons grouped by effective range ({@link RangeGroup}), groups from
 * near to far, empty groups left out, weapons alphabetical inside a group; all groups expanded.
 */
final class WeaponTree {

  private static final @NonNull Comparator<WeaponEntry> BY_NAME =
      Comparator.comparing(entry -> entry.attack().name(), String.CASE_INSENSITIVE_ORDER);

  private WeaponTree() {}

  /** The entries per group in tree order; only groups with at least one weapon. */
  static @NonNull Map<RangeGroup, List<WeaponEntry>> grouped(@NonNull List<WeaponEntry> entries) {
    Map<RangeGroup, List<WeaponEntry>> groups = new EnumMap<>(RangeGroup.class);
    for (WeaponEntry entry : entries) {
      groups.computeIfAbsent(RangeGroup.of(entry.attack()), group -> new ArrayList<>()).add(entry);
    }
    groups.values().forEach(group -> group.sort(BY_NAME));
    return groups;
  }

  /**
   * The tree with {@code current} selected and scrolled into view (none if {@code null}); {@code onSelect}
   * gets every newly selected weapon, or {@code null} when a group or nothing is selected.
   */
  static @NonNull TreeView<WeaponTreeNode> build(@NonNull List<WeaponEntry> entries, @Nullable WeaponEntry current,
      @NonNull Consumer<@Nullable WeaponEntry> onSelect) {
    TreeItem<WeaponTreeNode> root = new TreeItem<>();
    TreeItem<WeaponTreeNode> selected = null;
    for (Map.Entry<RangeGroup, List<WeaponEntry>> group : grouped(entries).entrySet()) {
      TreeItem<WeaponTreeNode> groupItem = new TreeItem<>(group.getKey());
      groupItem.setExpanded(true);
      for (WeaponEntry entry : group.getValue()) {
        TreeItem<WeaponTreeNode> item = new TreeItem<>(entry);
        groupItem.getChildren().add(item);
        if (entry == current) {
          selected = item;
        }
      }
      root.getChildren().add(groupItem);
    }
    TreeView<WeaponTreeNode> tree = new TreeView<>(root);
    tree.setShowRoot(false);
    tree.setCellFactory(view -> new WeaponTreeCell());
    if (selected != null) {
      tree.getSelectionModel().select(selected);
      // The tree is rebuilt after every change; keep the selected weapon in view.
      tree.scrollTo(tree.getRow(selected));
    }
    tree.getSelectionModel().selectedItemProperty().addListener((obs, old, item) ->
        onSelect.accept(item != null && item.getValue() instanceof WeaponEntry entry ? entry : null));
    return tree;
  }
}
