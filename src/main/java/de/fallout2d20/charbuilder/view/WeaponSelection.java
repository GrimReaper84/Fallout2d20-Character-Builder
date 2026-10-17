package de.fallout2d20.charbuilder.view;

import org.jspecify.annotations.Nullable;

/**
 * The weapon selected in the Weapons tab and the scroll positions of its two sides. Held by the character
 * view, so they survive the rebuild after every change; never saved. Starts on the Unarmed Strike.
 */
final class WeaponSelection {

  /** The index of the Unarmed Strike, which is no carried weapon. */
  static final int UNARMED_STRIKE_INDEX = -1;

  private @Nullable Integer index = UNARMED_STRIKE_INDEX;
  private double listScroll;
  private double detailsScroll;

  /** Selects the carried weapon with this index, or {@link #UNARMED_STRIKE_INDEX}. */
  void select(int newIndex) {
    index = newIndex;
  }

  void clear() {
    index = null;
  }

  /**
   * The selected index if it still names a weapon of a list of {@code weaponCount} weapons (or the
   * Unarmed Strike); {@code null} when nothing or a removed weapon is selected.
   */
  @Nullable Integer current(int weaponCount) {
    return index == null || index >= weaponCount ? null : index;
  }

  /** Scroll position (0–1) of the left side with the weapon tree. */
  double listScroll() {
    return listScroll;
  }

  void setListScroll(double position) {
    listScroll = position;
  }

  /** Scroll position (0–1) of the right side with the selected weapon's values and mods. */
  double detailsScroll() {
    return detailsScroll;
  }

  void setDetailsScroll(double position) {
    detailsScroll = position;
  }
}
