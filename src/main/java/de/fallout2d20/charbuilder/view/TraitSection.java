package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.trait.TraitCatalog;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
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
import net.miginfocom.layout.CC;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Traits of a character as a two-column grid: the origin trait with the origin's options (extra Tag
 * skill, purpose) below it, then the chosen traits. Each trait shows its name and below it a short
 * description with the source. During creation and in GM mode, origins with trait lists (Survivor
 * CRB p. 56, Tribal WG p. 15) offer one checkbox per trait, grouped by list, with benefit and penalty
 * below each, and the attribute choosers of Gifted below Gifted; the checkboxes show the choice, so the
 * chosen traits are not listed again. Every change goes to the model and then to {@code onChanged}, which
 * rebuilds the view.
 */
final class TraitSection {

  private static final String FULL_TOOLTIP = "Two traits are already chosen; uncheck one first.";
  /** Indent levels of a checkbox's description, so it lines up with the checkbox text. */
  private static final int UNDER_CHECKBOX = 2;

  private TraitSection() {}

  /**
   * @param locked whether the character is complete and not in GM mode (read-only)
   * @param onEdited called after an edit that needs no rebuild (typed purpose)
   * @param onChanged called after an edit that changes other values; rebuilds the view
   */
  static @NonNull MigPane build(@NonNull PlayerCharacter character, boolean locked, @NonNull Runnable onEdited,
      @NonNull Runnable onChanged) {
    MigPane pane = Layouts.tableWithGrowingLast(2);
    Optional<OriginData> origin = character.getOriginData();
    if (origin.isPresent()) {
      addOriginTrait(pane, origin.get(), locked);
      if (locked) {
        OriginSection.addChoices(pane, character.getOriginChoices());
      } else {
        OriginSection.addOptions(pane, character, onEdited, onChanged);
      }
    }
    for (TraitSelection trait : character.getTraits()) {
      // In creation the checkboxes show the traits of the origin's lists (imported ones marked there too);
      // others are listed as entries.
      if (locked || !offeredByOrigin(trait, origin)) {
        addSelection(pane, trait);
        if (!locked) {
          addAttributeChoosers(pane, character, trait, onChanged);
        }
      }
    }
    if (!locked && origin.isPresent()) {
      addChoice(pane, character, origin.get(), onChanged);
    }
    if (pane.getChildren().isEmpty()) {
      pane.add(new Label("None"), Layouts.span(2));
    }
    return pane;
  }

  /**
   * The origin's own trait, e.g. "The Chain that Binds (Origin Trait)" with its summary. Origins with
   * trait lists have none; during creation their summary ("Pick two …") introduces the choice instead.
   */
  private static void addOriginTrait(@NonNull MigPane pane, @NonNull OriginData origin, boolean locked) {
    String traitName = origin.traitName();
    if (traitName != null) {
      addEntry(pane, new Label(traitName + " (Origin Trait)"), origin.traitSummary(), origin.sources());
    } else if (!locked) {
      pane.add(descriptionLabel(origin.traitSummary(), origin.sources()), Layouts.span(2).growX());
    }
  }

  /** A chosen trait: resolved from the catalog (name with Gifted choices, description), else the reference. */
  private static void addSelection(@NonNull MigPane pane, @NonNull TraitSelection selection) {
    Optional<TraitDefinition> trait = TraitCatalog.byId(selection.trait().id());
    if (trait.isEmpty()) {
      pane.add(OverviewParts.importMarked(OverviewParts.reference(selection.trait()), selection.imported()),
          entryStart(pane));
      return;
    }
    String choices = selection.attributeChoices().isEmpty() ? ""
        : " (" + selection.attributeChoices().stream().map(Attribute::abbreviation)
            .collect(Collectors.joining(", ")) + ")";
    addEntry(pane, OverviewParts.importMarked(new Label(trait.get().name() + choices), selection.imported()),
        description(trait.get()), trait.get().sources());
  }

  /** Name in bold, below it the description with the sources, wrapping at the width of the half. */
  private static void addEntry(@NonNull MigPane pane, @NonNull Label name, @NonNull String description,
      @NonNull List<Source> sources) {
    name.setStyle("-fx-font-weight: bold;");
    pane.add(name, entryStart(pane));
    pane.add(descriptionLabel(description, sources), Layouts.span(2).growX());
  }

  /** The trait lists of the origin, one checkbox per trait with its benefit and penalty below. */
  private static void addChoice(@NonNull MigPane pane, @NonNull PlayerCharacter character, @NonNull OriginData origin,
      @NonNull Runnable onChanged) {
    Set<String> chosen = character.getTraits().stream().map(trait -> trait.trait().id())
        .collect(Collectors.toSet());
    Set<String> imported = character.getTraits().stream().filter(TraitSelection::imported)
        .map(trait -> trait.trait().id()).collect(Collectors.toSet());
    for (String list : origin.traitLists()) {
      Label title = new Label(listTitle(list));
      title.setStyle("-fx-font-weight: bold;");
      pane.add(title, entryStart(pane));
      for (TraitDefinition trait : TraitCatalog.inList(list)) {
        pane.add(checkBox(character, trait, chosen, imported.contains(trait.id()), onChanged),
            Layouts.spaced(Layouts.span(2)));
        pane.add(descriptionLabel(description(trait), trait.sources()),
            Layouts.indented(UNDER_CHECKBOX).spanX(2).growX());
        character.getTraits().stream().filter(selection -> selection.trait().id().equals(trait.id()))
            .findFirst().ifPresent(selection -> addAttributeChoosers(pane, character, selection, onChanged));
      }
    }
    if (TraitRules.takesExtraPerkInsteadOfTrait(character)) {
      pane.add(new Label("One trait chosen: you get an extra perk."), Layouts.spaced(Layouts.span(2)));
    }
  }

  /**
   * A checkbox per trait, marked if the trait was imported from a legacy file. Once two traits are chosen,
   * the others stay visible but disabled with the reason as tooltip; it sits on a wrapper because disabled
   * controls show no tooltip.
   */
  private static @NonNull HBox checkBox(@NonNull PlayerCharacter character, @NonNull TraitDefinition trait,
      @NonNull Set<String> chosen, boolean imported, @NonNull Runnable onChanged) {
    CheckBox box = OverviewParts.importMarked(new CheckBox(trait.name()), imported);
    boolean selected = chosen.contains(trait.id());
    box.setSelected(selected);
    boolean full = !selected && chosen.size() >= TraitRules.MAX_TRAITS;
    box.setDisable(full);
    box.selectedProperty().addListener((_, _, value) -> {
      if (value) {
        character.chooseTrait(CatalogRef.of(trait.id()));
      } else {
        character.removeTrait(trait.id());
      }
      onChanged.run();
    });
    HBox wrapper = new HBox(box);
    if (full) {
      Tooltip.install(wrapper, new Tooltip(FULL_TOOLTIP));
    }
    return wrapper;
  }

  /**
   * Gifted (CRB p. 56): one chooser per attribute the trait raises. The choices are stored as a
   * compact list, so a chooser is only enabled once all choosers before it are set – otherwise a
   * value picked in the second chooser would show up in the first after the rebuild.
   */
  private static void addAttributeChoosers(@NonNull MigPane pane, @NonNull PlayerCharacter character,
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
      pane.add(new Label("Attribute " + (i + 1)), Layouts.indented(UNDER_CHECKBOX));
      pane.add(chooser);
    }
    for (ComboBox<Attribute> chooser : choosers) {
      chooser.valueProperty().addListener((_, _, _) -> {
        List<Attribute> choices = choosers.stream().map(ComboBox::getValue)
            .filter(Objects::nonNull).toList();
        character.setTraitAttributeChoices(trait.get().id(), choices);
        onChanged.run();
      });
    }
  }

  /**
   * Whether the trait has a checkbox: a catalogued trait of one of the origin's trait lists. Other traits
   * (imported, unresolved, left over after an origin change) are listed as entries.
   */
  private static boolean offeredByOrigin(@NonNull TraitSelection selection, @NonNull Optional<OriginData> origin) {
    return origin.isPresent() && origin.get().traitLists().stream().anyMatch(list -> TraitCatalog.inList(list)
        .stream().anyMatch(trait -> trait.id().equals(selection.trait().id())));
  }

  /** A description with the sources on a line of their own, wrapping at the width of the half. */
  private static @NonNull Label descriptionLabel(@NonNull String description, @NonNull List<Source> sources) {
    return OverviewParts.wrappingInCell(new Label(description + "\n" + sources(sources)));
  }

  /** Benefit and penalty of a trait, one per line. */
  private static @NonNull String description(@NonNull TraitDefinition trait) {
    return "Benefit: " + trait.benefit() + "\nPenalty: " + trait.penalty();
  }

  /** Sources in brackets, e.g. {@code [CRB p. 56]}; always on a line of their own, so a wrap never splits them. */
  private static @NonNull String sources(@NonNull List<Source> sources) {
    return sources.stream().map(Source::toString).collect(Collectors.joining(", ", "[", "]"));
  }

  /** A cell over both columns, with space above unless it is the first one of the grid. */
  private static @NonNull CC entryStart(@NonNull MigPane pane) {
    return pane.getChildren().isEmpty() ? Layouts.span(2) : Layouts.spaced(Layouts.span(2));
  }

  private static @NonNull String listTitle(@NonNull String list) {
    return list.substring(0, 1).toUpperCase(Locale.ROOT) + list.substring(1) + " traits";
  }
}
