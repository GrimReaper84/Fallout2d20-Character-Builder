package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import de.fallout2d20.charbuilder.model.character.derived.Resistance;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Status tab: the derived statistics with the innate Damage Resistances and, once creation is complete,
 * the play-session values (D5).
 */
final class StatusTab {

  private static final @NonNull String RESISTANCE_TOOLTIP =
      "Innate resistance from origin and perks; with apparel see Damage resistance in the Armor tab.";
  private static final @NonNull String PHYSICAL_RESISTANCE_TOOLTIP =
      RESISTANCE_TOOLTIP + " A Barbarian bonus does not apply while wearing Power Armor.";

  private StatusTab() {}

  static @NonNull Node build(@NonNull Character character, @NonNull ViewCallbacks callbacks) {
    MigPane pane = OverviewParts.grid();
    if (character.isCreationComplete()) {
      OverviewParts.heading(pane, "Session");
      SessionSection.addTo(pane, character, callbacks.onEdited(), callbacks.onChanged());
    }

    DerivedStats derived = DerivedStats.of(character);
    OverviewParts.heading(pane, "Derived statistics");
    pane.add(new Label("Max HP"));
    pane.add(new Label(Integer.toString(derived.maxHp())));
    pane.add(new Label("Initiative"));
    pane.add(new Label(Integer.toString(derived.initiative())));
    pane.add(new Label("Defense"));
    pane.add(new Label(Integer.toString(derived.defense())));
    pane.add(new Label("Carry weight"));
    pane.add(new Label(derived.carryWeight() + " lbs."));
    pane.add(new Label("Melee damage"));
    pane.add(new Label(derived.meleeDamageText()));
    pane.add(new Label("Max Luck points"));
    pane.add(new Label(Integer.toString(derived.maxLuckPoints())));
    resistance(pane, "Physical DR", derived.physical(), PHYSICAL_RESISTANCE_TOOLTIP);
    resistance(pane, "Energy DR", derived.energy(), RESISTANCE_TOOLTIP);
    resistance(pane, "Radiation DR", derived.radiation(), RESISTANCE_TOOLTIP);
    resistance(pane, "Poison DR", derived.poison(), RESISTANCE_TOOLTIP);
    return pane;
  }

  private static void resistance(@NonNull MigPane pane, @NonNull String text,
      @NonNull Resistance resistance, @NonNull String tooltip) {
    Label label = new Label(text);
    label.setTooltip(new Tooltip(tooltip));
    pane.add(label);
    pane.add(new Label(resistance.toString()));
  }
}
