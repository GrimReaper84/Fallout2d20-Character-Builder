package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.trait.TraitCatalog;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import de.fallout2d20.charbuilder.model.character.creation.TraitRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Trait choice of a character in creation, for origins with trait lists (Survivor CRB p. 56,
 * Tribal WG p. 15): one checkbox per trait, grouped by list, and the attribute choosers of Gifted.
 * Every change goes to the model and then to {@code onChanged}, which rebuilds the overview.
 */
final class TraitSection {

  private static final String FULL_TOOLTIP = "Two traits are already chosen; uncheck one first.";

  private TraitSection() {}

  /** Adds the trait rows to a four-column overview grid; nothing for origins without trait lists. */
  static void addTo(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onChanged) {
    Optional<OriginData> origin = character.getOriginData();
    if (origin.isEmpty() || origin.get().traitLists().isEmpty()) {
      return;
    }
    Set<String> chosen = character.getTraits().stream().map(trait -> trait.trait().id())
        .collect(Collectors.toSet());
    for (String list : origin.get().traitLists()) {
      pane.add(new Label(listTitle(list)), "top");
      List<TraitDefinition> traits = TraitCatalog.inList(list);
      for (int i = 0; i < traits.size(); i++) {
        if (i > 0) {
          pane.add(new Label());
        }
        pane.add(checkBox(character, traits.get(i), chosen, onChanged), "span 3");
      }
    }
    for (TraitSelection selection : character.getTraits()) {
      addAttributeChoosers(pane, character, selection, onChanged);
    }
    if (TraitRules.takesExtraPerkInsteadOfTrait(character)) {
      pane.add(new Label());
      pane.add(new Label("One trait chosen: you get an extra perk."), "span 3");
    }
  }

  /**
   * A checkbox per trait. Once two traits are chosen, the others stay visible but disabled; the
   * tooltip sits on a wrapper because disabled controls show no tooltip.
   */
  private static @NonNull HBox checkBox(@NonNull Character character, @NonNull TraitDefinition trait,
      @NonNull Set<String> chosen, @NonNull Runnable onChanged) {
    CheckBox box = new CheckBox(trait.name());
    boolean selected = chosen.contains(trait.id());
    box.setSelected(selected);
    boolean full = !selected && chosen.size() >= TraitRules.MAX_TRAITS;
    box.setDisable(full);
    box.selectedProperty().addListener((obs, old, value) -> {
      if (value) {
        character.chooseTrait(CatalogRef.of(trait.id()));
      } else {
        character.removeTrait(trait.id());
      }
      onChanged.run();
    });
    HBox wrapper = new HBox(box);
    Tooltip.install(wrapper, new Tooltip(full ? FULL_TOOLTIP + "\n" + describe(trait) : describe(trait)));
    return wrapper;
  }

  /**
   * Gifted (CRB p. 56): one chooser per attribute the trait raises. The choices are stored as a
   * compact list, so a chooser is only enabled once all choosers before it are set – otherwise a
   * value picked in the second chooser would show up in the first after the rebuild.
   */
  private static void addAttributeChoosers(@NonNull MigPane pane, @NonNull Character character,
      @NonNull TraitSelection selection, @NonNull Runnable onChanged) {
    Optional<TraitDefinition> trait = TraitCatalog.byId(selection.trait().id());
    if (trait.isEmpty() || !selection.choosesAttributes()) {
      return;
    }
    List<ComboBox<Attribute>> choosers = new ArrayList<>();
    for (int i = 0; i < trait.get().attributeChoices(); i++) {
      ComboBox<Attribute> chooser = new ComboBox<>();
      chooser.getItems().setAll(Attribute.values());
      chooser.setConverter(new DisplayConverter<>(Attribute::displayName));
      chooser.setPromptText("Choose an attribute");
      if (i < selection.attributeChoices().size()) {
        chooser.setValue(selection.attributeChoices().get(i));
      }
      chooser.setDisable(i > selection.attributeChoices().size());
      choosers.add(chooser);
      pane.add(new Label(trait.get().name() + " attribute " + (i + 1)));
      pane.add(chooser, "span 3");
    }
    for (ComboBox<Attribute> chooser : choosers) {
      chooser.valueProperty().addListener((obs, old, value) -> {
        List<Attribute> choices = choosers.stream().map(ComboBox::getValue)
            .filter(Objects::nonNull).toList();
        character.setTraitAttributeChoices(trait.get().id(), choices);
        onChanged.run();
      });
    }
  }

  /** Source, benefit and penalty of a trait for tooltips. */
  static @NonNull String describe(@NonNull TraitDefinition trait) {
    return trait.sources().stream().map(Source::toString).collect(Collectors.joining(", "))
        + "\nBenefit: " + trait.benefit() + "\nPenalty: " + trait.penalty();
  }

  private static @NonNull String listTitle(@NonNull String list) {
    return list.substring(0, 1).toUpperCase(Locale.ROOT) + list.substring(1) + " traits";
  }
}
