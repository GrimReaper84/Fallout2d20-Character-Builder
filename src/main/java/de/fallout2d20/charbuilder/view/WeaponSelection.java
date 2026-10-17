package de.fallout2d20.charbuilder.view;

import org.jspecify.annotations.Nullable;

/**
 * The weapon selected in the Weapons tab. Held by the character view, so the selection survives the
 * rebuild after every change; never saved. Starts on the Unarmed Strike.
 */
final class WeaponSelection {

  /** The index of the Unarmed Strike, which is no carried weapon. */
  static final int UNARMED_STRIKE_INDEX = -1;

  private @Nullable Integer index = UNARMED_STRIKE_INDEX;

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
}
