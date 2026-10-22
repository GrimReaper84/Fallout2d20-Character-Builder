package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.apparel.ArmorResistances;
import de.fallout2d20.charbuilder.model.character.apparel.LocationResistance;
import de.fallout2d20.charbuilder.model.character.apparel.ShadowedArmor;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The top of the Armor tab's right side: the damage resistance per hit location of the whole character
 * ({@link ArmorResistances}, CRB p. 130, 146: worn apparel plus innate), the Sneak bonus of worn Shadowed armor
 * ({@link ShadowedArmor}, CRB p. 133) and the tab's hints (the apparel hints collected once by the character view).
 */
final class ApparelResistances {

  private ApparelResistances() {}

  static void addTo(@NonNull MigPane pane, @NonNull PlayerCharacter character, @NonNull List<CreationProblem> hints) {
    OverviewParts.sectionHeading(pane, "Damage resistance");
    MigPane table = Layouts.wideTable(5);
    for (String header : List.of("Location", "Physical", "Energy", "Radiation", "Poison")) {
      table.add(bold(header));
    }
    for (LocationResistance location : ArmorResistances.of(character)) {
      table.add(new Label(location.location().displayName() + " (" + location.location().dieRange() + ")"));
      table.add(new Label(location.physical().toString()));
      table.add(new Label(location.energy().toString()));
      table.add(new Label(location.radiation().toString()));
      table.add(new Label(location.poison().toString()));
    }
    Tooltip.install(table, new Tooltip("Worn apparel plus innate resistances from origin and perks."));
    pane.add(table);
    int shadowed = ShadowedArmor.count(character);
    if (shadowed > 0) {
      Label line = new Label("Shadowed armor (" + shadowed + (shadowed == 1 ? " piece" : " pieces") + "): "
          + ShadowedArmor.effect(shadowed));
      line.setTooltip(new Tooltip("CRB p. 133: a torso piece counts as two; worn pieces only."));
      pane.add(OverviewParts.wrappingInCell(line), Layouts.spaced(Layouts.cell().growX()));
    }
    if (!hints.isEmpty()) {
      pane.add(OverviewParts.wrappingInCell(OverviewParts.hintLabel(hints)), Layouts.spaced(Layouts.cell().growX()));
    }
  }

  private static @NonNull Label bold(@NonNull String text) {
    Label label = new Label(text);
    label.setStyle("-fx-font-weight: bold;");
    return label;
  }
}
