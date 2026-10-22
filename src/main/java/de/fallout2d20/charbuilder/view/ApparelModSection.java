package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModSlot;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModTable;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.apparel.ApparelMods;
import de.fallout2d20.charbuilder.model.character.apparel.CustomApparel;
import de.fallout2d20.charbuilder.model.character.apparel.ModdedApparel;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.view.dialog.ApparelModDialog;
import de.fallout2d20.charbuilder.view.dialog.ConfirmDialog;
import de.fallout2d20.charbuilder.view.dialog.ModTableChoice;
import de.fallout2d20.charbuilder.view.dialog.ModTexts;
import java.util.List;
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
 * The Mods section of the apparel details (D4 armor mods): for a custom piece the choice of its mod table (user
 * decision 2026-10-22), then one row per slot on offer ({@link ApparelMods#slots}) with the installed mod as label
 * (details as tooltip) and an edit button that opens {@link ApparelModDialog}; below, the mods' rules that change
 * no value. Nothing for a catalog piece that takes no mods. Every change goes to the model and then to
 * {@code onChanged}.
 */
final class ApparelModSection {

  private ApparelModSection() {}

  static void addTo(@NonNull MigPane pane, @NonNull PlayerCharacter character, int index, @NonNull WornApparel item,
      @Nullable ModdedApparel values, @NonNull Runnable onChanged) {
    CustomApparel custom = item.custom();
    List<ApparelModSlot> slots = ApparelMods.slots(item);
    if (slots.isEmpty() && custom == null) {
      return;
    }
    OverviewParts.sectionHeading(pane, "Mods");
    MigPane rows = Layouts.details(1);
    if (custom != null) {
      rows.add(new Label("Mod table"));
      rows.add(tableChoice(character, index, item, custom, onChanged), Layouts.span(2));
    }
    for (ApparelModSlot slot : slots) {
      Label name = new Label(slot.displayName());
      name.setMinWidth(Label.USE_PREF_SIZE);
      rows.add(name);
      rows.add(installed(item, slot, item.mods().get(slot)));
      rows.add(editButton(character, index, item, slot, onChanged));
    }
    pane.add(rows);
    if (values != null && !values.specials().isEmpty()) {
      MigPane specials = Layouts.column();
      for (String special : values.specials()) {
        specials.add(OverviewParts.wrappingInCell(new Label(special)), Layouts.cell().growX());
      }
      OverviewParts.sectionHeading(pane, "Mod effects");
      pane.add(specials);
    }
  }

  /**
   * The installed mod's name with its details, a dash for none; an unknown id or a mod that does not fit (hand-edited
   * file) is shown with a note that it has no effect.
   */
  private static @NonNull Label installed(@NonNull WornApparel item, @NonNull ApparelModSlot slot,
      @Nullable CatalogRef ref) {
    if (ref == null) {
      return new Label("–");
    }
    ApparelModData mod = ApparelCatalog.modById(ref.id()).orElse(null);
    if (mod == null || !ApparelMods.fits(item, slot, mod)) {
      Label unusable = new Label(mod == null ? ref.displayName() : mod.name());
      unusable.setTooltip(new Tooltip(mod == null ? "Not in the mod catalog; it has no effect."
          : "Does not fit this " + slot.displayName() + " slot; it has no effect here."));
      return unusable;
    }
    Label label = new Label(mod.name());
    label.setTooltip(new Tooltip(ModTexts.details(item, mod)));
    return label;
  }

  private static @NonNull Node editButton(@NonNull PlayerCharacter character, int index, @NonNull WornApparel item,
      @NonNull ApparelModSlot slot, @NonNull Runnable onChanged) {
    Button edit = new Button();
    edit.setGraphic(new FontIcon("fas-wrench"));
    edit.setTooltip(new Tooltip("Change the " + slot.displayName() + " mod"));
    edit.setOnAction(_ -> {
      CatalogRef ref = item.mods().get(slot);
      ApparelModData current = ref == null ? null : ApparelCatalog.modById(ref.id()).orElse(null);
      ApparelModDialog.show(window(edit), character, item, slot, current).ifPresent(choice -> {
        character.gear().setApparelMod(index, slot, choice.mod());
        onChanged.run();
      });
    });
    return edit;
  }

  /**
   * The custom piece's mod table; asks before a change removes installed mods and goes back to the old table if
   * the player declines (nothing changed, so no {@code onChanged}).
   */
  private static @NonNull Node tableChoice(@NonNull PlayerCharacter character, int index, @NonNull WornApparel item,
      @NonNull CustomApparel custom, @NonNull Runnable onChanged) {
    ModTableChoice choice = new ModTableChoice(custom.kind(), custom.modTable());
    choice.node().setOnAction(_ -> {
      ApparelModTable chosen = choice.value();
      if (chosen == custom.modTable()) {
        return;
      }
      if (confirmRemoval(choice.node(), item, chosen)) {
        character.gear().setCustomApparelModTable(index, chosen);
        onChanged.run();
      } else {
        choice.select(custom.modTable());
      }
    });
    return choice.node();
  }

  /** Asks before a table change that removes installed mods; true if nothing is lost or the player agrees. */
  private static boolean confirmRemoval(@NonNull Node owner, @NonNull WornApparel item,
      @Nullable ApparelModTable chosen) {
    boolean losesMods = item.withCustomModTable(chosen).mods().size() < item.mods().size();
    return !losesMods || ConfirmDialog.confirm(window(owner), "Remove mods", "Change the mod table?",
        "Installed mods that do not fit the new table are removed.", "Change");
  }

  private static @Nullable Window window(@NonNull Node node) {
    return node.getScene() == null ? null : node.getScene().getWindow();
  }
}
