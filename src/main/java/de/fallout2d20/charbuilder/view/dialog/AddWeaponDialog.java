package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.weapon.AmmunitionData;
import de.fallout2d20.charbuilder.catalog.weapon.DamageType;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.CustomWeapon;
import de.fallout2d20.charbuilder.view.DisplayConverter;
import de.fallout2d20.charbuilder.view.Layouts;
import de.fallout2d20.charbuilder.view.WholeNumber;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Picks a weapon to add: a catalog weapon of the enabled books (searchable by name) or a custom weapon
 * (homebrew, books not catalogued yet) with free-text effects, damage type and qualities and the mod slots
 * the player gives it.
 */
public final class AddWeaponDialog {

  private AddWeaponDialog() {}

  /**
   * Shows the dialog; empty if cancelled.
   *
   * @param weapons the catalog weapons to offer (the caller leaves out the Unarmed Strike every character has)
   */
  public static @NonNull Optional<CarriedWeapon> show(@Nullable Window owner, @NonNull List<WeaponData> weapons) {
    FilteredList<WeaponData> filtered = new FilteredList<>(FXCollections.observableArrayList(weapons));
    TextField search = new TextField();
    search.setPromptText("Search");
    search.textProperty().addListener((_, _, text) -> {
      String key = text.strip().toLowerCase(Locale.ROOT);
      filtered.setPredicate(weapon -> weapon.name().toLowerCase(Locale.ROOT).contains(key));
    });
    ListView<WeaponData> list = new ListView<>(filtered);
    list.setCellFactory(TextFieldListCell.forListView(new DisplayConverter<>(AddWeaponDialog::describe)));
    list.setPrefSize(460, 360);
    Tab catalog = new Tab("Catalog", new VBox(6, search, list));

    TextField name = new TextField();
    ComboBox<@Nullable Skill> skill = combo(Skill.weaponSkills(), Skill::displayName);
    TextField damage = new TextField();
    TextField effects = new TextField();
    ComboBox<@Nullable DamageType> damageType = combo(Arrays.asList(DamageType.values()), DamageType::displayName);
    damageType.setValue(DamageType.PHYSICAL);
    TextField fireRate = new TextField();
    ComboBox<@Nullable WeaponRange> range = combo(Arrays.asList(WeaponRange.values()), WeaponRange::displayName);
    TextField qualities = new TextField();
    ComboBox<@Nullable AmmunitionData> ammunition = combo(WeaponCatalog.ammunition(), AmmunitionData::name);
    MigPane form = Layouts.dialogForm();
    addRow(form, "Name", name);
    addRow(form, "Skill", skill);
    addRow(form, "Damage (CD)", damage);
    addRow(form, "Damage effects", effects);
    addRow(form, "Damage type", damageType);
    addRow(form, "Fire rate", fireRate);
    addRow(form, "Range", range);
    addRow(form, "Qualities", qualities);
    addRow(form, "Ammunition", ammunition);
    ModSlotCheckBoxes modSlots = new ModSlotCheckBoxes(Set.of());
    form.add(new Label("Mod slots"), Layouts.top());
    form.add(modSlots.node());
    Tab custom = new Tab("Custom weapon", form);

    TabPane tabs = new TabPane(catalog, custom);
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    Dialog<CarriedWeapon> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle("Add weapon");
    dialog.setHeaderText("Choose a weapon from the catalog or describe a custom one.");
    dialog.getDialogPane().setContent(tabs);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    Node ok = dialog.getDialogPane().lookupButton(ButtonType.OK);
    ok.disableProperty().bind(Bindings.createBooleanBinding(
        () -> catalog.isSelected() ? list.getSelectionModel().getSelectedItem() == null
            : name.getText().isBlank() || number(damage.getText()) == null || !validFireRate(fireRate.getText()),
        catalog.selectedProperty(), list.getSelectionModel().selectedItemProperty(), name.textProperty(),
        damage.textProperty(), fireRate.textProperty()));
    dialog.setResultConverter(button -> {
      if (button != ButtonType.OK) {
        return null;
      }
      if (catalog.isSelected()) {
        WeaponData selected = list.getSelectionModel().getSelectedItem();
        return selected == null ? null : CarriedWeapon.of(CatalogRef.of(selected.id()));
      }
      Integer rating = number(damage.getText());
      DamageType type = damageType.getValue();
      AmmunitionData ammo = ammunition.getValue();
      return CarriedWeapon.of(new CustomWeapon(name.getText(), skill.getValue(), rating == null ? 0 : rating,
          effects.getText(), type == null ? "" : type.displayName(), number(fireRate.getText()), range.getValue(),
          qualities.getText(), ammo == null ? null : ammo.id(), modSlots.selected()));
    });
    return dialog.showAndWait();
  }

  /** One list line, e.g. "10mm Pistol – Small Guns, 4 CD, CRB p. 95". */
  private static @NonNull String describe(@NonNull WeaponData weapon) {
    return weapon.name() + " – " + weapon.skill().displayName() + ", " + weapon.damage() + " CD, "
        + weapon.sources().getFirst();
  }

  /** A whole number of at least 0, or {@code null} for anything else. */
  private static @Nullable Integer number(@NonNull String text) {
    OptionalInt value = WholeNumber.parse(text);
    return value.isPresent() && value.getAsInt() >= 0 ? value.getAsInt() : null;
  }

  /** Empty (no fire rate) or 0–6 (CRB p. 89). */
  private static boolean validFireRate(@NonNull String text) {
    Integer rate = number(text);
    return text.isBlank() || (rate != null && rate <= CustomWeapon.MAX_FIRE_RATE);
  }

  private static void addRow(@NonNull MigPane form, @NonNull String label, @NonNull Node control) {
    form.add(new Label(label));
    form.add(control);
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
