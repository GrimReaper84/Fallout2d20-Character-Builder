package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.weapon.AmmunitionData;
import de.fallout2d20.charbuilder.catalog.weapon.DamageType;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.weapon.CustomWeapon;
import de.fallout2d20.charbuilder.view.DisplayConverter;
import de.fallout2d20.charbuilder.view.Layouts;
import de.fallout2d20.charbuilder.view.WholeNumber;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The form of a custom weapon (homebrew, books not catalogued yet): name, skill, damage, free-text effects,
 * damage type, fire rate, range, the book's qualities ({@link QualityCheckBoxes}), ammunition and the mod
 * slots the player gives it.
 */
final class CustomWeaponForm {

  private final @NonNull TextField name = new TextField();
  private final @NonNull ComboBox<@Nullable Skill> skill = combo(Skill.weaponSkills(), Skill::displayName);
  private final @NonNull TextField damage = new TextField();
  private final @NonNull TextField effects = new TextField();
  private final @NonNull ComboBox<@Nullable DamageType> damageType =
      combo(Arrays.asList(DamageType.values()), DamageType::displayName);
  private final @NonNull TextField fireRate = new TextField();
  private final @NonNull ComboBox<@Nullable WeaponRange> range =
      combo(Arrays.asList(WeaponRange.values()), WeaponRange::displayName);
  private final @NonNull QualityCheckBoxes qualities = new QualityCheckBoxes("");
  private final @NonNull ComboBox<@Nullable AmmunitionData> ammunition =
      combo(WeaponCatalog.ammunition(), AmmunitionData::name);
  private final @NonNull ModSlotCheckBoxes modSlots = new ModSlotCheckBoxes(Set.of());
  private final @NonNull MigPane form = Layouts.dialogForm();

  CustomWeaponForm() {
    damageType.setValue(DamageType.PHYSICAL);
    addRow("Name", name);
    addRow("Skill", skill);
    addRow("Damage (CD)", damage);
    addRow("Damage effects", effects);
    addRow("Damage type", damageType);
    addRow("Fire rate", fireRate);
    addRow("Range", range);
    form.add(new Label("Qualities"), Layouts.top());
    form.add(qualities.node());
    addRow("Ammunition", ammunition);
    form.add(new Label("Mod slots"), Layouts.top());
    form.add(modSlots.node());
  }

  @NonNull Node node() {
    return form;
  }

  /** What {@link #weapon()} depends on, for the bindings of the dialog. */
  @NonNull List<Observable> dependencies() {
    return List.of(name.textProperty(), damage.textProperty(), fireRate.textProperty(), skill.valueProperty(),
        ammunition.valueProperty());
  }

  @Nullable Skill skill() {
    return skill.getValue();
  }

  @Nullable AmmunitionData ammunition() {
    return ammunition.getValue();
  }

  /** The described weapon; {@code null} while the name is missing or damage or fire rate are invalid. */
  @Nullable CustomWeapon weapon() {
    Integer rating = WholeNumber.atLeast(damage.getText(), 0);
    if (name.getText().isBlank() || rating == null || !validFireRate(fireRate.getText())) {
      return null;
    }
    DamageType type = damageType.getValue();
    AmmunitionData ammo = ammunition.getValue();
    return new CustomWeapon(name.getText(), skill.getValue(), rating, effects.getText(),
        type == null ? "" : type.displayName(), WholeNumber.atLeast(fireRate.getText(), 0), range.getValue(), qualities.text(),
        ammo == null ? null : ammo.id(), modSlots.selected());
  }

  private void addRow(@NonNull String label, @NonNull Node control) {
    form.add(new Label(label));
    form.add(control);
  }

  /** Empty (no fire rate) or 0–6 (CRB p. 89). */
  private static boolean validFireRate(@NonNull String text) {
    return text.isBlank() || WholeNumber.between(text, 0, CustomWeapon.MAX_FIRE_RATE) != null;
  }

  /** A combo box with an empty first entry ("–", none) and the options shown by name. */
  private static <T> @NonNull ComboBox<@Nullable T> combo(@NonNull List<T> options,
      @NonNull Function<T, String> text) {
    List<@Nullable T> items = new ArrayList<>();
    items.add(null);
    items.addAll(options);
    ComboBox<@Nullable T> box = new ComboBox<>(FXCollections.observableArrayList(items));
    box.setConverter(new DisplayConverter<@Nullable T>(value -> value == null ? "–" : text.apply(value)));
    box.setMaxWidth(Double.MAX_VALUE);
    return box;
  }
}
