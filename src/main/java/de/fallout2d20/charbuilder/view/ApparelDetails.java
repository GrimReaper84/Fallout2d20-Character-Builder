package de.fallout2d20.charbuilder.view;

import static de.fallout2d20.charbuilder.view.OverviewParts.detailRow;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.Coverage;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.apparel.ApparelValues;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.apparel.ModdedApparel;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The details of the piece selected in the Armor tab: source, whether it is worn (C5: only worn pieces
 * protect, carried ones only count toward the load), kind, the covered locations – a choice of the limb for a
 * single arm or leg piece (CRB p. 130; robot armor one arm, CRB p. 146) –, its resistances, weight and cost with
 * the mods (changed values bold with the value without mods as tooltip), rarity, the perk it needs, the note and
 * the mods ({@link ApparelModSection}). Typed notes go to {@code onEdited}; everything else to the model and then
 * to {@code onChanged}.
 */
final class ApparelDetails {

  private ApparelDetails() {}

  /** The details of {@code entry}, or a prompt to select a piece if it is {@code null}. */
  static @NonNull Node build(@NonNull PlayerCharacter character, @Nullable ApparelEntry entry,
      @NonNull ViewCallbacks callbacks) {
    MigPane pane = Layouts.column();
    if (entry == null) {
      OverviewParts.sectionHeading(pane, "Apparel");
      pane.add(new Label("Select a piece on the left."));
      return pane;
    }
    int index = entry.index();
    // From the model, not the entry: typed notes change the piece without a rebuild of the tree's entries.
    WornApparel item = character.gear().getApparel().get(index);
    Optional<ApparelData> data = item.data();
    ModdedApparel modded = ModdedApparel.of(item).orElse(null);
    ModdedApparel base = ModdedApparel.unmodded(item).orElse(null);
    Optional<ApparelValues> values = Optional.ofNullable(modded).map(ModdedApparel::values);
    OverviewParts.sectionHeading(pane, item.displayName());

    MigPane rows = Layouts.details(0);
    detailRow(rows, "Source", new Label(data.map(catalog -> catalog.sources().getFirst().toString())
        .orElse("Custom item")));
    CheckBox worn = new CheckBox();
    worn.setSelected(!item.carried());
    worn.setTooltip(new Tooltip("Worn pieces protect; carried pieces only count toward the load."));
    worn.setOnAction(_ -> {
      character.gear().setApparelCarried(index, !worn.isSelected());
      callbacks.onChanged().run();
    });
    detailRow(rows, "Worn", worn);
    detailRow(rows, "Kind", new Label(values.map(value -> value.kind().displayName()).orElse("–")));
    detailRow(rows, "Covers", location(character, item, data, index, callbacks.onChanged()));
    detailRow(rows, "Physical", withBase(base, modded, value -> Integer.toString(value.values().physical())));
    detailRow(rows, "Energy", withBase(base, modded, value -> Integer.toString(value.values().energy())));
    detailRow(rows, "Radiation", withBase(base, modded, value -> radiation(value.values())));
    values.filter(value -> value.carryWeight() != 0).ifPresent(value ->
        detailRow(rows, "Carry weight",
            new Label((value.carryWeight() > 0 ? "+" : "") + value.carryWeight() + " lbs.")));
    if (data.isPresent() || !item.mods().isEmpty()) {
      detailRow(rows, "Weight",
          withBase(base, modded, value -> value.weight() == null ? "–" : value.weight() + " lbs."));
      detailRow(rows, "Cost", withBase(base, modded, value -> OverviewParts.orDash(value.cost())));
    }
    data.ifPresent(catalog -> {
      detailRow(rows, "Rarity", new Label(OverviewParts.orDash(catalog.rarity())));
      if (catalog.perk() != null) {
        detailRow(rows, "Needs perk", new Label(catalog.perk()));
      }
      String special = catalog.special();
      if (special != null) {
        detailRow(rows, "Special", OverviewParts.wrappingInCell(new Label(special)));
      }
    });
    OverviewParts.notesRow(rows, item.notes(), text -> {
      character.gear().setApparelNotes(index, text);
      callbacks.onEdited().run();
    });
    pane.add(rows);
    ApparelModSection.addTo(pane, character, index, item, modded, callbacks.onChanged());
    return pane;
  }

  /**
   * The modded value as label; bold, with the value without mods as tooltip, where the mods changed it. A dash for
   * an id outside the catalog.
   */
  private static @NonNull Label withBase(@Nullable ModdedApparel base, @Nullable ModdedApparel modded,
      @NonNull Function<ModdedApparel, String> text) {
    if (modded == null) {
      return new Label("–");
    }
    Label label = new Label(text.apply(modded));
    if (base != null && !text.apply(base).equals(text.apply(modded))) {
      label.setStyle("-fx-font-weight: bold;");
      label.setTooltip(new Tooltip("Without mods: " + text.apply(base)));
    }
    return label;
  }

  /** Radiation as in the tables: "Immune", the value, or "–" for robot items, whose table has no radiation. */
  private static @NonNull String radiation(@NonNull ApparelValues values) {
    if (values.radiationImmune()) {
      return "Immune";
    }
    return values.kind().isRobot() && values.radiation() == 0 ? "–" : Integer.toString(values.radiation());
  }

  /** A choice of the limb for a single-limb catalog piece, else the covered locations as text. */
  private static @NonNull Node location(@NonNull PlayerCharacter character, @NonNull WornApparel item,
      @NonNull Optional<ApparelData> data, int index, @NonNull Runnable onChanged) {
    Optional<Coverage> chosen = data.flatMap(entry -> entry.coverage().stream().filter(Coverage::isChosen).findFirst());
    if (chosen.isEmpty()) {
      return OverviewParts.wrappingInCell(new Label(OverviewParts.orDash(item.locations(character.hitLocations())
          .stream().map(HitLocation::displayName).collect(Collectors.joining(", ")))));
    }
    ComboBox<HitLocation> box = new ComboBox<>();
    box.getItems().setAll(chosen.get().locationsOn(character.hitLocations()));
    box.setConverter(new DisplayConverter<>(HitLocation::displayName));
    box.setPromptText("Choose");
    box.setValue(item.location());
    box.setMaxWidth(Region.USE_PREF_SIZE);
    box.setOnAction(_ -> {
      if (box.getValue() != item.location()) {
        character.gear().setApparelLocation(index, box.getValue());
        onChanged.run();
      }
    });
    return box;
  }
}
