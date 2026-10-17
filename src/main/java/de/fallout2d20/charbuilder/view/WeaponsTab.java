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
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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
 * rebuild after a change ({@link WeaponSelection}).
 */
final class WeaponsTab {

  static final @NonNull String UNARMED_STRIKE_ID = "unarmed-strike";
  private static final double TREE_COLUMN_WIDTH = 300;
  private static final double PADDING = 16;
  private static final double COLUMN_GAP = 32;
  /** The tree never shrinks below this when the ammunition list grows; the left side scrolls instead. */
  private static final double TREE_MIN_HEIGHT = 200;
  /** Space between a side's content and its scroll bar. */
  private static final double SCROLL_BAR_GAP = 12;

  private WeaponsTab() {}

  static @NonNull Node build(@NonNull Character character, @NonNull WeaponSelection selection,
      @NonNull ViewCallbacks callbacks) {
    List<WeaponEntry> entries = entries(character);
    Integer selected = selection.current(character.getWeapons().size());
    WeaponEntry current = selected == null ? null
        : entries.stream().filter(entry -> entry.index() == selected).findFirst().orElse(null);

    ScrollPane details = scrollPane(WeaponDetails.build(character, current, callbacks), selection::detailsScroll,
        selection::setDetailsScroll);
    Button remove = new Button("Remove");
    remove.setGraphic(new FontIcon("fas-trash"));
    // On a wrapper, so the reason also shows while the button is disabled.
    Tooltip removeTooltip = new Tooltip();
    HBox removeWrapper = new HBox(remove);
    Tooltip.install(removeWrapper, removeTooltip);
    updateRemove(remove, removeTooltip, current);
    TreeView<WeaponTreeNode> tree = WeaponTree.build(entries, current, entry -> {
      if (entry == null) {
        selection.clear();
      } else {
        selection.select(entry.index());
      }
      updateRemove(remove, removeTooltip, entry);
      details.setContent(padded(WeaponDetails.build(character, entry, callbacks)));
      details.setVvalue(0);
    });
    remove.setOnAction(event -> {
      Integer index = selection.current(character.getWeapons().size());
      if (index == null || index == WeaponSelection.UNARMED_STRIKE_INDEX) {
        return;
      }
      CarriedWeapon weapon = character.getWeapons().get(index);
      if (ConfirmDialog.confirm(window(remove), "Remove weapon", "Remove " + weapon.displayName() + "?",
          "The weapon and its note are removed from the weapons.", "Remove")) {
        character.removeWeapon(index);
        selection.clear();
        callbacks.onChanged().run();
      }
    });

    MigPane left = new MigPane("insets 0, gap 0 4, wrap 1", "[grow, fill]");
    OverviewParts.sectionHeading(left, "Weapons");
    MigPane buttons = new MigPane("insets 0, gap 8 0");
    buttons.add(addButton(character, selection, callbacks.onChanged()));
    buttons.add(removeWrapper);
    left.add(buttons);
    tree.setMinHeight(TREE_MIN_HEIGHT);
    left.add(tree, "grow, push");
    OverviewParts.sectionHeading(left, "Ammunition");
    left.add(AmmunitionSection.build(character, callbacks.onChanged()));

    ScrollPane leftScroll = scrollPane(left, selection::listScroll, selection::setListScroll);
    leftScroll.setFitToHeight(true);
    // A border pane gives the details all the width the fixed tree column leaves.
    leftScroll.setMinWidth(TREE_COLUMN_WIDTH);
    leftScroll.setPrefWidth(TREE_COLUMN_WIDTH);
    leftScroll.setMaxWidth(TREE_COLUMN_WIDTH);
    BorderPane pane = new BorderPane(details);
    pane.setLeft(leftScroll);
    pane.setPadding(new Insets(PADDING));
    BorderPane.setMargin(details, new Insets(0, 0, 0, COLUMN_GAP));
    return pane;
  }

  /**
   * A scroll pane filling its width that keeps its position across rebuilds: restored after layout, recorded
   * only while it is shown (a pane leaving the scene must not overwrite the position).
   */
  private static @NonNull ScrollPane scrollPane(@NonNull Node content, @NonNull DoubleSupplier saved,
      @NonNull DoubleConsumer save) {
    ScrollPane scroll = new ScrollPane(padded(content));
    scroll.setFitToWidth(true);
    scroll.setStyle("-fx-background-color: transparent;");
    scroll.vvalueProperty().addListener((obs, old, value) -> {
      if (scroll.getScene() != null) {
        save.accept(value.doubleValue());
      }
    });
    double position = saved.getAsDouble();
    Platform.runLater(() -> scroll.setVvalue(position));
    return scroll;
  }

  /** The content with some space to the scroll bar on its right, so it does not stick to it. */
  private static @NonNull Node padded(@NonNull Node content) {
    BorderPane pane = new BorderPane(content);
    pane.setPadding(new Insets(0, SCROLL_BAR_GAP, 0, 0));
    return pane;
  }

  /** The Unarmed Strike and every carried weapon with its list index. */
  private static @NonNull List<WeaponEntry> entries(@NonNull Character character) {
    List<WeaponEntry> entries = new ArrayList<>();
    WeaponCatalog.byId(UNARMED_STRIKE_ID).ifPresent(strike -> entries.add(
        new WeaponEntry(WeaponSelection.UNARMED_STRIKE_INDEX, WeaponAttack.of(character, strike), null)));
    List<CarriedWeapon> weapons = character.getWeapons();
    for (int i = 0; i < weapons.size(); i++) {
      entries.add(new WeaponEntry(i, WeaponAttack.of(character, weapons.get(i)), weapons.get(i)));
    }
    return entries;
  }

  /** Opens the catalog of the weapons offered to the character's origin and books; the new one is selected. */
  private static @NonNull Button addButton(@NonNull Character character, @NonNull WeaponSelection selection,
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
  private static void updateRemove(@NonNull Button remove, @NonNull Tooltip tooltip, @Nullable WeaponEntry entry) {
    remove.setDisable(entry == null || entry.weapon() == null);
    tooltip.setText(entry == null ? "Select a carried weapon to remove it."
        : entry.weapon() == null ? "Every character has the Unarmed Strike; it cannot be removed."
        : "Remove " + entry.attack().name());
  }

  private static @Nullable Window window(@NonNull Node node) {
    return node.getScene() == null ? null : node.getScene().getWindow();
  }
}
