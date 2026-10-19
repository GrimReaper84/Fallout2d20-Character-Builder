package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.origin.OriginCatalog;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.model.character.creation.HintArea;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.jspecify.annotations.NonNull;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The bar above the tabs: name, origin, level and XP in the first row; below, depending on the mode, the
 * "Create Character" button with the hint counter and the starting caps (creation), or adding XP and the
 * pending level-up points with Apply/Discard (after creation). In GM mode a banner on top and level/XP as
 * free fields. Origin and level are editable during creation (and in GM mode), labels afterwards.
 */
final class CharacterHeader {

  private CharacterHeader() {}

  /** @param hints all hints of the character, each with its area */
  static @NonNull Node build(@NonNull PlayerCharacter character, @NonNull List<CreationProblem> hints,
      @NonNull ViewCallbacks callbacks) {
    MigPane pane = new MigPane("insets 12 16 8 16, gap 8 4, wrap 8",
        "[right][grow,fill][right][grow,fill][right][][right][]");
    boolean complete = character.isCreationComplete();
    boolean gm = character.isGmMode();
    if (gm) {
      Label banner = new Label("GM mode: budgets and level-up points do not apply; every change counts at once.");
      banner.setStyle(OverviewParts.HINT_STYLE + " -fx-font-weight: bold;");
      banner.setWrapText(true);
      pane.add(banner, "span 8");
    }

    TextField name = new TextField(character.getName());
    name.setPromptText("Name");
    name.textProperty().addListener((obs, old, value) -> {
      character.setName(value);
      callbacks.onEdited().run();
    });
    pane.add(new Label("Name"));
    pane.add(name);
    pane.add(new Label("Origin"));
    pane.add(!complete || gm ? OriginSection.choice(character, callbacks.onChanged()) : origin(character));
    pane.add(new Label("Level"));
    if (!complete) {
      pane.add(LevelSection.control(character, callbacks.onChanged()));
    } else if (gm) {
      pane.add(GmLevelSection.levelField(character, callbacks.onChanged()));
    } else {
      pane.add(new Label(Integer.toString(character.getLevel())));
    }
    pane.add(new Label("XP"));
    pane.add(complete && gm ? GmLevelSection.xpField(character, callbacks.onChanged())
        : new Label(Long.toString(character.getXp())));

    HBox actions = new HBox(12);
    actions.setAlignment(Pos.CENTER_LEFT);
    if (!complete) {
      actions.getChildren().add(createButton(callbacks.onComplete()));
      actions.getChildren().add(hintCounter(hints));
      Label caps = LevelSection.startingCaps(character);
      if (caps != null) {
        actions.getChildren().add(caps);
      }
    } else if (!gm) {
      actions.getChildren().add(AdvancementSection.gainXp(character, callbacks.onChanged()));
      Node pending = AdvancementSection.pending(character, callbacks.onChanged());
      if (pending != null) {
        actions.getChildren().add(pending);
      }
    }
    List<CreationProblem> headerHints = hints.stream().filter(hint -> hint.area() == HintArea.HEADER).toList();
    if (!headerHints.isEmpty()) {
      actions.getChildren().add(OverviewParts.wrapping(OverviewParts.hintLabel(headerHints)));
    }
    if (!actions.getChildren().isEmpty()) {
      pane.add(actions, "span 8, growx");
    }
    return pane;
  }

  /** CRB p. 50: ends creation after the last step; the caller confirms open hints first. */
  private static @NonNull Button createButton(@NonNull Runnable onComplete) {
    Button create = new Button("Create Character");
    create.setGraphic(new FontIcon("fas-check"));
    create.setTooltip(new Tooltip("End character creation; creation choices cannot be changed afterwards."));
    create.setOnAction(event -> onComplete.run());
    return create;
  }

  /** "3 open hints" with the hints and their tabs as tooltip, or "No open hints". */
  private static @NonNull Label hintCounter(@NonNull List<CreationProblem> hints) {
    if (hints.isEmpty()) {
      return new Label("No open hints");
    }
    Label counter = new Label(hints.size() + (hints.size() == 1 ? " open hint" : " open hints"));
    counter.setGraphic(warningIcon());
    counter.setStyle(OverviewParts.HINT_STYLE);
    counter.setTooltip(new Tooltip(hints.stream().map(hint -> CharacterTab.titleOf(hint.area()) + ": " + hint.message())
        .collect(Collectors.joining("\n"))));
    return counter;
  }

  /** The warning icon used by the hint counter and the tab markers. */
  static @NonNull FontIcon warningIcon() {
    FontIcon icon = new FontIcon("fas-exclamation-triangle");
    icon.setIconColor(Color.web("#a35200"));
    return icon;
  }

  /** Origin of a complete character from the catalog (name, sources, trait summary), else the reference. */
  private static @NonNull Label origin(@NonNull PlayerCharacter character) {
    CatalogRef ref = character.getOrigin();
    if (ref == null) {
      return new Label("Not chosen");
    }
    Optional<OriginData> origin = OriginCatalog.byId(ref.id());
    if (origin.isEmpty()) {
      return OverviewParts.reference(ref);
    }
    Label label = new Label(origin.get().name());
    label.setTooltip(new Tooltip(OriginSection.describe(origin.get())));
    return label;
  }
}
