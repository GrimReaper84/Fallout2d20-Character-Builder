package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.apparel.Coverage;
import de.fallout2d20.charbuilder.model.character.apparel.ApparelValues;
import de.fallout2d20.charbuilder.model.character.apparel.CustomApparel;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javafx.scene.control.TreeView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The tree of the Armor tab: every piece (worn or carried) in its {@link ApparelGroup}, groups in body order,
 * empty groups left out, pieces alphabetical inside a group; all groups expanded.
 */
final class ApparelTree {

  private static final @NonNull Comparator<ApparelEntry> BY_NAME =
      Comparator.comparing(entry -> entry.item().displayName(), String.CASE_INSENSITIVE_ORDER);

  private ApparelTree() {}

  /** The pieces as tree entries with their groups on a body with these hit locations, in list order. */
  static @NonNull List<ApparelEntry> entries(@NonNull List<WornApparel> apparel, @NonNull List<HitLocation> body) {
    List<ApparelEntry> entries = new ArrayList<>();
    for (int i = 0; i < apparel.size(); i++) {
      WornApparel item = apparel.get(i);
      entries.add(new ApparelEntry(i, item, group(item, body), item.needsLocation(body)));
    }
    return entries;
  }

  /**
   * The group of a piece: robot plating in Plating, clothing in Clothing (it goes under armor, CRB p. 130);
   * otherwise the group of the locations it can cover on this body (a single-limb piece by both limbs it may
   * be worn on, robot armor of a Protectron as mapped), and Outfit if they span several groups or none are
   * known.
   */
  static @NonNull ApparelGroup group(@NonNull WornApparel item, @NonNull List<HitLocation> body) {
    ApparelKind kind = item.values().map(ApparelValues::kind).orElse(null);
    if (kind == ApparelKind.ROBOT_PLATING) {
      return ApparelGroup.PLATING;
    }
    if (kind == ApparelKind.CLOTHING) {
      return ApparelGroup.CLOTHING;
    }
    Set<ApparelGroup> groups = EnumSet.noneOf(ApparelGroup.class);
    for (HitLocation location : coverable(item, body)) {
      groups.add(ApparelGroup.of(location));
    }
    return groups.size() == 1 ? groups.iterator().next() : ApparelGroup.OUTFIT;
  }

  /** The entries per group in tree order; only groups with at least one piece. */
  static @NonNull Map<ApparelGroup, List<ApparelEntry>> grouped(@NonNull List<ApparelEntry> entries) {
    Map<ApparelGroup, List<ApparelEntry>> groups = new EnumMap<>(ApparelGroup.class);
    for (ApparelEntry entry : entries) {
      groups.computeIfAbsent(entry.group(), group -> new ArrayList<>()).add(entry);
    }
    groups.values().forEach(group -> group.sort(BY_NAME));
    return groups;
  }

  /**
   * The tree with {@code current} selected and scrolled into view (none if {@code null}); {@code onSelect}
   * gets every newly selected piece, or {@code null} when a group or nothing is selected.
   */
  static @NonNull TreeView<ApparelTreeNode> build(@NonNull List<ApparelEntry> entries, @Nullable ApparelEntry current,
      @NonNull Consumer<@Nullable ApparelEntry> onSelect) {
    return GroupedTree.<ApparelTreeNode, ApparelGroup, ApparelEntry>build(grouped(entries),
        entry -> current != null && entry.index() == current.index(), ApparelEntry.class, ApparelTreeCell::new,
        onSelect);
  }

  /** Every location the piece can cover on this body, whether or not its limb is chosen yet. */
  private static @NonNull Set<HitLocation> coverable(@NonNull WornApparel item, @NonNull List<HitLocation> body) {
    CustomApparel custom = item.custom();
    if (custom != null) {
      return custom.locations();
    }
    Set<HitLocation> locations = EnumSet.noneOf(HitLocation.class);
    for (Coverage coverage : item.data().map(ApparelData::coverage).orElse(List.of())) {
      locations.addAll(coverage.locationsOn(body));
    }
    return locations;
  }
}
