package de.fallout2d20.charbuilder.view;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The tree of a master/detail tab (Weapons, Armor): one expanded node per group with its entries below,
 * groups and entries in the given order, the root hidden.
 */
final class GroupedTree {

  /** A tree never shrinks below this when other content of the left side grows; the side scrolls instead. */
  private static final double MIN_HEIGHT = 200;

  private GroupedTree() {}

  /**
   * The tree with the entry matching {@code isCurrent} selected and scrolled into view (the tree is rebuilt
   * after every change); {@code onSelect} gets every newly selected entry, or {@code null} when a group or
   * nothing is selected.
   *
   * @param entryType the class of the entries, to tell them from the groups
   */
  static <N, G extends N, E extends N> @NonNull TreeView<N> build(@NonNull Map<G, List<E>> groups,
      @NonNull Predicate<E> isCurrent, @NonNull Class<E> entryType, @NonNull Supplier<TreeCell<N>> cells,
      @NonNull Consumer<@Nullable E> onSelect) {
    TreeItem<N> root = new TreeItem<>();
    TreeItem<N> selected = null;
    for (Map.Entry<G, List<E>> group : groups.entrySet()) {
      TreeItem<N> groupItem = new TreeItem<>(group.getKey());
      groupItem.setExpanded(true);
      for (E entry : group.getValue()) {
        TreeItem<N> item = new TreeItem<>(entry);
        groupItem.getChildren().add(item);
        if (isCurrent.test(entry)) {
          selected = item;
        }
      }
      root.getChildren().add(groupItem);
    }
    TreeView<N> tree = new TreeView<>(root);
    tree.setShowRoot(false);
    tree.setMinHeight(MIN_HEIGHT);
    tree.setCellFactory(view -> cells.get());
    if (selected != null) {
      tree.getSelectionModel().select(selected);
      tree.scrollTo(tree.getRow(selected));
    }
    tree.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
      N value = item == null ? null : item.getValue();
      onSelect.accept(entryType.isInstance(value) ? entryType.cast(value) : null);
    });
    return tree;
  }
}
