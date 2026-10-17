package de.fallout2d20.charbuilder.view;

import org.jspecify.annotations.Nullable;

/**
 * The selected entry of a master/detail tab (Weapons, Armor) and the scroll positions of its two sides.
 * Held by the character view, so they survive the rebuild after every change; never saved.
 */
final class MasterDetailState {

  private @Nullable Integer index;
  private double listScroll;
  private double detailsScroll;

  /** Starts with {@code initial} selected; {@code null} for nothing. */
  MasterDetailState(@Nullable Integer initial) {
    index = initial;
  }

  /** Selects the entry with this list index (a tab may use negative indexes for fixed entries). */
  void select(int newIndex) {
    index = newIndex;
  }

  void clear() {
    index = null;
  }

  /**
   * The selected index if it still names an entry of a list of {@code count} entries (or a fixed entry
   * below 0); {@code null} when nothing or a removed entry is selected.
   */
  @Nullable Integer current(int count) {
    return index == null || index >= count ? null : index;
  }

  /** Scroll position (0–1) of the left side with the tree. */
  double listScroll() {
    return listScroll;
  }

  void setListScroll(double position) {
    listScroll = position;
  }

  /** Scroll position (0–1) of the right side with the details. */
  double detailsScroll() {
    return detailsScroll;
  }

  void setDetailsScroll(double position) {
    detailsScroll = position;
  }
}
