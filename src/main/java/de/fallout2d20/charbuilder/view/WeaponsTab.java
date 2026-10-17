package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponAttack;
import de.fallout2d20.charbuilder.view.dialog.AddWeaponDialog;
import de.fallout2d20.charbuilder.view.dialog.ConfirmDialog;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeView;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Weapons tab, in every mode (weapons are bought and found at any time): on the left the add and remove
 * buttons, the weapon tree grouped by effective range ({@link WeaponTree}) with the Unarmed Strike every
 * character has (CRB p. 111) and the ammunition below it; on the right the values of the selected weapon
 * ({@link WeaponDetails}). Each side scrolls on its own, so editing mods never moves the weapon list.
 * Selecting a weapon only replaces the right side; the selection and both scroll positions survive the
 * rebuild after a change ({@link MasterDetailState}, layout {@link MasterDetail}).
 */
final class WeaponsTab {

  static final @NonNull String UNARMED_STRIKE_ID = "unarmed-strike";
  /** The list index of the Unarmed Strike, which is no carried weapon; selected when the tab is first shown. */
  static final int UNARMED_STRIKE_INDEX = -1;

  private WeaponsTab() {}

  static @NonNull Node build(@NonNull Character character, @NonNull MasterDetailState selection,
      @NonNull ViewCallbacks callbacks) {
    List<WeaponEntry> entries = entries(character);
    Integer selected = selection.current(character.getWeapons().size());
    WeaponEntry current = selected == null ? null
        : entries.stream().filter(entry -> entry.index() == selected).findFirst().orElse(null);

    ScrollPane details = MasterDetail.right(WeaponDetails.build(character, current, callbacks), selection);
    RemoveButton remove = new RemoveButton(() -> {
      Integer index = selection.current(character.getWeapons().size());
      if (index == null || index == UNARMED_STRIKE_INDEX) {
        return;
      }
      CarriedWeapon weapon = character.getWeapons().get(index);
      if (ConfirmDialog.confirm(window(details), "Remove weapon", "Remove " + weapon.displayName() + "?",
          "The weapon and its note are removed from the weapons.", "Remove")) {
        character.removeWeapon(index);
        selection.clear();
        callbacks.onChanged().run();
      }
    });
    updateRemove(remove, current);
    TreeView<WeaponTreeNode> tree = WeaponTree.build(entries, current, entry -> {
      if (entry == null) {
        selection.clear();
      } else {
        selection.select(entry.index());
      }
      updateRemove(remove, entry);
      MasterDetail.replace(details, WeaponDetails.build(character, entry, callbacks));
    });

    MigPane left = new MigPane("insets 0, gap 0 4, wrap 1", "[grow, fill]");
    OverviewParts.sectionHeading(left, "Weapons");
    MigPane buttons = new MigPane("insets 0, gap 8 0");
    buttons.add(addButton(character, selection, callbacks.onChanged()));
    buttons.add(remove.node());
    left.add(buttons);
    left.add(tree, "grow, push");
    OverviewParts.sectionHeading(left, "Ammunition");
    left.add(AmmunitionSection.build(character, callbacks.onChanged()));

    return MasterDetail.layout(MasterDetail.left(left, selection), details);
  }

  /** The Unarmed Strike and every carried weapon with its list index. */
  private static @NonNull List<WeaponEntry> entries(@NonNull Character character) {
    List<WeaponEntry> entries = new ArrayList<>();
    WeaponCatalog.byId(UNARMED_STRIKE_ID).ifPresent(strike -> entries.add(
        new WeaponEntry(UNARMED_STRIKE_INDEX, WeaponAttack.of(character, strike), null)));
    List<CarriedWeapon> weapons = character.getWeapons();
    for (int i = 0; i < weapons.size(); i++) {
      entries.add(new WeaponEntry(i, WeaponAttack.of(character, weapons.get(i)), weapons.get(i)));
    }
    return entries;
  }

  /** Opens the catalog of the weapons offered to the character's origin and books; the new one is selected. */
  private static @NonNull Button addButton(@NonNull Character character, @NonNull MasterDetailState selection,
      @NonNull Runnable onChanged) {
    Button add = new Button("Add weapon");
    add.setGraphic(new FontIcon("fas-plus"));
    CatalogRef origin = character.getOrigin();
    List<WeaponData> offered = WeaponCatalog.offered(origin == null ? null : origin.id(), character.getEnabledBooks())
        .stream().filter(data -> !data.id().equals(UNARMED_STRIKE_ID)).toList();
    add.setOnAction(event -> AddWeaponDialog.show(window(add), offered).ifPresent(weapon -> {
      character.addWeapon(weapon);
      selection.select(character.getWeapons().size() - 1);
      onChanged.run();
    }));
    return add;
  }

  /** Every carried weapon can be removed; the Unarmed Strike, a group or no selection cannot. */
  private static void updateRemove(@NonNull RemoveButton remove, @Nullable WeaponEntry entry) {
    remove.update(entry != null && entry.weapon() != null, entry == null ? "Select a carried weapon to remove it."
        : entry.weapon() == null ? "Every character has the Unarmed Strike; it cannot be removed."
        : "Remove " + entry.attack().name());
  }

  private static @Nullable Window window(@NonNull Node node) {
    return node.getScene() == null ? null : node.getScene().getWindow();
  }
}
