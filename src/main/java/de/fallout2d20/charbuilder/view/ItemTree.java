package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.scene.control.TreeView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The tree of the Gear tab: every item in its {@link ItemGroup}, groups in table order, empty groups left out,
 * items alphabetical inside a group; all groups expanded.
 */
final class ItemTree {

  private static final @NonNull Comparator<ItemEntry> BY_NAME =
      Comparator.comparing(entry -> entry.item().displayName(), String.CASE_INSENSITIVE_ORDER);

  private ItemTree() {}

  /** The items as tree entries with their groups, in list order. */
  static @NonNull List<ItemEntry> entries(@NonNull List<CarriedItem> items) {
    List<ItemEntry> entries = new ArrayList<>();
    for (int i = 0; i < items.size(); i++) {
      entries.add(new ItemEntry(i, items.get(i), ItemGroup.of(items.get(i))));
    }
    return entries;
  }

  /** The entries per group in tree order; only groups with at least one item. */
  static @NonNull Map<ItemGroup, List<ItemEntry>> grouped(@NonNull List<ItemEntry> entries) {
    Map<ItemGroup, List<ItemEntry>> groups = new EnumMap<>(ItemGroup.class);
    for (ItemEntry entry : entries) {
      groups.computeIfAbsent(entry.group(), group -> new ArrayList<>()).add(entry);
    }
    groups.values().forEach(group -> group.sort(BY_NAME));
    return groups;
  }

  /**
   * The tree with {@code current} selected and scrolled into view (none if {@code null}); {@code onSelect}
   * gets every newly selected item, or {@code null} when a group or nothing is selected.
   */
  static @NonNull TreeView<ItemTreeNode> build(@NonNull List<ItemEntry> entries, @Nullable ItemEntry current,
      @NonNull Consumer<@Nullable ItemEntry> onSelect) {
    return GroupedTree.<ItemTreeNode, ItemGroup, ItemEntry>build(grouped(entries),
        entry -> current != null && entry.index() == current.index(), ItemEntry.class, ItemTreeCell::new, onSelect);
  }
}
