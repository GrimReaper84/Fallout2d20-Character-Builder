package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.derived.CarryLoad;
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
    MigPane left = Layouts.column();
    OverviewParts.sectionHeading(left, "Session");
    if (character.isCreationComplete()) {
      left.add(SessionSection.build(character, callbacks.onChanged()));
    } else {
      left.add(new Label("Tracked once the character is created."));
    }

    MigPane right = Layouts.column();
    OverviewParts.sectionHeading(right, "Derived statistics");
    right.add(derivedStatistics(character));
    return Layouts.halves(left, right);
  }

  /** Name and value per row, one below the other. */
  private static @NonNull MigPane derivedStatistics(@NonNull PlayerCharacter character) {
    DerivedStats derived = DerivedStats.of(character);
    MigPane pane = Layouts.table(2);
    row(pane, new Label("Max HP"), Integer.toString(derived.maxHp()));
    initiative(pane, derived.initiative(), CarryLoad.of(character));
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

  /** The Initiative with the load; when the load lowers it, the reason as tooltip (CRB p. 87). */
  private static void initiative(@NonNull MigPane pane, int base, @NonNull CarryLoad load) {
    int initiative = load.initiative(base);
    Label value = new Label(Integer.toString(initiative));
    if (initiative < base) {
      String reason = load.immobile() ? "0: carrying twice the carry weight or more"
          : "−" + (base - initiative) + " for carrying too much";
      value.setTooltip(new Tooltip(base + " unencumbered; " + reason + " (CRB p. 87)"));
      value.setStyle(OverviewParts.HINT_STYLE);
    }
    pane.add(new Label("Initiative"));
    pane.add(value);
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
