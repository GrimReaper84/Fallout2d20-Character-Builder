package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.perk.PerkChoice;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.creation.PerkRules;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.NonNull;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * One perk in the perk grid (user concept, Fallout 4 style): name, rank as filled and empty stars,
 * the requirements of the next rank, the choices made, the source and −/+ buttons. The tooltip shows
 * the summary and, if the next rank cannot be taken, why.
 */
final class PerkCard {

  private static final @NonNull String CARD_STYLE =
      "-fx-border-color: #9a9a9a; -fx-border-radius: 4; -fx-padding: 6; -fx-spacing: 3;";
  private static final double CARD_WIDTH = 220;
  private static final double UNAVAILABLE_OPACITY = 0.5;
  private static final int STAR_SIZE = 12;

  private PerkCard() {}

  /**
   * @param blockers why the next rank cannot be taken ({@link PerkRules#blockers}), computed once by the grid
   * @param available whether the card sits in the "chosen and available" area; otherwise it is greyed
   */
  static @NonNull VBox of(@NonNull Character character, @NonNull PerkDefinition perk,
      @NonNull List<String> blockers, boolean available, @NonNull Runnable onChanged) {
    int rank = PerkRules.rank(character, perk.id());
    List<String> invalid = PerkRules.unmetForTakenRank(character, perk);

    Label name = new Label(perk.name());
    name.setStyle("-fx-font-weight: bold;");
    HBox title = new HBox(4, name);
    if (!invalid.isEmpty()) {
      FontIcon warning = new FontIcon("fas-exclamation-triangle");
      Tooltip.install(warning, new Tooltip(String.join("\n", invalid)));
      title.getChildren().add(warning);
    }

    VBox card = new VBox(title, stars(rank, perk.ranks()), new Label(requirementLine(character, perk, rank)));
    String choices = choices(character, perk.id());
    if (!choices.isEmpty()) {
      card.getChildren().add(new Label("Chosen: " + choices));
    }
    card.getChildren().add(new Label(perk.sources().stream().map(Source::toString)
        .collect(Collectors.joining(", "))));
    boolean canAdd = PerkRules.perksLeft(character) > 0 && blockers.isEmpty();
    boolean canRemove = PerkRules.canRemove(character, perk.id());
    card.getChildren().add(StepButtons.of(
        StepButtons.minus(canRemove ? "Give back a rank of " + perk.name() : "No rank to give back.",
            canRemove, () -> {
              if (PerkRules.canRemove(character, perk.id())) {
                character.removePerkRank(perk.id());
              }
              onChanged.run();
            }),
        StepButtons.plus(canAdd ? "Take a rank of " + perk.name() : addBlocked(character, blockers),
            canAdd, () -> add(character, perk, onChanged))));
    card.setStyle(CARD_STYLE);
    card.setPrefWidth(CARD_WIDTH);
    if (!available) {
      card.setOpacity(UNAVAILABLE_OPACITY);
    }
    List<String> tooltip = new ArrayList<>(List.of(perk.summary()));
    tooltip.addAll(blockers);
    Tooltip.install(card, new Tooltip(String.join("\n", tooltip)));
    return card;
  }

  /** Filled stars for the ranks taken, empty ones for the rest. */
  private static @NonNull HBox stars(int rank, int ranks) {
    HBox stars = new HBox(1);
    for (int i = 0; i < ranks; i++) {
      FontIcon star = new FontIcon(i < rank ? "fas-star" : "far-star");
      star.setIconSize(STAR_SIZE);
      stars.getChildren().add(star);
    }
    return stars;
  }

  /** Requirements of the next rank, e.g. "Requires END 7, level 4, no robot". */
  private static @NonNull String requirementLine(@NonNull Character character, @NonNull PerkDefinition perk,
      int rank) {
    if (rank >= perk.ranks()) {
      return "Maximum rank reached";
    }
    Set<String> parts = PerkRules.requirementStatus(character, perk, rank + 1).keySet();
    return parts.isEmpty() ? "No requirements" : "Requires " + String.join(", ", parts);
  }

  /** The attributes or skills chosen for the perk's ranks, e.g. "AGI, STR". */
  private static @NonNull String choices(@NonNull Character character, @NonNull String perkId) {
    return character.getPerks().stream().filter(perk -> perk.perk().id().equals(perkId)).findFirst()
        .map(PerkCard::choiceText).orElse("");
  }

  /** The attributes (abbreviated) and skills chosen for a perk, e.g. "AGI, STR" or "Sneak". */
  static @NonNull String choiceText(@NonNull PerkSelection perk) {
    List<String> names = new ArrayList<>();
    perk.attributeChoices().forEach(attribute -> names.add(attribute.abbreviation()));
    perk.skillChoices().forEach(skill -> names.add(skill.displayName()));
    return String.join(", ", names);
  }

  private static @NonNull String addBlocked(@NonNull Character character, @NonNull List<String> blockers) {
    if (!blockers.isEmpty()) {
      return String.join("\n", blockers);
    }
    return PerkRules.perksLeft(character) <= 0 ? "No perk choices left." : "";
  }

  /**
   * Takes a rank; choice perks first ask for their attribute or skill (cancel adds nothing). Checked
   * again against the current model state, which may have changed since the card was built.
   */
  private static void add(@NonNull Character character, @NonNull PerkDefinition perk,
      @NonNull Runnable onChanged) {
    CatalogRef ref = CatalogRef.of(perk.id());
    if (!PerkRules.canAdd(character, perk)) {
      onChanged.run();
    } else if (perk.choice() == PerkChoice.ATTRIBUTE) {
      Optional<Attribute> attribute = choose("Choose the attribute " + perk.name() + " raises",
          PerkRules.attributeOptions(character), Attribute::displayName);
      attribute.ifPresent(choice -> character.addPerkRank(ref, choice, null));
    } else if (perk.choice() == PerkChoice.SKILL) {
      Optional<Skill> skill = choose("Choose the skill " + perk.name() + " tags",
          PerkRules.skillOptions(character), Skill::displayName);
      skill.ifPresent(choice -> character.addPerkRank(ref, null, choice));
    } else {
      character.addPerkRank(ref, null, null);
    }
    onChanged.run();
  }

  /** A choice dialog over display names; empty when cancelled or nothing can be chosen. */
  private static <T> @NonNull Optional<T> choose(@NonNull String header, @NonNull List<T> options,
      @NonNull Function<T, String> displayName) {
    if (options.isEmpty()) {
      return Optional.empty();
    }
    Map<String, T> byName = options.stream().collect(Collectors.toMap(displayName, option -> option,
        (first, second) -> first, LinkedHashMap::new));
    ChoiceDialog<String> dialog = new ChoiceDialog<>(byName.keySet().iterator().next(), byName.keySet());
    dialog.setTitle("Perk choice");
    dialog.setHeaderText(header);
    return dialog.showAndWait().map(byName::get);
  }
}
