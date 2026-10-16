package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Character;
import javafx.scene.Node;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/** Armor tab: worn and carried apparel, Damage Resistance per hit location and the apparel hints. */
final class ArmorTab {

  private ArmorTab() {}

  static @NonNull Node build(@NonNull Character character, @NonNull ViewCallbacks callbacks) {
    MigPane pane = OverviewParts.grid();
    ApparelSection.addTo(pane, character, callbacks.onEdited(), callbacks.onChanged());
    return pane;
  }
}
