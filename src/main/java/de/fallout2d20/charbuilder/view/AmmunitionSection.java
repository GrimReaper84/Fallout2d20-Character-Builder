package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.weapon.AmmunitionData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponAttack;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The shots per ammunition type (CRB p. 91), below the weapon tree: one row per type a carried weapon uses
 * or that has shots, with an editable number of shots; a combo box adds another type.
 */
final class AmmunitionSection {

  private AmmunitionSection() {}

  static @NonNull MigPane build(@NonNull PlayerCharacter character, @NonNull Runnable onChanged) {
    Set<String> shown = new LinkedHashSet<>();
    for (CarriedWeapon weapon : character.getWeapons()) {
      shown.addAll(WeaponAttack.of(character, weapon).ammunition());
    }
    shown.addAll(character.getAmmunition().keySet());
    MigPane rows = new MigPane("insets 0, gap 10 4, wrap 2", "[grow, left][right]");
    for (String id : shown) {
      Label name = new Label(ammunitionName(id));
      WeaponCatalog.ammunitionById(id).ifPresent(data -> name.setTooltip(new Tooltip("Found: "
          + OverviewParts.orDash(data.quantityFound()) + ", rarity " + OverviewParts.orDash(data.rarity())
          + " (" + data.sources().getFirst() + ")")));
      rows.add(name);
      rows.add(NumberField.of(character.ammunition(id), 0, shots -> {
        character.setAmmunition(id, shots);
        onChanged.run();
      }));
    }
    List<AmmunitionData> others = WeaponCatalog.ammunition().stream()
        .filter(data -> !shown.contains(data.id()) && data.isAvailableWith(character.getEnabledBooks())).toList();
    ComboBox<AmmunitionData> add = new ComboBox<>();
    add.getItems().setAll(others);
    add.setConverter(new DisplayConverter<>(AmmunitionData::name));
    add.setPromptText("Add ammunition type");
    add.setMaxWidth(Double.MAX_VALUE);
    add.setOnAction(event -> {
      AmmunitionData chosen = add.getValue();
      if (chosen != null) {
        character.setAmmunition(chosen.id(), 1);
        onChanged.run();
      }
    });
    rows.add(add, "span 2, growx");
    return rows;
  }

  /** The catalog name of an ammunition type; the id if the catalog does not know it. */
  static @NonNull String ammunitionName(@NonNull String id) {
    return WeaponCatalog.ammunitionById(id).map(AmmunitionData::name).orElse(id);
  }
}
