package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponKind;
import de.fallout2d20.charbuilder.view.Layouts;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javafx.scene.control.CheckBox;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * One check box per constant of an enum, in declaration order: the mod slots a player gives a custom weapon (user
 * decision 2026-10-17) and its kinds for perks (user decision 2026-10-31).
 *
 * @param <E> the enum
 */
public final class EnumCheckBoxes<E extends Enum<E>> {

  private static final int MOD_SLOT_COLUMNS = 4;

  private final @NonNull Class<E> type;
  private final @NonNull Map<E, CheckBox> boxes;
  private final @NonNull MigPane pane;

  /**
   * @param type the enum class
   * @param selected the constants ticked at the start
   * @param label the check box text of a constant
   * @param columns check boxes per row
   */
  public EnumCheckBoxes(@NonNull Class<E> type, @NonNull Set<E> selected, @NonNull Function<E, String> label,
      int columns) {
    this.type = type;
    this.boxes = new EnumMap<>(type);
    this.pane = Layouts.wideTable(columns);
    for (E constant : type.getEnumConstants()) {
      CheckBox box = new CheckBox(label.apply(constant));
      box.setSelected(selected.contains(constant));
      boxes.put(constant, box);
      pane.add(box);
    }
  }

  /** The mod slots of a custom weapon, four per row. */
  public static @NonNull EnumCheckBoxes<ModSlot> modSlots(@NonNull Set<ModSlot> selected) {
    return new EnumCheckBoxes<>(ModSlot.class, selected, ModSlot::displayName, MOD_SLOT_COLUMNS);
  }

  /** The kinds of a custom weapon, in one row. */
  public static @NonNull EnumCheckBoxes<WeaponKind> weaponKinds(@NonNull Set<WeaponKind> selected) {
    return new EnumCheckBoxes<>(WeaponKind.class, selected, WeaponKind::displayName, WeaponKind.values().length);
  }

  public @NonNull MigPane node() {
    return pane;
  }

  public @NonNull Set<E> selected() {
    Set<E> ticked = EnumSet.noneOf(type);
    boxes.forEach((constant, box) -> {
      if (box.isSelected()) {
        ticked.add(constant);
      }
    });
    return ticked;
  }

  /** Calls {@code action} whenever a box is ticked or cleared. */
  public void onChange(@NonNull Runnable action) {
    boxes.values().forEach(box -> box.selectedProperty().addListener((observable, before, now) -> action.run()));
  }
}
