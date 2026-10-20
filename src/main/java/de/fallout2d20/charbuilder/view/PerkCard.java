package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.perk.PerkChoice;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.creation.PerkRules;
import de.fallout2d20.charbuilder.view.dialog.ConfirmDialog;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * One perk in the perk grid (user concept, Fallout 4 style): name with an info button, rank as filled and
 * empty stars, the requirements of the next rank, the choices made and, while perks can be changed, −/+
 * buttons. The info button shows the summary, the source and, if the next rank cannot be taken, why.
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
   * @param editable whether perks can be changed now (creation, GM mode, pending level-up perks)
   */
  static @NonNull VBox of(@NonNull PlayerCharacter character, @NonNull PerkDefinition perk,
      @NonNull List<String> blockers, boolean editable, @NonNull Runnable onChanged) {
    Optional<PerkSelection> selection = character.getPerks().stream()
        .filter(taken -> taken.perk().id().equals(perk.id())).findFirst();
    int rank = PerkRules.rank(character, perk.id());
    List<String> invalid = PerkRules.unmetForTakenRank(character, perk);

    boolean imported = selection.map(taken -> taken.importedRanks() > 0).orElse(false);
    StackPane info = PerkInfo.button();
    HBox title = title(OverviewParts.importMarked(new Label(perk.name()), imported), info);
    if (!invalid.isEmpty()) {
      FontIcon warning = new FontIcon("fas-exclamation-triangle");
      Tooltip.install(warning, new Tooltip(String.join("\n", invalid)));
      title.getChildren().add(1, warning);
    }

    VBox card = new VBox(title, stars(rank, perk.ranks()), new Label(requirementLine(character, perk, rank)));
    String choices = selection.map(PerkSelection::choiceText).orElse("");
    if (!choices.isEmpty()) {
      card.getChildren().add(new Label("Chosen: " + choices));
    }
    int staged = selection.map(PerkSelection::stagedRanks).orElse(0);
    if (staged > 0) {
      Label stagedLabel = new Label(staged + " rank" + (staged == 1 ? "" : "s") + " staged, not applied yet");
      stagedLabel.setStyle("-fx-font-style: italic;");
      card.getChildren().add(stagedLabel);
    }
    if (editable) {
      card.getChildren().add(buttons(character, perk, blockers, card, onChanged));
    }
    style(card);
    PerkInfo.attach(info, card, infoText(perk, blockers, rank > 0));
    // Not available: not taken and the next rank is blocked.
    if (rank == 0 && !blockers.isEmpty()) {
      card.setOpacity(UNAVAILABLE_OPACITY);
    }
    return card;
  }

  /**
   * A perk taken from a legacy file that is not in the catalog: name and rank only, shown in the chosen
   * group so it is not lost from sight.
   */
  static @NonNull VBox unresolved(@NonNull PerkSelection selection) {
    VBox card = new VBox(title(OverviewParts.importMarked(OverviewParts.reference(selection.perk()),
        selection.importedRanks() > 0)), new Label("Rank " + selection.rank()));
    String choices = selection.choiceText();
    if (!choices.isEmpty()) {
      card.getChildren().add(new Label("Chosen: " + choices));
    }
    style(card);
    return card;
  }

  /** The bold name, then the given nodes (the info button goes to the right edge). */
  private static @NonNull HBox title(@NonNull Label name, @NonNull Node... after) {
    name.setStyle("-fx-font-weight: bold;");
    HBox title = new HBox(4, name);
    if (after.length > 0) {
      Region spacer = new Region();
      HBox.setHgrow(spacer, Priority.ALWAYS);
      title.getChildren().add(spacer);
      title.getChildren().addAll(after);
    }
    return title;
  }

  private static void style(@NonNull VBox card) {
    card.setStyle(CARD_STYLE);
    card.setPrefWidth(CARD_WIDTH);
  }

  /**
   * Summary, sources and, if the next rank cannot be taken, the reasons: "Not available" for a perk not
   * taken yet, "Next rank" for a taken one (e.g. at its maximum rank).
   */
  private static @NonNull String infoText(@NonNull PerkDefinition perk, @NonNull List<String> blockers,
      boolean taken) {
    StringBuilder text = new StringBuilder(perk.summary()).append("\n[")
        .append(perk.sources().stream().map(Source::toString).collect(Collectors.joining(", "))).append("]");
    if (!blockers.isEmpty()) {
      text.append("\n\n").append(taken ? "Next rank:" : "Not available:").append("\n")
          .append(String.join("\n", blockers));
    }
    return text.toString();
  }

  /** The −/+ buttons; their tooltips name the reason when a button is disabled. */
  private static @NonNull HBox buttons(@NonNull PlayerCharacter character, @NonNull PerkDefinition perk,
      @NonNull List<String> blockers, @NonNull Node card, @NonNull Runnable onChanged) {
    boolean canAdd = PerkRules.canAdd(character, perk);
    boolean canRemove = PerkRules.canRemove(character, perk.id());
    return StepButtons.of(
        StepButtons.minus(canRemove ? "Give back a rank of " + perk.name() : "No rank to give back.",
            canRemove, () -> {
              if (PerkRules.canRemove(character, perk.id()) && confirmImportedRemoval(character, perk, card)) {
                character.removePerkRank(perk.id());
              }
              onChanged.run();
            }),
        StepButtons.plus(canAdd ? "Take a rank of " + perk.name() : addBlocked(character, blockers),
            canAdd, () -> add(character, perk, card, onChanged)));
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
  private static @NonNull String requirementLine(@NonNull PlayerCharacter character, @NonNull PerkDefinition perk,
      int rank) {
    if (rank >= perk.ranks()) {
      return "Maximum rank reached";
    }
    Set<String> parts = PerkRules.requirementStatus(character, perk, rank + 1).keySet();
    return parts.isEmpty() ? "No requirements" : "Requires " + String.join(", ", parts);
  }

  private static @NonNull String addBlocked(@NonNull PlayerCharacter character, @NonNull List<String> blockers) {
    if (!blockers.isEmpty()) {
      return String.join("\n", blockers);
    }
    return PerkRules.perksLeft(character) <= 0 ? "No perk choices left." : "";
  }

  /**
   * Takes a rank; choice perks first ask for their attribute or skill (cancel adds nothing). Checked
   * again against the current model state, which may have changed since the card was built.
   */
  private static void add(@NonNull PlayerCharacter character, @NonNull PerkDefinition perk, @NonNull Node owner,
      @NonNull Runnable onChanged) {
    CatalogRef ref = CatalogRef.of(perk.id());
    if (!PerkRules.canAdd(character, perk)) {
      onChanged.run();
    } else if (perk.choice() == PerkChoice.ATTRIBUTE) {
      Optional<Attribute> attribute = choose(owner, "Choose the attribute " + perk.name() + " raises",
          PerkRules.attributeOptions(character), Attribute::displayName);
      attribute.ifPresent(choice -> character.addPerkRank(ref, choice, null));
    } else if (perk.choice() == PerkChoice.SKILL) {
      Optional<Skill> skill = choose(owner, "Choose the skill " + perk.name() + " tags",
          PerkRules.skillOptions(character), Skill::displayName);
      skill.ifPresent(choice -> character.addPerkRank(ref, null, choice));
    } else {
      character.addPerkRank(ref, null, null);
    }
    onChanged.run();
  }

  /** A choice dialog over display names; empty when cancelled or nothing can be chosen. */
  private static <T> @NonNull Optional<T> choose(@NonNull Node owner, @NonNull String header,
      @NonNull List<T> options, @NonNull Function<T, String> displayName) {
    if (options.isEmpty()) {
      return Optional.empty();
    }
    Map<String, T> byName = options.stream().collect(Collectors.toMap(displayName, option -> option,
        (first, _) -> first, LinkedHashMap::new));
    ChoiceDialog<String> dialog = new ChoiceDialog<>(byName.keySet().iterator().next(), byName.keySet());
    if (owner.getScene() != null) {
      dialog.initOwner(owner.getScene().getWindow());
    }
    dialog.setTitle("Perk choice");
    dialog.setHeaderText(header);
    return dialog.showAndWait().map(byName::get);
  }

  /**
   * Asks before the GM removes an imported rank (D2): its effect may already be part of the imported
   * attribute and skill values, which are not recalculated.
   */
  private static boolean confirmImportedRemoval(@NonNull PlayerCharacter character, @NonNull PerkDefinition perk,
      @NonNull Node owner) {
    boolean imported = character.getPerks().stream()
        .anyMatch(selection -> selection.perk().id().equals(perk.id()) && selection.lastRankImported());
    if (!imported) {
      return true;
    }
    Window window = owner.getScene() == null ? null : owner.getScene().getWindow();
    return ConfirmDialog.confirm(window, "Remove imported perk rank", "Remove a rank of " + perk.name() + "?",
        "This rank came from the old save. Its effect may already be part of the imported attribute and skill "
            + "values, which are not recalculated.", "Remove");
  }
}
