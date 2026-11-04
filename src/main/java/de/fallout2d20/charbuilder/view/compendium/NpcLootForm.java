package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.catalog.loot.LootCategory;
import de.fallout2d20.charbuilder.catalog.loot.LootRow;
import de.fallout2d20.charbuilder.model.scavenging.Find;
import de.fallout2d20.charbuilder.model.scavenging.FindReport;
import de.fallout2d20.charbuilder.model.scavenging.NpcLoot;
import de.fallout2d20.charbuilder.view.DisplayConverter;
import de.fallout2d20.charbuilder.view.Layouts;
import de.fallout2d20.charbuilder.view.WholeNumber;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.NonNull;
import org.kordamp.ikonli.javafx.FontIcon;

/** The NPC part of the Scavenging section: loots defeated NPCs (rules in {@link NpcLoot}). */
final class NpcLootForm {

  private static final int DEFAULT_WEALTH = 1;
  private static final @NonNull String REMINDER = "Also on the NPC: the listed inventory and consumables not used in "
      + "the scene. Animals can be butchered, robots salvaged, as their description says.";

  private final @NonNull NpcLoot loot;
  private final @NonNull TextField npcs = ScavengingControls.smallField(1);
  private final @NonNull TextField wealth = ScavengingControls.smallField(DEFAULT_WEALTH);
  private final @NonNull CheckBox prolific = new CheckBox("Prolific scavengers");
  private final @NonNull ComboBox<Optional<LootRow>> ammunition = new ComboBox<>();
  private final @NonNull Button copy = new Button("Copy as text", new FontIcon("fas-copy"));
  private final @NonNull FindList list;
  private final @NonNull VBox node;
  private @NonNull List<Find> finds = List.of();

  NpcLootForm(@NonNull RandomGenerator random, @NonNull Predicate<Reference> isListed,
      @NonNull Consumer<Reference> open) {
    loot = new NpcLoot(random);
    list = new FindList(isListed, open);
    List<Optional<LootRow>> choices = new ArrayList<>(List.of(Optional.empty()));
    NpcLoot.ammunitionRows().forEach(row -> choices.add(Optional.of(row)));
    ammunition.setItems(FXCollections.observableArrayList(choices));
    ammunition.setConverter(new DisplayConverter<>(choice -> choice.map(LootRow::label).orElse("No ranged weapon")));
    ammunition.setValue(Optional.empty());
    Button lootButton = new Button("Loot", new FontIcon("fas-dice-d20"));
    lootButton.setOnAction(_ -> loot());
    copy.setDisable(true);
    copy.setOnAction(_ -> ScavengingControls.copyToClipboard(FindReport.text("NPC loot", finds)));
    HBox top = new HBox(Layouts.GAP, new Label("NPCs"), npcs, new Label("Wealth"), wealth, new Label("Ammunition"),
        ammunition, prolific, lootButton, copy);
    top.setAlignment(Pos.CENTER_LEFT);
    node = new VBox(Layouts.GAP, top, ScavengingControls.muted(REMINDER), list.node());
    VBox.setVgrow(list.node(), Priority.ALWAYS);
  }

  @NonNull Node node() {
    return node;
  }

  private void loot() {
    Integer typedNpcs = WholeNumber.atLeast(npcs.getText(), 1);
    Integer typedWealth = WholeNumber.atLeast(wealth.getText(), 0);
    Optional<LootRow> row = ammunition.getValue();
    finds = loot.loot(typedNpcs == null ? 1 : typedNpcs, typedWealth == null ? DEFAULT_WEALTH : typedWealth,
        row == null ? null : row.orElse(null), prolific.isSelected());
    copy.setDisable(false);
    list.show(finds, Arrays.stream(LootCategory.values())
        .filter(category -> finds.stream().anyMatch(find -> find.category() == category)).toList(),
        _ -> List.of(), _ -> List.of());
  }
}
