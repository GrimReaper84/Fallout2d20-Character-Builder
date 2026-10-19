package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import de.fallout2d20.charbuilder.model.character.derived.Resistance;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Status tab in two halves of equal width: the play-session values (D5, once creation is complete) on the
 * left, the derived statistics with the innate Damage Resistances on the right.
 */
final class StatusTab {

  private static final @NonNull String RESISTANCE_TOOLTIP =
      "Innate resistance from origin and perks; with apparel see Damage resistance in the Armor tab.";
  private static final @NonNull String PHYSICAL_RESISTANCE_TOOLTIP =
      RESISTANCE_TOOLTIP + " A Barbarian bonus does not apply while wearing Power Armor.";

  private StatusTab() {}

  static @NonNull Node build(@NonNull PlayerCharacter character, @NonNull ViewCallbacks callbacks) {
    MigPane left = OverviewParts.half();
    OverviewParts.sectionHeading(left, "Session");
    if (character.isCreationComplete()) {
      left.add(SessionSection.build(character, callbacks.onChanged()));
    } else {
      left.add(new Label("Tracked once the character is created."));
    }

    MigPane right = OverviewParts.half();
    OverviewParts.sectionHeading(right, "Derived statistics");
    right.add(derivedStatistics(character));
    return OverviewParts.halves(left, right);
  }

  /** Name and value per row, one below the other. */
  private static @NonNull MigPane derivedStatistics(@NonNull PlayerCharacter character) {
    DerivedStats derived = DerivedStats.of(character);
    MigPane pane = new MigPane("insets 0, gap 8 4, wrap 2", "[left][left]");
    row(pane, new Label("Max HP"), Integer.toString(derived.maxHp()));
    row(pane, new Label("Initiative"), Integer.toString(derived.initiative()));
    row(pane, new Label("Defense"), Integer.toString(derived.defense()));
    row(pane, new Label("Carry weight"), derived.carryWeight() + " lbs.");
    row(pane, new Label("Melee damage"), derived.meleeDamageText());
    row(pane, new Label("Max Luck points"), Integer.toString(derived.maxLuckPoints()));
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
    row(pane, label, resistance.toString());
  }

  private static void row(@NonNull MigPane pane, @NonNull Label name, @NonNull String value) {
    pane.add(name);
    pane.add(new Label(value));
  }
}
