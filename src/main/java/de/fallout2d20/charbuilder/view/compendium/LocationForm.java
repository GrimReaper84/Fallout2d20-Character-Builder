package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.loot.LootCategory;
import de.fallout2d20.charbuilder.model.scavenging.FindReport;
import de.fallout2d20.charbuilder.model.scavenging.ItemRange;
import de.fallout2d20.charbuilder.model.scavenging.JunkMode;
import de.fallout2d20.charbuilder.model.scavenging.LocationDegree;
import de.fallout2d20.charbuilder.model.scavenging.LocationItems;
import de.fallout2d20.charbuilder.model.scavenging.LocationScale;
import de.fallout2d20.charbuilder.model.scavenging.LootOption;
import de.fallout2d20.charbuilder.model.scavenging.LootRoller;
import de.fallout2d20.charbuilder.model.scavenging.ScaleSuggestion;
import de.fallout2d20.charbuilder.model.scavenging.ScavengeSearch;
import de.fallout2d20.charbuilder.model.scavenging.SearchSetup;
import de.fallout2d20.charbuilder.model.scavenging.SearchTimes;
import de.fallout2d20.charbuilder.view.DisplayConverter;
import de.fallout2d20.charbuilder.view.Layouts;
import de.fallout2d20.charbuilder.view.WholeNumber;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.NonNull;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The Location part of the Scavenging section: describe the location, enter the test's successes, spend AP and Luck on
 * the finds. All rules are in {@link ScavengeSearch}.
 */
final class LocationForm {

  private static final int DEFAULT_LEVEL = 1;
  private static final int DEFAULT_LUCK = 5;

  private final boolean wandererGuide;
  private final @NonNull RandomGenerator random;
  private final @NonNull ComboBox<LocationScale> scale =
      new ComboBox<>(FXCollections.observableArrayList(LocationScale.values()));
  private final @NonNull ComboBox<LocationDegree> degree =
      new ComboBox<>(FXCollections.observableArrayList(LocationDegree.values()));
  private final @NonNull ComboBox<JunkMode> junkMode =
      new ComboBox<>(FXCollections.observableArrayList(JunkMode.values()));
  private final @NonNull TextField level = ScavengingControls.smallField(DEFAULT_LEVEL);
  private final @NonNull TextField successes = ScavengingControls.smallField(0);
  private final @NonNull TextField complications = ScavengingControls.smallField(0);
  private final @NonNull TextField groupAp = ScavengingControls.smallField(0);
  private final @NonNull TextField luck = ScavengingControls.smallField(DEFAULT_LUCK);
  private final @NonNull Map<LootCategory, TextField> minimums = new EnumMap<>(LootCategory.class);
  private final @NonNull Map<LootCategory, TextField> maximums = new EnumMap<>(LootCategory.class);
  private final @NonNull Label summary = new Label("Enter the location and the successes of the PER + Survival test.");
  private final @NonNull HBox searchActions = new HBox(Layouts.GAP);
  private final @NonNull FindList list;
  private final @NonNull VBox node;

  LocationForm(@NonNull Set<Book> books, @NonNull RandomGenerator random, @NonNull Predicate<Reference> isListed,
      @NonNull Consumer<Reference> open) {
    this.random = random;
    wandererGuide = books.contains(Book.WG);
    list = new FindList(isListed, open);
    scale.setConverter(new DisplayConverter<>(LocationScale::displayName));
    degree.setConverter(new DisplayConverter<>(value -> value.displayName() + " (difficulty " + value.difficulty() + ")"));
    degree.setValue(LocationDegree.UNTOUCHED);
    junkMode.setConverter(new DisplayConverter<>(LocationForm::junkModeText));
    junkMode.setValue(JunkMode.COUNT);
    junkMode.setDisable(!wandererGuide);
    HBox junkWrapper = new HBox(junkMode);
    if (!wandererGuide) {
      Tooltip.install(junkWrapper, new Tooltip("Detailed junk needs the Wanderer's Guide"));
    }
    Button searchButton = new Button("Search", new FontIcon("fas-dice-d20"));
    searchButton.setOnAction(_ -> search());
    luck.setTooltip(new Tooltip("LCK of the character spending Luck, for junk counts and the junk tables"));
    complications.setTooltip(new Tooltip("Complications the GM turns into lost time: each adds half the time"));
    groupAp.setTooltip(new Tooltip("AP of the group pool the group may spend here; normally at most 6"));
    HBox top = row(new Label("Scale"), scale, new Label("Degree"), degree, new Label("Level"), level,
        new Label("Successes"), successes, new Label("Complications"), complications, new Label("Group AP"), groupAp, searchButton);
    HBox second = row(new Label("Junk"), junkWrapper, new Label("LCK"), luck);
    MigPane grid = categoryGrid();
    Label suggestion = ScavengingControls.muted(
        "Min: rolls every successful search gets. Max: limit for extra rolls with AP. So Min 0 is only found with AP. "
            + "The values are suggestions for the scale (house rule, not from the book).");
    scale.valueProperty().addListener((_, _, chosen) -> fill(ScaleSuggestion.of(Objects.requireNonNull(chosen))));
    scale.setValue(LocationScale.AVERAGE);
    searchActions.setAlignment(Pos.CENTER_LEFT);
    node = new VBox(Layouts.GAP, top, second, suggestion, grid, summary, searchActions, list.node());
    VBox.setVgrow(list.node(), Priority.ALWAYS);
  }

  @NonNull Node node() {
    return node;
  }

  private @NonNull MigPane categoryGrid() {
    MigPane grid = Layouts.wideTable(6);
    for (int column = 0; column < 2; column++) {
      grid.add(new Label("Category"));
      grid.add(headerWithTooltip("Min", "Rolls every successful search gets"));
      grid.add(headerWithTooltip("Max", "Most rolls in this category; AP buy the rolls above Min"));
    }
    for (LootCategory category : LootCategory.values()) {
      TextField minimum = ScavengingControls.smallField(0);
      TextField maximum = ScavengingControls.smallField(0);
      minimums.put(category, minimum);
      maximums.put(category, maximum);
      grid.add(new Label(category.displayName()));
      grid.add(minimum);
      grid.add(maximum);
    }
    return grid;
  }

  private static @NonNull Label headerWithTooltip(@NonNull String text, @NonNull String tooltip) {
    Label header = new Label(text);
    header.setTooltip(new Tooltip(tooltip));
    return header;
  }

  private void fill(@NonNull LocationItems items) {
    for (LootCategory category : LootCategory.values()) {
      ItemRange range = items.range(category);
      field(minimums, category).setText(Integer.toString(range.min()));
      field(maximums, category).setText(Integer.toString(range.max()));
    }
  }

  private @NonNull LocationItems items() {
    Map<LootCategory, ItemRange> ranges = new EnumMap<>(LootCategory.class);
    for (LootCategory category : LootCategory.values()) {
      TextField minField = field(minimums, category);
      TextField maxField = field(maximums, category);
      int minimum = value(minField, 0);
      int maximum = Math.max(minimum, value(maxField, 0));
      minField.setText(Integer.toString(minimum));
      maxField.setText(Integer.toString(maximum));
      ranges.put(category, new ItemRange(minimum, maximum));
    }
    return new LocationItems(ranges);
  }

  private void search() {
    int locationLevel = value(level, DEFAULT_LEVEL);
    level.setText(Integer.toString(locationLevel));
    SearchSetup setup = new SearchSetup(items(), Objects.requireNonNull(scale.getValue()),
        Objects.requireNonNull(degree.getValue()), locationLevel, Objects.requireNonNull(junkMode.getValue()),
        wandererGuide);
    refresh(new ScavengeSearch(setup, value(successes, 0), value(complications, 0), value(groupAp, 0),
        new LootRoller(random)));
  }

  /** Shows the search; every action on it rebuilds this. */
  private void refresh(@NonNull ScavengeSearch search) {
    summary.setText(summaryText(search));
    searchActions.getChildren().clear();
    if (!search.succeeded()) {
      list.clear();
      return;
    }
    Button halve = new Button("Halve time (2 AP)");
    halve.setOnAction(_ -> act(search, search::halveTime));
    Button toMaximum = new Button("Roll to maximum");
    toMaximum.setTooltip(new Tooltip("Fills every category without AP; the finds are marked"));
    toMaximum.setOnAction(_ -> act(search, search::rollToMaximum));
    Button copy = new Button("Copy as text", new FontIcon("fas-copy"));
    copy.setOnAction(_ -> ScavengingControls.copyToClipboard(FindReport.text(summaryText(search), search.finds())));
    searchActions.getChildren().addAll(ScavengingControls.blockable(halve, search.halveTimeBlock()), toMaximum, copy);
    list.show(search.finds(), search.categories(), category -> categoryActions(search, category),
        index -> findActions(search, index));
  }

  private void act(@NonNull ScavengeSearch search, @NonNull Runnable action) {
    action.run();
    refresh(search);
  }

  private @NonNull List<Node> categoryActions(@NonNull ScavengeSearch search, @NonNull LootCategory category) {
    Label count = new Label(search.rolls(category) + " / " + search.maximum(category));
    Button extra = new Button("+ AP");
    extra.setOnAction(_ -> act(search, () -> search.addExtraRoll(category)));
    Button free = new Button("+ free");
    free.setTooltip(new Tooltip("Roll outside AP and maximum: Can Do!, Pharma Farma, a GM gift"));
    free.setOnAction(_ -> act(search, () -> search.addFreeRoll(category)));
    return List.of(count, ScavengingControls.blockable(extra, search.extraRollBlock(category)), free);
  }

  private @NonNull List<Node> findActions(@NonNull ScavengeSearch search, int index) {
    Button action = switch (search.findAction(index)) {
      case SHIFT_WITH_LUCK -> shiftButton(search, index, "Luck", "1 Luck: move the roll by up to the location's level");
      case ADJUST_JUNK -> shiftButton(search, index, "Adjust", "Move the roll by up to the LCK score; costs no Luck");
      case ADD_LUCK_TO_COUNT -> {
        Button more = new Button("Luck");
        more.setTooltip(new Tooltip("1 Luck: add the LCK score to the number of junk items"));
        more.setOnAction(_ -> act(search, () -> search.spendLuckOnJunkCount(index, luckScore())));
        yield more;
      }
    };
    List<Node> actions = new ArrayList<>(List.of(ScavengingControls.blockable(action, search.findActionBlock(index))));
    if (search.finds().get(index).magazine()) {
      Button rareBook = new Button("Rare Book");
      rareBook.setTooltip(new Tooltip("1 Luck: a Rare Book instead of this publication"));
      rareBook.setOnAction(_ -> act(search, () -> search.spendLuckOnRareBook(index)));
      actions.add(ScavengingControls.blockable(rareBook, search.rareBookBlock(index)));
    }
    return actions;
  }

  private @NonNull Button shiftButton(@NonNull ScavengeSearch search, int index, @NonNull String text,
      @NonNull String tooltip) {
    Button button = new Button(text);
    button.setTooltip(new Tooltip(tooltip));
    button.setOnAction(_ -> showOptions(button, search.shiftOptions(index, luckScore()),
        roll -> act(search, () -> search.shift(index, roll))));
    return button;
  }

  /** The typed LCK score; anything but a number of at least 1 goes back to the default. */
  private int luckScore() {
    Integer typed = WholeNumber.atLeast(luck.getText(), 1);
    if (typed == null) {
      luck.setText(Integer.toString(DEFAULT_LUCK));
      return DEFAULT_LUCK;
    }
    return typed;
  }

  private static void showOptions(@NonNull Button owner, @NonNull List<LootOption> options,
      @NonNull IntConsumer choose) {
    ContextMenu menu = new ContextMenu();
    for (LootOption option : options) {
      MenuItem item = new MenuItem(option.roll() + ": " + option.row().label());
      item.setOnAction(_ -> choose.accept(option.roll()));
      menu.getItems().add(item);
    }
    if (options.isEmpty()) {
      MenuItem none = new MenuItem("No other row within reach");
      none.setDisable(true);
      menu.getItems().add(none);
    }
    menu.show(owner, Side.BOTTOM, 0, 0);
  }

  private static @NonNull String summaryText(@NonNull ScavengeSearch search) {
    String test = "Difficulty " + search.difficulty() + ", " + search.successes()
        + (search.successes() == 1 ? " success" : " successes");
    if (!search.succeeded()) {
      return test + ": nothing found. Another attempt takes " + SearchTimes.text(search.retryTime()) + " again.";
    }
    return test + ": " + search.apLeft() + " AP left (" + search.generatedAp() + " from the test, " + search.groupAp()
        + " from the group), time " + SearchTimes.text(search.time())
        + (search.timeHalved() ? " (halved)" : "") + ", Luck spent " + search.luckSpent();
  }

  private static @NonNull String junkModeText(@NonNull JunkMode mode) {
    return switch (mode) {
      case COUNT -> "Junk count (Core Rulebook)";
      case DETAILED -> "Detailed junk (Wanderer's Guide)";
    };
  }

  private static @NonNull HBox row(@NonNull Node... nodes) {
    HBox row = new HBox(Layouts.GAP, nodes);
    row.setAlignment(Pos.CENTER_LEFT);
    return row;
  }

  private static @NonNull TextField field(@NonNull Map<LootCategory, TextField> fields, @NonNull LootCategory category) {
    return Objects.requireNonNull(fields.get(category));
  }

  /** The field's number; anything but a whole number of at least 0 counts as the fallback. */
  private static int value(@NonNull TextField field, int fallback) {
    Integer typed = WholeNumber.atLeast(field.getText(), 0);
    return typed == null ? fallback : typed;
  }
}
