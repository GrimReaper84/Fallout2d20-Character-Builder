package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.view.Layouts;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.jspecify.annotations.NonNull;

/**
 * The Scavenging section of the GM compendium: a location search or looting NPCs, each part in its own form. Nothing
 * here is saved; results vanish with the section.
 */
final class ScavengingSection {

  private static final double PADDING = 8;

  private final @NonNull BorderPane node = new BorderPane();

  ScavengingSection(@NonNull Set<Book> books, @NonNull RandomGenerator random, @NonNull Predicate<Reference> isListed,
      @NonNull Consumer<Reference> open) {
    Node location = new LocationForm(books, random, isListed, open).node();
    Node npc = new NpcLootForm(random, isListed, open).node();
    ToggleGroup group = new ToggleGroup();
    RadioButton locationChoice = new RadioButton("Location");
    RadioButton npcChoice = new RadioButton("NPC");
    locationChoice.setToggleGroup(group);
    npcChoice.setToggleGroup(group);
    locationChoice.setSelected(true);
    locationChoice.setOnAction(_ -> node.setCenter(location));
    npcChoice.setOnAction(_ -> node.setCenter(npc));
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox parts = new HBox(Layouts.GAP, locationChoice, npcChoice, spacer, new ScavengingHelp().button());
    parts.setAlignment(Pos.CENTER_LEFT);
    parts.setPadding(new Insets(0, 0, Layouts.GAP, 0));
    node.setTop(parts);
    node.setCenter(location);
    node.setPadding(new Insets(PADDING));
  }

  @NonNull Node node() {
    return node;
  }
}
