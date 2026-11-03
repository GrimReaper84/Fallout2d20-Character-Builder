package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.party.Party;
import de.fallout2d20.charbuilder.model.party.PartyMember;
import de.fallout2d20.charbuilder.view.Layouts;
import de.fallout2d20.charbuilder.view.WholeNumber;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The Party section of the GM compendium (H2): the GM's own quick overview of the group, saved as a party file. A
 * sortable table (the shown order is the saved order), a form for the selected member with quick buttons for the
 * frequent changes, and "Take values from character" copying an open character's values once (no link, nothing is
 * written back; user decisions 2026-11-02).
 */
final class PartySection {

  private static final double PADDING = 8;
  private static final double TEXT_FIELD_WIDTH = 220;
  private static final double NUMBER_FIELD_WIDTH = 60;

  private final @NonNull Supplier<List<PlayerCharacter>> openCharacters;
  private final @NonNull ObservableList<PartyMember> rows;
  private final @NonNull TableView<PartyMember> table;
  private final @NonNull TextField name = new TextField();
  private final @NonNull TextField player = new TextField();
  private final @NonNull TextField injuries = new TextField();
  private final @NonNull TextField level = new TextField();
  private final @NonNull TextField currentHp = new TextField();
  private final @NonNull TextField maxHp = new TextField();
  private final @NonNull TextField rads = new TextField();
  private final @NonNull TextField initiative = new TextField();
  private final @NonNull Button hpDown = new Button("HP −1");
  private final @NonNull Button hpUp = new Button("HP +1");
  private final @NonNull Button fullHp = new Button("Full HP");
  private final @NonNull Button clearRads = new Button("Clear rads");
  private final @NonNull VBox form;
  /** While a form change replaces the selected row, so the selection listener does not react. */
  private boolean changing;
  private final @NonNull VBox node;

  /**
   * @param openCharacters the characters of the open tabs, for "Take values from character"
   * @param onChanged after every change of the party (marks it unsaved)
   */
  PartySection(@NonNull Party party, @NonNull Supplier<List<PlayerCharacter>> openCharacters,
      @NonNull Runnable onChanged) {
    this.openCharacters = openCharacters;
    rows = FXCollections.observableArrayList(party.members());
    table = new TableView<>(rows);
    table.setPlaceholder(new Label("No members yet. Add one, or take the values of an open character."));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.getColumns().setAll(List.of(textColumn("Name", PartyMember::name), textColumn("Player", PartyMember::player),
        numberColumn("Level", PartyMember::level), hpColumn(), numberColumn("Rads", PartyMember::rads),
        numberColumn("Initiative", PartyMember::initiative), textColumn("Injuries", PartyMember::injuries)));
    // Sorting by a column reorders the rows; the party keeps the shown order.
    rows.addListener((ListChangeListener<PartyMember>) _ -> {
      party.reorder(new ArrayList<>(rows));
      onChanged.run();
    });
    table.getSelectionModel().selectedIndexProperty().addListener((_, _, _) -> {
      if (!changing) {
        refreshForm();
      }
    });

    TextField partyName = new TextField(party.getName());
    partyName.setPromptText("Party name");
    partyName.setPrefWidth(TEXT_FIELD_WIDTH);
    partyName.textProperty().addListener((_, _, value) -> {
      party.setName(value);
      onChanged.run();
    });
    form = buildForm();
    HBox top = new HBox(Layouts.GAP, new Label("Party"), partyName, addButton(), takeValuesButton(), removeButton());
    top.setAlignment(Pos.CENTER_LEFT);
    node = new VBox(Layouts.GAP, top, table, form);
    node.setPadding(new Insets(PADDING));
    VBox.setVgrow(table, Priority.ALWAYS);
    refreshForm();
  }

  @NonNull Node node() {
    return node;
  }

  private @NonNull Button addButton() {
    Button add = new Button("Add member");
    add.setOnAction(_ -> {
      rows.add(PartyMember.NEW);
      table.getSelectionModel().select(rows.size() - 1);
    });
    return add;
  }

  private @NonNull Button removeButton() {
    Button remove = new Button("Remove");
    remove.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
    remove.setOnAction(_ -> {
      int index = table.getSelectionModel().getSelectedIndex();
      if (index >= 0) {
        rows.remove(index);
      }
    });
    return remove;
  }

  /** Copies an open character's values into the selected member (keeping its player) or into a new one. */
  private @NonNull MenuButton takeValuesButton() {
    MenuButton take = new MenuButton("Take values from character");
    take.setTooltip(new Tooltip("Copies level, HP, rads, Initiative and injuries of an open character once, into the "
        + "selected member or a new one. The party keeps no link to the character."));
    take.setOnShowing(_ -> {
      take.getItems().clear();
      for (PlayerCharacter character : openCharacters.get()) {
        String characterName = character.getName().isBlank() ? "Unnamed" : character.getName();
        MenuItem item = new MenuItem(characterName);
        item.setOnAction(_ -> takeValues(character));
        take.getItems().add(item);
      }
      if (take.getItems().isEmpty()) {
        MenuItem none = new MenuItem("No character is open");
        none.setDisable(true);
        take.getItems().add(none);
      }
    });
    return take;
  }

  private void takeValues(@NonNull PlayerCharacter character) {
    int index = table.getSelectionModel().getSelectedIndex();
    if (index < 0) {
      rows.add(PartyMember.of(character, ""));
      table.getSelectionModel().select(rows.size() - 1);
    } else {
      apply(member -> PartyMember.of(character, member.player()));
    }
  }

  /**
   * The form for the selected member, built once: a commit (Enter or leaving the field) changes the member at once
   * and only updates the fields, so the focus moves on as usual and a click on a quick button right after typing
   * works on the changed member.
   */
  private @NonNull VBox buildForm() {
    MigPane fields = Layouts.table(8);
    fields.add(new Label("Name"));
    fields.add(textField(name, PartyMember::withName));
    fields.add(new Label("Player"));
    fields.add(textField(player, PartyMember::withPlayer));
    fields.add(new Label("Injuries"));
    fields.add(textField(injuries, PartyMember::withInjuries), Layouts.span(3));
    fields.add(new Label("Level"));
    fields.add(numberField(level, PartyMember::withLevel));
    fields.add(new Label("HP"));
    fields.add(numberField(currentHp, PartyMember::withCurrentHp));
    fields.add(new Label("Max HP"));
    fields.add(numberField(maxHp, PartyMember::withMaxHp));
    fields.add(new Label("Rads"));
    fields.add(numberField(rads, PartyMember::withRads));
    fields.add(new Label("Initiative"));
    fields.add(numberField(initiative, PartyMember::withInitiative));
    quick(hpDown, "One HP less", member -> member.withCurrentHp(member.currentHp() - 1));
    quick(hpUp, "One HP more", member -> member.withCurrentHp(member.currentHp() + 1));
    quick(fullHp, "Current HP at the maximum lowered by rads", PartyMember::healed);
    quick(clearRads, "No rads; HP are not healed (CRB p. 35)", member -> member.withRads(0));
    return new VBox(Layouts.GAP, fields, new HBox(Layouts.GAP, hpDown, hpUp, fullHp, clearRads));
  }

  /** The selected member, if any. */
  private @NonNull Optional<PartyMember> selected() {
    int index = table.getSelectionModel().getSelectedIndex();
    return index < 0 || index >= rows.size() ? Optional.empty() : Optional.of(rows.get(index));
  }

  /** Applies a change to the selected member; the selection stays on it. */
  private void apply(@NonNull UnaryOperator<PartyMember> change) {
    int index = table.getSelectionModel().getSelectedIndex();
    if (index >= 0 && index < rows.size()) {
      PartyMember changed = change.apply(rows.get(index));
      if (!changed.equals(rows.get(index))) {
        changing = true;
        rows.set(index, changed);
        table.getSelectionModel().select(index);
        changing = false;
      }
    }
    refreshForm();
  }

  /** Shows the selected member's values in the form; the form is disabled while none is selected. */
  private void refreshForm() {
    Optional<PartyMember> member = selected();
    form.setDisable(member.isEmpty());
    PartyMember shown = member.orElse(PartyMember.NEW);
    show(name, member.isEmpty() ? "" : shown.name());
    show(player, shown.player());
    show(injuries, shown.injuries());
    show(level, Integer.toString(shown.level()));
    show(currentHp, Integer.toString(shown.currentHp()));
    show(maxHp, Integer.toString(shown.maxHp()));
    show(rads, Integer.toString(shown.rads()));
    show(initiative, Integer.toString(shown.initiative()));
    hpDown.setDisable(shown.currentHp() <= 0);
    hpUp.setDisable(shown.currentHp() >= shown.effectiveMaxHp());
    fullHp.setDisable(shown.currentHp() >= shown.effectiveMaxHp());
    clearRads.setDisable(shown.rads() <= 0);
  }

  /** Sets the text unless the field already shows it (keeps the caret where it is). */
  private static void show(@NonNull TextField field, @NonNull String text) {
    if (!field.getText().equals(text)) {
      field.setText(text);
    }
  }

  private void quick(@NonNull Button button, @NonNull String tooltip, @NonNull UnaryOperator<PartyMember> change) {
    button.setTooltip(new Tooltip(tooltip));
    button.setOnAction(_ -> apply(change));
  }

  /** A text field taking its value on Enter or when it loses the focus. */
  private @NonNull TextField textField(@NonNull TextField field,
      @NonNull BiFunction<PartyMember, String, PartyMember> change) {
    field.setPrefWidth(TEXT_FIELD_WIDTH);
    onCommit(field, () -> apply(member -> change.apply(member, field.getText().strip())));
    return field;
  }

  /** A number field taking a whole number (at least 0) on Enter or when it loses the focus; else it resets. */
  private @NonNull TextField numberField(@NonNull TextField field,
      @NonNull BiFunction<PartyMember, Integer, PartyMember> change) {
    field.setPrefWidth(NUMBER_FIELD_WIDTH);
    onCommit(field, () -> {
      Integer typed = WholeNumber.atLeast(field.getText(), 0);
      if (typed == null) {
        refreshForm();
      } else {
        apply(member -> change.apply(member, typed));
      }
    });
    return field;
  }

  private static void onCommit(@NonNull TextField field, @NonNull Runnable commit) {
    field.setOnAction(_ -> commit.run());
    field.focusedProperty().addListener((_, _, focused) -> {
      if (!focused) {
        commit.run();
      }
    });
  }

  private static @NonNull TableColumn<PartyMember, String> textColumn(@NonNull String header,
      @NonNull Function<PartyMember, String> text) {
    TableColumn<PartyMember, String> column = new TableColumn<>(header);
    column.setCellValueFactory(cell -> new SimpleStringProperty(text.apply(cell.getValue())));
    column.setComparator(String.CASE_INSENSITIVE_ORDER);
    return column;
  }

  private static @NonNull TableColumn<PartyMember, Integer> numberColumn(@NonNull String header,
      @NonNull ToIntFunction<PartyMember> number) {
    TableColumn<PartyMember, Integer> column = new TableColumn<>(header);
    column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(number.applyAsInt(cell.getValue())));
    return column;
  }

  /** "HP" as "current/max", e.g. "9/12", with the rad-lowered maximum when rads are taken; sorted by current HP. */
  private static @NonNull TableColumn<PartyMember, PartyMember> hpColumn() {
    TableColumn<PartyMember, PartyMember> column = new TableColumn<>("HP");
    column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
    column.setComparator(Comparator.comparingInt(PartyMember::currentHp));
    column.setCellFactory(_ -> new TableCell<>() {
      @Override
      protected void updateItem(@Nullable PartyMember member, boolean empty) {
        super.updateItem(member, empty);
        setText(empty || member == null ? null : hpText(member));
      }
    });
    return column;
  }

  static @NonNull String hpText(@NonNull PartyMember member) {
    String rads = member.rads() > 0 ? " (max " + member.effectiveMaxHp() + ")" : "";
    return member.currentHp() + "/" + member.maxHp() + rads;
  }
}
