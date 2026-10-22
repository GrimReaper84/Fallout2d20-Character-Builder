package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.weapon.AmmunitionData;
import de.fallout2d20.charbuilder.catalog.weapon.DamageType;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.derived.Weight;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.CustomWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponAttack;
import de.fallout2d20.charbuilder.view.DisplayConverter;
import de.fallout2d20.charbuilder.view.Layouts;
import de.fallout2d20.charbuilder.view.WholeNumber;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Adds weapons: catalog weapons of the enabled books ({@link CatalogPage}, by the range groups of the
 * Weapons tree) or custom weapons ({@link CustomWeaponForm}). Explosives and thrown weapons come in the
 * chosen quantity (CRB p. 27); rounds of the weapon's ammunition can be added along with it.
 */
public final class AddWeaponDialog {

  /** Upper bound of a typed quantity or number of rounds, so adding to the stock can not overflow. */
  private static final int MAX_TYPED = 9999;

  private AddWeaponDialog() {}

  /**
   * Shows the dialog until it is closed; every add goes to {@code onAdd} at once.
   *
   * @param weapons the catalog weapons to offer (the caller leaves out the Unarmed Strike every character has)
   * @param categoryOf the Weapons tree group of a catalog weapon
   * @param categories the Weapons tree groups in tree order
   * @param onAdd enters a weapon and its rounds and returns the weapon's index
   * @return the index of the last added weapon; empty if none was added
   */
  public static @NonNull OptionalInt show(@Nullable Window owner, @NonNull List<WeaponData> weapons,
      @NonNull Function<WeaponData, String> categoryOf, @NonNull List<String> categories,
      @NonNull ToIntFunction<WeaponPick> onAdd) {
    CatalogPage<WeaponData> page = new CatalogPage<>(weapons, WeaponData::name, categoryOf, categories,
        AddWeaponDialog::details);
    page.addColumn("Skill", weapon -> weapon.skill().displayName());
    page.addNumberColumn("Damage", WeaponData::damage,
        weapon -> weapon.specialDamage() ? WeaponAttack.SPECIAL_DAMAGE : weapon.damage() + " CD");
    page.addColumn("Range", AddWeaponDialog::rangeText);
    page.addNumberColumn("Weight", weapon -> Weight.pounds(weapon.weight()),
        weapon -> weapon.weight() == null ? "–" : weapon.weight());
    page.addColumn("Source", weapon -> weapon.sources().getFirst().toString());
    Tab catalog = new Tab("Catalog", page.node());
    CustomWeaponForm form = new CustomWeaponForm();
    Tab custom = new Tab("Custom weapon", form.node());
    TabPane tabs = new TabPane(catalog, custom);
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

    TextField quantity = new TextField("1");
    quantity.setPrefColumnCount(4);
    TextField rounds = new TextField("0");
    rounds.setPrefColumnCount(4);
    ComboBox<AmmunitionData> ammunition = new ComboBox<>();
    ammunition.setConverter(new DisplayConverter<>(AmmunitionData::name));
    List<Observable> dependencies = new ArrayList<>(List.of(catalog.selectedProperty(), page.selectedItemProperty()));
    dependencies.addAll(form.dependencies());
    Observable[] all = dependencies.toArray(Observable[]::new);
    BooleanBinding counted = Bindings.createBooleanBinding(() -> skill(catalog, page, form)
        .map(Skill::isCountedWeapon).orElse(false), all);
    quantity.disableProperty().bind(counted.not());
    Runnable offerAmmunition = () -> {
      List<AmmunitionData> types = ammunitionOf(catalog.isSelected(), page.selected(), form.ammunition());
      if (types.equals(ammunition.getItems())) {
        return;
      }
      // Rounds typed for another weapon are not meant for this one.
      rounds.setText("0");
      ammunition.getItems().setAll(types);
      ammunition.setValue(types.isEmpty() ? null : types.getFirst());
    };
    for (Observable dependency : all) {
      dependency.addListener(_ -> offerAmmunition.run());
    }
    rounds.disableProperty().bind(Bindings.isEmpty(ammunition.getItems()));
    ammunition.disableProperty().bind(Bindings.isEmpty(ammunition.getItems()));
    rounds.setTooltip(new Tooltip("Rounds added to what the character already has; 0 adds none."));
    MigPane extras = Layouts.row();
    extras.add(new Label("Quantity"));
    extras.add(quantity);
    extras.add(new Label("Also add"), Layouts.gapBefore(2));
    extras.add(rounds);
    extras.add(new Label("rounds of"));
    extras.add(ammunition);

    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle("Add weapon");
    dialog.setHeaderText("Choose weapons from the catalog or describe a custom one.");
    AtomicInteger last = new AtomicInteger(-1);
    List<Observable> inputs = new ArrayList<>(dependencies);
    inputs.addAll(List.of(quantity.textProperty(), rounds.textProperty(), ammunition.valueProperty(), counted));
    Label status = AddButtons.install(dialog, Bindings.createBooleanBinding(
        () -> pick(catalog, page, form, quantity, rounds, ammunition, counted.get()) == null,
        inputs.toArray(Observable[]::new)), () -> {
          WeaponPick pick = pick(catalog, page, form, quantity, rounds, ammunition, counted.get());
          if (pick == null) {
            return null;
          }
          last.set(onAdd.applyAsInt(pick));
          return describe(pick);
        });
    page.setOnActivate(() -> AddButtons.addNow(dialog));
    dialog.getDialogPane().setContent(new VBox(Layouts.GAP, tabs, extras, status));
    dialog.showAndWait();
    return last.get() < 0 ? OptionalInt.empty() : OptionalInt.of(last.get());
  }

  /** The weapon to add with quantity and rounds; {@code null} while nothing (valid) is chosen. */
  private static @Nullable WeaponPick pick(@NonNull Tab catalog, @NonNull CatalogPage<WeaponData> page,
      @NonNull CustomWeaponForm form, @NonNull TextField quantity, @NonNull TextField rounds,
      @NonNull ComboBox<AmmunitionData> ammunition, boolean counted) {
    CarriedWeapon weapon = weapon(catalog.isSelected(), page.selected(), form.weapon());
    Integer count = counted ? WholeNumber.between(quantity.getText(), 1, MAX_TYPED) : Integer.valueOf(1);
    AmmunitionData type = ammunition.getValue();
    Integer shots = type == null ? Integer.valueOf(0) : WholeNumber.between(rounds.getText(), 0, MAX_TYPED);
    if (weapon == null || count == null || shots == null) {
      return null;
    }
    String ammunitionId = type == null || shots == 0 ? null : type.id();
    return new WeaponPick(weapon.withQuantity(count), ammunitionId, shots);
  }

  private static @Nullable CarriedWeapon weapon(boolean fromCatalog, @Nullable WeaponData selected,
      @Nullable CustomWeapon described) {
    if (fromCatalog) {
      return selected == null ? null : CarriedWeapon.of(CatalogRef.of(selected.id()));
    }
    return described == null ? null : CarriedWeapon.of(described);
  }

  private static @NonNull Optional<Skill> skill(@NonNull Tab catalog, @NonNull CatalogPage<WeaponData> page,
      @NonNull CustomWeaponForm form) {
    if (catalog.isSelected()) {
      return Optional.ofNullable(page.selected()).map(WeaponData::skill);
    }
    return Optional.ofNullable(form.skill());
  }

  /** The ammunition types the chosen weapon uses: the catalog row's, or the one of the custom form. */
  private static @NonNull List<AmmunitionData> ammunitionOf(boolean fromCatalog, @Nullable WeaponData selected,
      @Nullable AmmunitionData custom) {
    if (!fromCatalog) {
      return custom == null ? List.of() : List.of(custom);
    }
    if (selected == null) {
      return List.of();
    }
    return selected.ammunition().stream().map(WeaponCatalog::ammunitionById).flatMap(Optional::stream).toList();
  }

  /** Status line, e.g. "2 × Frag Grenade" or "10mm Pistol, 20 rounds of 10mm". */
  private static @NonNull String describe(@NonNull WeaponPick pick) {
    int count = pick.weapon().quantity();
    String text = (count > 1 ? count + " × " : "") + pick.weapon().displayName();
    String ammunitionId = pick.ammunitionId();
    if (ammunitionId == null) {
      return text;
    }
    String name = WeaponCatalog.ammunitionById(ammunitionId).map(AmmunitionData::name).orElse(ammunitionId);
    return text + ", " + pick.rounds() + " rounds of " + name;
  }

  /** The effective range like the Weapons tree: the table range, else the Thrown range, e.g. "Medium (thrown)". */
  private static @NonNull String rangeText(@NonNull WeaponData weapon) {
    WeaponRange range = WeaponAttack.effectiveRange(weapon);
    if (range == null) {
      return "–";
    }
    return range.displayName() + (weapon.range() == null ? " (thrown)" : "");
  }

  /** The row tooltip: the values the table has no column for. */
  private static @NonNull String details(@NonNull WeaponData weapon) {
    List<String> lines = new ArrayList<>();
    if (!weapon.damageEffects().isEmpty()) {
      lines.add("Effects: " + String.join(", ", weapon.damageEffects()));
    }
    lines.add("Damage type: " + weapon.damageTypes().stream().map(DamageType::displayName)
        .collect(Collectors.joining("/")));
    if (weapon.fireRate() != null) {
      lines.add("Fire rate: " + weapon.fireRate());
    }
    if (!weapon.qualities().isEmpty()) {
      lines.add("Qualities: " + String.join(", ", weapon.qualities()));
    }
    return String.join("\n", lines);
  }
}
