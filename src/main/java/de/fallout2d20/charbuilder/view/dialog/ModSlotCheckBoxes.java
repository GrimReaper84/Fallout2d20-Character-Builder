package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.view.Layouts;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javafx.scene.control.CheckBox;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/** One check box per mod slot, for the slots a player gives a custom weapon (user decision 2026-10-17). */
public final class ModSlotCheckBoxes {

  private static final int COLUMNS = 4;

  private final @NonNull Map<ModSlot, CheckBox> boxes = new EnumMap<>(ModSlot.class);
  private final @NonNull MigPane pane = Layouts.wideTable(COLUMNS);

  public ModSlotCheckBoxes(@NonNull Set<ModSlot> selected) {
    for (ModSlot slot : ModSlot.values()) {
      CheckBox box = new CheckBox(slot.displayName());
      box.setSelected(selected.contains(slot));
      boxes.put(slot, box);
      pane.add(box);
    }
  }

  public @NonNull MigPane node() {
    return pane;
  }

  public @NonNull Set<ModSlot> selected() {
    Set<ModSlot> slots = EnumSet.noneOf(ModSlot.class);
    boxes.forEach((slot, box) -> {
      if (box.isSelected()) {
        slots.add(slot);
      }
    });
    return slots;
  }
}
