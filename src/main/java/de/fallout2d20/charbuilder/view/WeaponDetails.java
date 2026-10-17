package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponAttack;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The right side of the Weapons tab: every value of the selected weapon ({@link WeaponAttack}: target
 * number, critical range, damage with the melee bonus, effects, type, fire rate, effective range,
 * qualities, ammunition left), the catalog's weight, cost and rarity, a quantity for explosives and thrown
 * weapons and the note (e.g. mods). A custom weapon imported without skill gets a skill choice instead of
 * the attack value. Typed notes go to {@code onEdited}; everything else to the model and then to
 * {@code onChanged}.
 */
final class WeaponDetails {

  private static final int NOTES_ROWS = 4;

  private WeaponDetails() {}

  /** The panel of {@code entry}, or a prompt to select a weapon if it is {@code null}. */
  static @NonNull Node build(@NonNull Character character, @Nullable WeaponEntry entry,
      @NonNull ViewCallbacks callbacks) {
    MigPane pane = OverviewParts.half();
    if (entry == null) {
      pane.add(new Label("Select a weapon on the left."));
      return pane;
    }
    WeaponAttack attack = entry.attack();
    // From the model, not the entry: typed notes change the weapon without a rebuild of the tree's entries.
    CarriedWeapon weapon = entry.weapon() == null ? null : character.getWeapons().get(entry.index());
    Optional<WeaponData> data = weapon == null ? WeaponCatalog.byId(WeaponsTab.UNARMED_STRIKE_ID) : weapon.data();
    OverviewParts.sectionHeading(pane, attack.name());

    MigPane rows = new MigPane("insets 0, gap 16 6, wrap 2", "[left][grow, fill]");
    row(rows, "Source", new Label(data.map(weaponData -> weaponData.sources().getFirst().toString())
        .orElse("Custom weapon")));
    row(rows, "Attack", entry.needsSkill() ? skillChoice(character, entry.index(), callbacks.onChanged())
        : attackLabel(character, attack));
    row(rows, "Damage", new Label(attack.damageText()));
    row(rows, "Effects", text(attack.effects()));
    row(rows, "Damage type", text(attack.damageType()));
    row(rows, "Fire rate", new Label(attack.fireRate() == null ? "–" : attack.fireRate().toString()));
    row(rows, "Range", new Label(rangeText(attack)));
    row(rows, "Qualities", text(attack.qualities()));
    row(rows, "Ammunition", new Label(attack.ammunition().isEmpty() ? "–" : attack.ammunition().stream()
        .map(id -> AmmunitionSection.ammunitionName(id) + ": " + character.ammunition(id)).collect(Collectors.joining(", "))));
    data.ifPresent(weaponData -> {
      boolean integrated = weapon != null && weapon.isIntegrated();
      row(rows, "Weight", new Label(integrated ? "– (built in)"
          : weaponData.weight() == null ? "–" : weaponData.weight() + " lbs."));
      row(rows, "Cost", new Label(OverviewParts.orDash(weaponData.cost())));
      row(rows, "Rarity", new Label(OverviewParts.orDash(weaponData.rarity())));
    });
    if (weapon != null) {
      int index = entry.index();
      if (weapon.isCounted()) {
        row(rows, "Quantity", NumberField.of(weapon.quantity(), 1, quantity -> {
          character.setWeaponQuantity(index, quantity);
          callbacks.onChanged().run();
        }));
      }
      TextArea notes = new TextArea(weapon.notes());
      notes.setPromptText("Notes, e.g. mods");
      notes.setWrapText(true);
      notes.setPrefRowCount(NOTES_ROWS);
      notes.textProperty().addListener((obs, old, text) -> {
        character.setWeaponNotes(index, text);
        callbacks.onEdited().run();
      });
      rows.add(rowName("Notes"), "top");
      rows.add(notes);
    }
    pane.add(rows);
    return pane;
  }

  private static void row(@NonNull MigPane rows, @NonNull String name, @NonNull Node value) {
    rows.add(rowName(name));
    rows.add(value);
  }

  /** A row name that never shrinks below its text (it was cut to "…" while the values took the width). */
  private static @NonNull Label rowName(@NonNull String name) {
    Label label = new Label(name);
    label.setMinWidth(Region.USE_PREF_SIZE);
    return label;
  }

  /** Target number and critical range, with the attribute and skill as tooltip. */
  private static @NonNull Label attackLabel(@NonNull Character character, @NonNull WeaponAttack attack) {
    Label roll = new Label(attack.targetNumber() == null ? "–"
        : "TN " + attack.targetNumber() + ", crit ≤ " + attack.criticalRange());
    Skill skill = attack.skill();
    Attribute attribute = attack.attribute();
    if (skill != null && attribute != null) {
      roll.setTooltip(new Tooltip(attribute.abbreviation() + " + " + skill.displayName()
          + (character.isTagged(skill) ? " (Tag skill)" : "")));
    }
    return roll;
  }

  /** The effective range, marked when it comes from the Thrown quality (CRB p. 90). */
  private static @NonNull String rangeText(@NonNull WeaponAttack attack) {
    WeaponRange range = attack.effectiveRange();
    if (range == null) {
      return "–";
    }
    return range.displayName() + (attack.range() == null ? " (thrown)" : "");
  }

  /** The attack skill of a custom weapon imported without one (the legacy app stored none). */
  private static @NonNull Node skillChoice(@NonNull Character character, int index, @NonNull Runnable onChanged) {
    ComboBox<Skill> box = new ComboBox<>();
    box.getItems().setAll(Skill.weaponSkills());
    box.setConverter(new DisplayConverter<>(Skill::displayName));
    box.setPromptText("Choose skill");
    box.setTooltip(new Tooltip("The skill used to attack with this weapon; it decides the target number."));
    box.setOnAction(event -> {
      Skill chosen = box.getValue();
      if (chosen != null) {
        character.setCustomWeaponSkill(index, chosen);
        onChanged.run();
      }
    });
    return box;
  }

  /** A value that may be long (effects, qualities); wraps at the width of the panel. */
  private static @NonNull Label text(@NonNull String value) {
    return OverviewParts.wrappingInCell(new Label(OverviewParts.orDash(value)));
  }
}
