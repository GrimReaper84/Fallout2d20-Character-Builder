package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.derived.CarryLoad;
import de.fallout2d20.charbuilder.model.character.item.Materials;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import net.miginfocom.layout.AC;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The bar above the Gear tab's tree (user decision 2026-10-19): caps on hand, junk and the three grades of
 * material (CRB p. 208), and the carried load against the carry weight with the encumbrance penalty
 * (CRB p. 87). Every edit goes to the model and then to {@code onChanged}.
 */
final class GearSummary {

  private GearSummary() {}

  static @NonNull Node build(@NonNull PlayerCharacter character, @NonNull Runnable onChanged) {
    MigPane bar = Layouts.row();
    bar.setRowConstraints(new AC().align("top"));
    bar.add(new Label("Caps"));
    TextField caps = NumberField.of((int) Math.min(Integer.MAX_VALUE, character.gear().getCaps()), 0, value -> {
      character.gear().setCaps(value);
      onChanged.run();
    });
    caps.setTooltip(new Tooltip("Caps on hand. The equipment pack fills in the fixed starting caps; rolled caps "
        + "(e.g. Barter 2d20) are a line in the gear notes."));
    bar.add(caps);

    Materials materials = character.gear().getMaterials();
    Label junk = new Label("Junk");
    junk.setTooltip(new Tooltip("CRB p. 208: a junk item weighs 2 lbs. (2 caps); a unit of material 1 lb. "
        + "(common 1, uncommon 3, rare 5 caps)."));
    bar.add(junk, Layouts.gapBefore(2));
    bar.add(NumberField.of(materials.junk(), 0, value -> set(character,
        new Materials(value, materials.common(), materials.uncommon(), materials.rare()), onChanged)));
    bar.add(new Label("Common"), Layouts.gapBefore(1));
    bar.add(NumberField.of(materials.common(), 0, value -> set(character,
        new Materials(materials.junk(), value, materials.uncommon(), materials.rare()), onChanged)));
    bar.add(new Label("Uncommon"), Layouts.gapBefore(1));
    bar.add(NumberField.of(materials.uncommon(), 0, value -> set(character,
        new Materials(materials.junk(), materials.common(), value, materials.rare()), onChanged)));
    bar.add(new Label("Rare"), Layouts.gapBefore(1));
    bar.add(NumberField.of(materials.rare(), 0, value -> set(character,
        new Materials(materials.junk(), materials.common(), materials.uncommon(), value), onChanged)));

    bar.add(new Label("Load"), Layouts.gapBefore(2));
    bar.add(load(character));
    return bar;
  }

  private static void set(@NonNull PlayerCharacter character, @NonNull Materials materials,
      @NonNull Runnable onChanged) {
    character.gear().setMaterials(materials);
    onChanged.run();
  }

  /** CRB p. 87: "143 / 190 lbs." and, when over the carry weight, the penalty below it. */
  private static @NonNull Node load(@NonNull PlayerCharacter character) {
    CarryLoad load = CarryLoad.of(character);
    Label total = new Label(load.carried() + " / " + load.carryWeight() + " lbs.");
    total.setTooltip(new Tooltip("Weapons, worn apparel, items, heavy ammunition, junk and materials; items of "
        + "<1 lb. count nothing (CRB p. 87). Custom weapons and apparel have no weight."));
    if (!load.isOverEncumbered()) {
      return total;
    }
    Label penalty = new Label(load.immobile()
        ? "Twice the carry weight: cannot move, STR and AGI tests fail, Initiative 0 (CRB p. 87)."
        : "Over-encumbered: +" + load.penalty() + " difficulty on STR and AGI tests, no Sprint, Initiative −"
            + load.penalty() + " (CRB p. 87).");
    penalty.setStyle(OverviewParts.HINT_STYLE);
    penalty.setWrapText(true);
    penalty.setMaxWidth(OverviewParts.TEXT_WIDTH / 2);
    return new VBox(2, total, penalty);
  }
}
