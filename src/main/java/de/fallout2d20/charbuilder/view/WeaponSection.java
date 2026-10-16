package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.weapon.AmmunitionData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.CustomWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponAttack;
import de.fallout2d20.charbuilder.view.dialog.AddWeaponDialog;
import de.fallout2d20.charbuilder.view.dialog.ConfirmDialog;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Weapons and ammunition of a character, in every mode (weapons are bought and found at any time): one
 * row per weapon with what to roll ({@link WeaponAttack}: target number, critical range, damage with the
 * melee bonus), its effects, type, fire rate, range, qualities and ammunition left, a quantity for
 * explosives and thrown weapons, a note (e.g. mods) and a remove button; the Unarmed Strike every
 * character has comes first (CRB p. 111). Below, the shots per ammunition type (CRB p. 91). Typed notes
 * go to {@code onEdited}; everything else to the model and then to {@code onChanged}.
 */
final class WeaponSection {

  private static final @NonNull String UNARMED_STRIKE = "unarmed-strike";
  private static final @NonNull List<String> HEADERS = List.of("Weapon", "Attack", "Damage", "Effects", "Type",
      "Rate", "Range", "Qualities", "Ammo", "Qty", "Notes", "");
  private static final double NUMBER_WIDTH = 60;
  private static final double NOTES_WIDTH = 160;

  private WeaponSection() {}

  /** Adds the weapon table, the add button and the ammunition rows to a four-column overview grid. */
  static void addTo(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onEdited,
      @NonNull Runnable onChanged) {
    MigPane table = new MigPane("insets 0, gap 10 4, wrap " + HEADERS.size());
    for (String header : HEADERS) {
      Label label = new Label(header);
      label.setStyle("-fx-font-weight: bold;");
      table.add(label);
    }
    WeaponCatalog.byId(UNARMED_STRIKE).ifPresent(strike -> {
      addAttack(table, character, WeaponAttack.of(character, strike), strike.sources().get(0).toString(), null);
      table.add(new Label(""), "span 3");
    });
    List<CarriedWeapon> weapons = character.getWeapons();
    for (int i = 0; i < weapons.size(); i++) {
      addWeapon(table, character, weapons.get(i), i, onEdited, onChanged);
    }
    pane.add(table, "span 4, growx, wmin 0");

    Button add = new Button("Add weapon");
    add.setGraphic(new FontIcon("fas-plus"));
    List<WeaponData> offered = WeaponCatalog.available(character.getEnabledBooks()).stream()
        .filter(data -> !data.id().equals(UNARMED_STRIKE)).toList();
    add.setOnAction(event -> AddWeaponDialog.show(window(add), offered).ifPresent(weapon -> {
          character.addWeapon(weapon);
          onChanged.run();
        }));
    pane.add(add, "span 4, left");

    addAmmunition(pane, character, onChanged);
  }

  private static void addWeapon(@NonNull MigPane table, @NonNull Character character, @NonNull CarriedWeapon weapon,
      int index, @NonNull Runnable onEdited, @NonNull Runnable onChanged) {
    String source = weapon.data().map(data -> data.sources().get(0).toString()).orElse("Custom weapon");
    CustomWeapon custom = weapon.custom();
    Node skillChoice = custom != null && custom.skill() == null ? skillChoice(character, index, onChanged) : null;
    addAttack(table, character, WeaponAttack.of(character, weapon), source, skillChoice);
    table.add(weapon.isCounted() ? numberField(weapon.quantity(), 1, quantity -> {
      character.setWeaponQuantity(index, quantity);
      onChanged.run();
    }) : new Label(""));
    TextField notes = new TextField(weapon.notes());
    notes.setPromptText("Notes, e.g. mods");
    notes.setPrefWidth(NOTES_WIDTH);
    notes.textProperty().addListener((obs, old, text) -> {
      character.setWeaponNotes(index, text);
      onEdited.run();
    });
    table.add(notes);
    Button remove = new Button();
    remove.setGraphic(new FontIcon("fas-trash"));
    remove.setTooltip(new Tooltip("Remove " + weapon.displayName()));
    remove.setOnAction(event -> {
      if (ConfirmDialog.confirm(window(remove), "Remove weapon", "Remove " + weapon.displayName() + "?",
          "The weapon and its note are removed from the weapons.", "Remove")) {
        character.removeWeapon(index);
        onChanged.run();
      }
    });
    table.add(remove);
  }

  /**
   * The nine columns from name to ammunition; {@code skillChoice} replaces the attack column of a custom
   * weapon without skill.
   */
  private static void addAttack(@NonNull MigPane table, @NonNull Character character, @NonNull WeaponAttack attack,
      @NonNull String source, @Nullable Node skillChoice) {
    Label name = new Label(attack.name());
    name.setTooltip(new Tooltip(source));
    table.add(name);
    Skill skill = attack.skill();
    if (skillChoice != null) {
      table.add(skillChoice);
    } else {
      Label roll = new Label(attack.targetNumber() == null ? "–"
          : "TN " + attack.targetNumber() + ", crit ≤ " + attack.criticalRange());
      if (skill != null) {
        roll.setTooltip(new Tooltip(skill.governingAttribute().abbreviation() + " + " + skill.displayName()
            + (character.isTagged(skill) ? " (Tag skill)" : "")));
      }
      table.add(roll);
    }
    table.add(new Label(attack.damage() + " CD" + (attack.meleeBonus() > 0 ? " + " + attack.meleeBonus() + " CD" : "")));
    table.add(new Label(orDash(attack.effects())));
    table.add(new Label(orDash(attack.damageType())));
    table.add(new Label(attack.fireRate() == null ? "–" : attack.fireRate().toString()));
    table.add(new Label(attack.range() == null ? "–" : attack.range().code()));
    table.add(new Label(orDash(attack.qualities())));
    table.add(new Label(attack.ammunition().isEmpty() ? "–" : attack.ammunition().stream()
        .map(id -> ammunitionName(id) + ": " + character.ammunition(id)).collect(Collectors.joining(", "))));
  }

  /**
   * One row per ammunition type a carried weapon uses or that has shots, with an editable number of shots;
   * a combo box adds another type.
   */
  private static void addAmmunition(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onChanged) {
    Set<String> shown = new LinkedHashSet<>();
    for (CarriedWeapon weapon : character.getWeapons()) {
      shown.addAll(WeaponAttack.of(character, weapon).ammunition());
    }
    shown.addAll(character.getAmmunition().keySet());
    pane.add(new Label("Ammunition"), "top");
    MigPane rows = new MigPane("insets 0, gap 10 4, wrap 2");
    for (String id : shown) {
      Label name = new Label(ammunitionName(id));
      WeaponCatalog.ammunitionById(id).ifPresent(data -> name.setTooltip(new Tooltip("Found: " + data.quantityFound()
          + ", rarity " + data.rarity() + " (" + data.sources().get(0) + ")")));
      rows.add(name);
      rows.add(numberField(character.ammunition(id), 0, shots -> {
        character.setAmmunition(id, shots);
        onChanged.run();
      }));
    }
    List<AmmunitionData> others = WeaponCatalog.ammunition().stream().filter(data -> !shown.contains(data.id())).toList();
    ComboBox<AmmunitionData> add = new ComboBox<>();
    add.getItems().setAll(others);
    add.setConverter(new DisplayConverter<>(AmmunitionData::name));
    add.setPromptText("Add ammunition type");
    add.setOnAction(event -> {
      AmmunitionData chosen = add.getValue();
      if (chosen != null) {
        character.setAmmunition(chosen.id(), 1);
        onChanged.run();
      }
    });
    rows.add(add, "span 2");
    pane.add(rows, "span 3");
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

  /**
   * A number field that takes its value on Enter or when it loses the focus, once: the commit rebuilds the
   * overview, and the old field must not commit again when it loses the focus on removal. Anything but a
   * whole number of at least {@code minimum} resets it.
   */
  private static @NonNull TextField numberField(int value, int minimum, @NonNull IntConsumer commit) {
    TextField field = new TextField(Integer.toString(value));
    field.setPrefWidth(NUMBER_WIDTH);
    Runnable take = () -> {
      if (field.getUserData() != null) {
        return;
      }
      OptionalInt typed = WholeNumber.parse(field.getText());
      if (typed.isPresent() && typed.getAsInt() >= minimum) {
        if (typed.getAsInt() != value) {
          field.setUserData(Boolean.TRUE);
          commit.accept(typed.getAsInt());
        }
      } else {
        field.setText(Integer.toString(value));
      }
    };
    field.setOnAction(event -> take.run());
    field.focusedProperty().addListener((obs, old, focused) -> {
      if (!focused) {
        take.run();
      }
    });
    return field;
  }

  private static @NonNull String ammunitionName(@NonNull String id) {
    return WeaponCatalog.ammunitionById(id).map(AmmunitionData::name).orElse(id);
  }

  private static @NonNull String orDash(@NonNull String text) {
    return text.isBlank() ? "–" : text;
  }

  private static @Nullable Window window(@NonNull Node node) {
    return node.getScene() == null ? null : node.getScene().getWindow();
  }
}
