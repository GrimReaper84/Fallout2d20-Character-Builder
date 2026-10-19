package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.CustomWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.ModdedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponMods;
import de.fallout2d20.charbuilder.view.dialog.ConfirmDialog;
import de.fallout2d20.charbuilder.view.dialog.ModSlotsDialog;
import de.fallout2d20.charbuilder.view.dialog.ModTexts;
import de.fallout2d20.charbuilder.view.dialog.WeaponModDialog;
import java.util.List;
import java.util.Set;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The Mods section of the weapon panel (D4): one row per slot on offer with the installed mod as label
 * (details as tooltip) and an edit button that opens {@link WeaponModDialog}; for a custom weapon an "Edit
 * slots" button (user decision 2026-10-17); below, the mods' rules that change no value. Nothing for a
 * catalog weapon that takes no mods. Every change goes to the model and then to {@code onChanged}.
 */
final class WeaponModSection {

  private static final @NonNull String GRIP_OR_STOCK = "This weapon takes a Grip or a Stock mod, not both (CRB p. 98).";

  private WeaponModSection() {}

  static void addTo(@NonNull MigPane pane, @NonNull PlayerCharacter character, int index, @NonNull CarriedWeapon weapon,
      @NonNull ModdedWeapon values, @NonNull Runnable onChanged) {
    CustomWeapon custom = weapon.custom();
    List<ModSlot> slots = WeaponMods.slots(weapon);
    if (slots.isEmpty() && custom == null) {
      return;
    }
    OverviewParts.sectionHeading(pane, "Mods");
    MigPane rows = new MigPane("insets 0, gap 16 6, wrap 3", "[left][grow, fill][]");
    boolean gripOrStock = weapon.data().map(WeaponData::gripOrStock).orElse(false);
    for (ModSlot slot : slots) {
      Label name = new Label(slot.displayName());
      name.setMinWidth(Label.USE_PREF_SIZE);
      if (gripOrStock && (slot == ModSlot.GRIP || slot == ModSlot.STOCK)) {
        name.setTooltip(new Tooltip(GRIP_OR_STOCK));
      }
      rows.add(name);
      rows.add(installed(slot, weapon.mods().get(slot)));
      rows.add(editButton(character, index, weapon, slot, onChanged));
    }
    if (custom != null) {
      if (slots.isEmpty()) {
        rows.add(new Label("No mod slots chosen."), "span 2");
      }
      Button editSlots = new Button("Edit slots");
      editSlots.setGraphic(new FontIcon("fas-sliders-h"));
      editSlots.setOnAction(event -> ModSlotsDialog.show(window(editSlots), custom.name(), custom.modSlots())
          .ifPresent(chosen -> {
            if (confirmRemoval(editSlots, weapon, chosen)) {
              character.setCustomWeaponModSlots(index, chosen);
              onChanged.run();
            }
          }));
      rows.add(editSlots, slots.isEmpty() ? "" : "span 3");
    }
    pane.add(rows);
    if (!values.specials().isEmpty()) {
      MigPane specials = new MigPane("insets 0, gap 16 4, wrap 1", "[grow, fill]");
      for (String special : values.specials()) {
        specials.add(OverviewParts.wrappingInCell(new Label(special)), "growx");
      }
      OverviewParts.sectionHeading(pane, "Mod effects");
      pane.add(specials);
    }
  }

  /**
   * The installed mod's name with its details, a dash for none; an unknown id or a mod of another slot
   * (hand-edited file) is shown with a note that it has no effect.
   */
  private static @NonNull Label installed(@NonNull ModSlot slot, @Nullable CatalogRef ref) {
    if (ref == null) {
      return new Label("–");
    }
    WeaponModData mod = WeaponCatalog.modById(ref.id()).orElse(null);
    if (mod == null || mod.slot() != slot) {
      Label unusable = new Label(mod == null ? ref.displayName() : mod.name());
      unusable.setTooltip(new Tooltip(mod == null ? "Not in the mod catalog; it has no effect."
          : "Not a " + slot.displayName() + " mod; it has no effect here."));
      return unusable;
    }
    Label label = new Label(mod.name());
    label.setTooltip(new Tooltip(ModTexts.details(mod)));
    return label;
  }

  private static @NonNull Node editButton(@NonNull PlayerCharacter character, int index, @NonNull CarriedWeapon weapon,
      @NonNull ModSlot slot, @NonNull Runnable onChanged) {
    Button edit = new Button();
    edit.setGraphic(new FontIcon("fas-wrench"));
    edit.setTooltip(new Tooltip("Change the " + slot.displayName() + " mod"));
    edit.setOnAction(event -> {
      CatalogRef ref = weapon.mods().get(slot);
      WeaponModData current = ref == null ? null : WeaponCatalog.modById(ref.id()).orElse(null);
      WeaponModDialog.show(window(edit), character, weapon.displayName(), slot,
          WeaponMods.options(weapon, slot, character.getEnabledBooks()), current).ifPresent(choice -> {
            character.setWeaponMod(index, slot, choice.mod());
            onChanged.run();
          });
    });
    return edit;
  }

  /** Asks before unticking slots that hold a mod; true if nothing is lost or the player agrees. */
  private static boolean confirmRemoval(@NonNull Node owner, @NonNull CarriedWeapon weapon, @NonNull Set<ModSlot> chosen) {
    List<String> lost = weapon.mods().keySet().stream().filter(slot -> !chosen.contains(slot))
        .map(ModSlot::displayName).toList();
    return lost.isEmpty() || ConfirmDialog.confirm(window(owner), "Remove mods", "Remove the mods of "
        + String.join(", ", lost) + "?", "Unticked slots lose their installed mods.", "Remove");
  }

  private static @Nullable Window window(@NonNull Node node) {
    return node.getScene() == null ? null : node.getScene().getWindow();
  }
}
