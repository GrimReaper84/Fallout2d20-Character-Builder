package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.catalog.loot.LootCategory;
import de.fallout2d20.charbuilder.model.scavenging.Find;
import de.fallout2d20.charbuilder.model.scavenging.FoundAmmunition;
import de.fallout2d20.charbuilder.view.Layouts;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.NonNull;

/**
 * The finds of a search or looted NPC, per category: result with a link to the compendium, why it was rolled, the
 * rolls behind it and the actions the caller offers. Holds no rules; the caller rebuilds the list after every action.
 */
final class FindList {

  private final @NonNull Predicate<Reference> isListed;
  private final @NonNull Consumer<Reference> open;
  private final @NonNull VBox rows = new VBox(Layouts.ROW_GAP);
  private final @NonNull ScrollPane node = new ScrollPane(rows);

  /**
   * Creates an empty list.
   *
   * @param isListed whether a link target is listed with the enabled books
   * @param open follows a link
   */
  FindList(@NonNull Predicate<Reference> isListed, @NonNull Consumer<Reference> open) {
    this.isListed = isListed;
    this.open = open;
    node.setFitToWidth(true);
  }

  @NonNull Node node() {
    return node;
  }

  void clear() {
    rows.getChildren().clear();
  }

  /**
   * Shows the finds.
   *
   * @param categories the categories to show, each with a heading even without finds
   * @param categoryActions the controls beside a category's heading
   * @param findActions the controls beside a find, by its index in {@code finds}
   */
  void show(@NonNull List<Find> finds, @NonNull List<LootCategory> categories,
      @NonNull Function<LootCategory, List<Node>> categoryActions, @NonNull IntFunction<List<Node>> findActions) {
    rows.getChildren().clear();
    for (LootCategory category : categories) {
      Label heading = new Label(category.displayName());
      heading.setStyle("-fx-font-weight: bold;");
      rows.getChildren().add(line(heading, categoryActions.apply(category)));
      for (int i = 0; i < finds.size(); i++) {
        if (finds.get(i).category() == category) {
          rows.getChildren().add(findRow(finds.get(i), findActions.apply(i)));
        }
      }
    }
  }

  private @NonNull Node findRow(@NonNull Find find, @NonNull List<Node> actions) {
    HBox result = new HBox(Layouts.GAP, resultNode(find.text(), FindReferences.of(find)));
    result.setAlignment(Pos.CENTER_LEFT);
    FoundAmmunition ammunition = find.ammunition();
    if (ammunition != null) {
      result.getChildren().add(resultNode(ammunition.text(), FindReferences.of(ammunition)));
    }
    if (!find.origin().label().isEmpty()) {
      result.getChildren().add(ScavengingControls.muted("[" + find.origin().label() + "]"));
    }
    VBox text = new VBox(result, ScavengingControls.muted(find.stepsText()));
    String note = find.note();
    if (note != null) {
      text.getChildren().add(ScavengingControls.muted(note));
    }
    text.setPadding(new Insets(0, 0, 0, Layouts.INDENT));
    return line(text, actions);
  }

  private @NonNull Node resultNode(@NonNull String text, @NonNull Optional<Reference> target) {
    Optional<Reference> reference = target.filter(isListed);
    if (reference.isEmpty()) {
      return new Label(text);
    }
    Hyperlink link = new Hyperlink(text);
    link.setOnAction(_ -> open.accept(reference.get()));
    return link;
  }

  private static @NonNull HBox line(@NonNull Node start, @NonNull List<Node> actions) {
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    List<Node> children = new ArrayList<>(List.of(start, spacer));
    children.addAll(actions);
    HBox line = new HBox(Layouts.GAP);
    line.getChildren().setAll(children);
    line.setAlignment(Pos.CENTER_LEFT);
    return line;
  }
}
