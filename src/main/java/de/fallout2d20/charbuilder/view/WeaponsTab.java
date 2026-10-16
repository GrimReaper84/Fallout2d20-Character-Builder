package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Character;
import javafx.scene.Node;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/** Weapons tab: the weapon table with the add button and the ammunition per type. */
final class WeaponsTab {

  private WeaponsTab() {}

  static @NonNull Node build(@NonNull Character character, @NonNull ViewCallbacks callbacks) {
    MigPane pane = OverviewParts.grid();
    WeaponSection.addTo(pane, character, callbacks.onEdited(), callbacks.onChanged());
    return pane;
  }
}
