package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.LegacyImport;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Gear tab: starting equipment (pack, Mister Handy arms), caps, materials, carry load, the pack button,
 * inventory notes, the items, and for an imported character where its legacy equipment text went.
 */
final class GearTab {

  private GearTab() {}

  /** @param hints the hints of this tab */
  static @NonNull Node build(@NonNull Character character, @NonNull List<CreationProblem> hints,
      @NonNull ViewCallbacks callbacks) {
    MigPane pane = OverviewParts.grid();
    OverviewParts.addHints(pane, hints);
    OverviewParts.heading(pane, "Equipment");
    EquipmentSection.addTo(pane, character, callbacks.onEdited(), callbacks.onChanged());
    OverviewParts.heading(pane, "Inventory");
    ItemSection.addTo(pane, character, callbacks.onEdited(), callbacks.onChanged());
    LegacyImport legacy = character.getLegacyImport();
    if (legacy != null) {
      OverviewParts.heading(pane, "Legacy data (imported " + legacy.importedOn() + ")");
      pane.add(new Label("The equipment text is in the inventory notes."), "span 4");
    }
    return pane;
  }
}
