package de.fallout2d20.charbuilder.view;

import static de.fallout2d20.charbuilder.view.OverviewParts.detailRow;

import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.ModdedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponAttack;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The right side of the Weapons tab: every value of the selected weapon ({@link WeaponAttack}: target
 * number, critical range, damage with the melee bonus, effects, type, fire rate, effective range,
 * qualities, ammunition left), the catalog's weight, cost and rarity, a quantity for explosives and thrown
 * weapons, the note and the mods ({@link WeaponModSection}); values the mods changed are bold with the value
 * without mods as tooltip. A custom weapon imported without skill gets a skill choice instead of
 * the attack value. Typed notes go to {@code onEdited}; everything else to the model and then to
 * {@code onChanged}.
 */
final class WeaponDetails {

  private WeaponDetails() {}

  /** The panel of {@code entry}, or a prompt to select a weapon if it is {@code null}. */
  static @NonNull Node build(@NonNull PlayerCharacter character, @Nullable WeaponEntry entry,
      @NonNull ViewCallbacks callbacks) {
    MigPane pane = Layouts.column();
    if (entry == null) {
      pane.add(new Label("Select a weapon on the left."));
      return pane;
    }
    WeaponAttack attack = entry.attack();
    // From the model, not the entry: typed notes change the weapon without a rebuild of the tree's entries.
    CarriedWeapon weapon = entry.weapon() == null ? null : character.gear().getWeapons().get(entry.index());
    Optional<WeaponData> data = weapon == null ? WeaponCatalog.byId(WeaponsTab.UNARMED_STRIKE_ID) : weapon.data();
    ModdedWeapon values = weapon == null ? data.map(ModdedWeapon::of).orElse(null)
        : ModdedWeapon.of(weapon).orElse(null);
    ModdedWeapon base = weapon == null ? values : ModdedWeapon.unmodded(weapon).orElse(null);
    OverviewParts.sectionHeading(pane, attack.name());

    MigPane rows = Layouts.details(0);
    detailRow(rows, "Source", new Label(data.map(weaponData -> weaponData.sources().getFirst().toString())
        .orElse("Custom weapon")));
    if (!attack.moddedName().equals(attack.name())) {
      detailRow(rows, "Modded name", text(attack.moddedName()));
    }
    detailRow(rows, "Attack", entry.needsSkill() ? skillChoice(character, entry.index(), callbacks.onChanged())
        : attackLabel(character, attack));
    detailRow(rows, "Damage", withBase(new Label(attack.damageText()), base, values,
        modded -> modded.damage() + " CD"));
    detailRow(rows, "Effects", withBase(text(attack.effects()), base, values,
        modded -> String.join(", ", modded.damageEffects())));
    detailRow(rows, "Damage type", withBase(text(attack.damageType()), base, values, ModdedWeapon::damageType));
    detailRow(rows, "Fire rate", withBase(new Label(attack.fireRate() == null ? "–" : attack.fireRate().toString()),
        base, values, modded -> OverviewParts.orDash(modded.fireRate())));
    detailRow(rows, "Range", withBase(new Label(rangeText(attack)), base, values,
        modded -> modded.range() == null ? "–" : modded.range().displayName()));
    detailRow(rows, "Qualities", withRemoved(withBase(text(attack.qualities()), base, values,
        modded -> String.join(", ", modded.qualities())), values));
    detailRow(rows, "Ammunition", withBase(new Label(attack.ammunition().isEmpty() ? "–" : attack.ammunition().stream()
        .map(id -> AmmunitionSection.ammunitionName(id) + ": " + character.gear().ammunition(id))
        .collect(Collectors.joining(", "))), base, values, modded -> String.join(", ", modded.ammunition())));
    if (values != null && (data.isPresent() || values.weight() != null)) {
      boolean integrated = weapon != null && weapon.isIntegrated();
      Label weight = new Label(values.weight() == null ? "–" : values.weight() + " lbs.");
      // A built-in weapon weighs nothing, with or without mods.
      detailRow(rows, "Weight", integrated ? new Label("– (built in)")
          : withBase(weight, base, values, modded -> OverviewParts.orDash(modded.weight())));
      detailRow(rows, "Cost", withBase(new Label(OverviewParts.orDash(values.cost())), base, values,
          modded -> OverviewParts.orDash(modded.cost())));
    }
    data.ifPresent(weaponData -> detailRow(rows, "Rarity", new Label(OverviewParts.orDash(weaponData.rarity()))));
    if (weapon != null) {
      int index = entry.index();
      if (weapon.isCounted()) {
        detailRow(rows, "Quantity", NumberField.of(weapon.quantity(), 1, quantity -> {
          character.gear().setWeaponQuantity(index, quantity);
          callbacks.onChanged().run();
        }));
      }
      OverviewParts.notesRow(rows, weapon.notes(), text -> {
        character.gear().setWeaponNotes(index, text);
        callbacks.onEdited().run();
      });
    }
    pane.add(rows);
    if (weapon != null && values != null) {
      WeaponModSection.addTo(pane, character, entry.index(), weapon, values, callbacks.onChanged());
    }
    return pane;
  }

  /**
   * Marks a value the mods changed: bold, with the value without mods as tooltip. {@code text} renders both
   * sides alike, so the comparison ignores the melee bonus and ammunition counts of the shown text.
   */
  private static @NonNull Label withBase(@NonNull Label label, @Nullable ModdedWeapon base,
      @Nullable ModdedWeapon values, @NonNull Function<ModdedWeapon, String> text) {
    if (base != null && values != null && !text.apply(base).equals(text.apply(values))) {
      label.setStyle("-fx-font-weight: bold;");
      label.setTooltip(new Tooltip("Without mods: " + text.apply(base)));
    }
    return label;
  }

  /** Adds the qualities the mods removed or kept away to the tooltip (house rule: a removal wins). */
  private static @NonNull Label withRemoved(@NonNull Label label, @Nullable ModdedWeapon values) {
    if (values == null || values.removedQualities().isEmpty()) {
      return label;
    }
    Tooltip tooltip = label.getTooltip();
    String removed = String.join("\n", values.removedQualities());
    label.setTooltip(new Tooltip(tooltip == null ? removed : tooltip.getText() + "\n" + removed));
    return label;
  }

  /** Target number and critical range, with the attribute and skill as tooltip. */
  private static @NonNull Label attackLabel(@NonNull PlayerCharacter character, @NonNull WeaponAttack attack) {
    Label roll = new Label(attack.targetNumber() == null ? "–"
        : "TN " + attack.targetNumber() + ", crit ≤ " + attack.criticalRange());
    Skill skill = attack.skill();
    Attribute attribute = attack.attribute();
    if (skill != null && attribute != null) {
      roll.setTooltip(new Tooltip(attribute.abbreviation() + " + " + skill.displayName()
          + (character.abilities().isTagged(skill) ? " (Tag skill)" : "")));
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
  private static @NonNull Node skillChoice(@NonNull PlayerCharacter character, int index, @NonNull Runnable onChanged) {
    ComboBox<Skill> box = new ComboBox<>();
    box.getItems().setAll(Skill.weaponSkills());
    box.setConverter(new DisplayConverter<>(Skill::displayName));
    box.setPromptText("Choose skill");
    box.setTooltip(new Tooltip("The skill used to attack with this weapon; it decides the target number."));
    box.setOnAction(event -> {
      Skill chosen = box.getValue();
      if (chosen != null) {
        character.gear().setCustomWeaponSkill(index, chosen);
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
