package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorLocation;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryCatalog;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponType;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.legendary.LegendaryRoll;
import de.fallout2d20.charbuilder.model.character.legendary.LegendaryRollResult;
import de.fallout2d20.charbuilder.model.character.legendary.LegendaryRules;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import org.controlsfx.control.Notifications;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The Legendary section of the weapon and armor details (F6d, WG pp. 96–108): a choice of the legendary property
 * among those {@link LegendaryRules} offers ("None" makes the item ordinary), a Roll button following the book
 * tables while there are options (a property kept from GM mode or a disabled book can only be removed outside it) ({@link LegendaryRoll}; for a piece covering several table columns with a choice of the column), and the
 * chosen property's rule. Shown only when the item can be legendary or already is. Every change goes to the model
 * and then to {@code onChanged}; the dice rolled are shown as a notification.
 */
final class LegendarySection {

  /** The combo box value for "not legendary". */
  private static final @NonNull String NONE = "";
  private static final @NonNull LegendaryRoll ROLL = new LegendaryRoll(RandomGenerator.getDefault());

  private LegendarySection() {}

  /** The section for the carried weapon at the index. */
  static void addTo(@NonNull MigPane pane, @NonNull PlayerCharacter character, int index,
      @NonNull CarriedWeapon weapon, @NonNull Runnable onChanged) {
    List<LegendaryWeaponData> options = LegendaryRules.options(character, weapon);
    if (options.isEmpty() && weapon.legendary() == null) {
      return;
    }
    Consumer<@Nullable String> choose = id -> character.gear().setWeaponLegendary(index,
        id == null ? null : LegendaryCatalog.weaponPropertyById(id).orElse(null));
    List<Node> rollControls = new ArrayList<>();
    Optional<LegendaryWeaponType> type = weapon.legendaryType();
    if (type.isPresent() && !options.isEmpty()) {
      Button roll = rollButton("Roll on the Legendary Weapons tables (" + type.get().displayName() + ")");
      roll.setOnAction(_ -> {
        LegendaryRollResult<LegendaryWeaponData> result = ROLL.weapon(type.get());
        character.gear().setWeaponLegendary(index, result.property());
        announce(roll, result.description(), result.property().name());
        onChanged.run();
      });
      rollControls.add(roll);
    }
    build(pane, options.stream().map(LegendaryWeaponData::id).toList(), weapon.legendary(),
        id -> LegendaryCatalog.weaponPropertyById(id).map(property -> new LegendaryOption(property.name(),
            property.rule(), property.sources().getFirst())), choose, rollControls, onChanged);
  }

  /** The section for the apparel piece at the index. */
  static void addTo(@NonNull MigPane pane, @NonNull PlayerCharacter character, int index, @NonNull WornApparel item,
      @NonNull Runnable onChanged) {
    List<LegendaryArmorData> options = LegendaryRules.options(character, item);
    if (options.isEmpty() && item.legendary() == null) {
      return;
    }
    Consumer<@Nullable String> choose = id -> character.gear().setApparelLegendary(index,
        id == null ? null : LegendaryCatalog.armorPropertyById(id).orElse(null));
    List<Node> rollControls = new ArrayList<>();
    Set<LegendaryArmorLocation> columns = item.legendaryLocations();
    if (!columns.isEmpty() && !options.isEmpty()) {
      ComboBox<LegendaryArmorLocation> column = new ComboBox<>();
      column.getItems().setAll(columns);
      column.setConverter(new DisplayConverter<>(LegendaryArmorLocation::displayName));
      column.setValue(columns.contains(LegendaryArmorLocation.TORSO) ? LegendaryArmorLocation.TORSO
          : column.getItems().getFirst());
      column.setTooltip(new Tooltip("The covered location whose table column is rolled on (WG p. 104)"));
      column.setMaxWidth(Region.USE_PREF_SIZE);
      Button roll = rollButton("Roll on the Legendary Armor table");
      roll.setOnAction(_ -> {
        LegendaryRollResult<LegendaryArmorData> result = ROLL.armor(column.getValue());
        character.gear().setApparelLegendary(index, result.property());
        announce(roll, result.description(), result.property().name());
        onChanged.run();
      });
      if (columns.size() > 1) {
        rollControls.add(column);
      }
      rollControls.add(roll);
    }
    build(pane, options.stream().map(LegendaryArmorData::id).toList(), item.legendary(),
        id -> LegendaryCatalog.armorPropertyById(id).map(property -> new LegendaryOption(property.name(),
            property.rule() + " Every legendary piece also has +1 Physical and Energy DR.",
            property.sources().getFirst())), choose, rollControls, onChanged);
  }

  /**
   * The heading, a row with the property choice and the roll controls, and the chosen property's rule. A stored
   * property outside the options (GM mode left, book disabled, unknown id) stays selectable as it is.
   */
  private static void build(@NonNull MigPane pane, @NonNull List<String> options, @Nullable CatalogRef stored,
      @NonNull Function<String, Optional<LegendaryOption>> shown, @NonNull Consumer<@Nullable String> choose,
      @NonNull List<Node> rollControls, @NonNull Runnable onChanged) {
    OverviewParts.sectionHeading(pane, "Legendary");
    ComboBox<String> choice = new ComboBox<>();
    choice.getItems().add(NONE);
    choice.getItems().addAll(options);
    if (stored != null && !options.contains(stored.id())) {
      choice.getItems().add(stored.id());
    }
    Function<String, String> name = id -> id.equals(NONE) ? "None"
        : shown.apply(id).map(LegendaryOption::name).orElseGet(() -> stored == null ? id : stored.displayName());
    choice.setConverter(new DisplayConverter<>(name));
    choice.setCellFactory(_ -> new ListCell<>() {
      @Override
      protected void updateItem(@Nullable String id, boolean empty) {
        super.updateItem(id, empty);
        setText(empty || id == null ? null : name.apply(id));
        setTooltip(empty || id == null ? null
            : shown.apply(id).map(property -> new Tooltip(property.rule())).orElse(null));
      }
    });
    choice.setValue(stored == null ? NONE : stored.id());
    choice.setOnAction(_ -> {
      String chosen = choice.getValue();
      String current = stored == null ? NONE : stored.id();
      if (chosen != null && !chosen.equals(current)) {
        choose.accept(chosen.equals(NONE) ? null : chosen);
        onChanged.run();
      }
    });

    MigPane rows = Layouts.details(0);
    MigPane row = Layouts.row();
    row.add(choice);
    rollControls.forEach(row::add);
    OverviewParts.detailRow(rows, "Property", row);
    if (stored != null) {
      Label rule = shown.apply(stored.id())
          .map(property -> new Label(property.rule() + " (" + property.source() + ")"))
          .orElseGet(() -> new Label("Not in the legendary catalog; it has no effect."));
      OverviewParts.detailRow(rows, "Rule", OverviewParts.wrappingInCell(rule));
    }
    pane.add(rows);
  }

  private static @NonNull Button rollButton(@NonNull String tooltip) {
    Button roll = new Button("Roll");
    roll.setGraphic(new FontIcon("fas-dice-d20"));
    roll.setTooltip(new Tooltip(tooltip));
    return roll;
  }

  private static void announce(@NonNull Node owner, @NonNull String dice, @NonNull String property) {
    Notifications.create().owner(owner).title("Legendary property").text(dice + " → " + property)
        .showInformation();
  }
}
