package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.model.character.HouseRule;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.view.Layouts;
import java.util.EnumMap;
import java.util.Map;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Character ▸ Rules: the books the character may use, each with a short summary (the Core Rulebook always), and the
 * house rules it is built with, each with a description to fold out (user decisions 2026-11-02). OK applies the
 * choices to the character, Cancel drops them.
 */
public final class RulesDialog {

  private static final double TEXT_WIDTH = 480;
  private static final @NonNull String BOLD = "-fx-font-weight: bold;";
  private static final @NonNull String UNFOLDED = "fas-chevron-down";
  private static final @NonNull String FOLDED = "fas-chevron-right";

  private RulesDialog() {}

  /** Shows the dialog; returns whether OK changed anything. */
  public static boolean show(@Nullable Window owner, @NonNull PlayerCharacter character) {
    MigPane content = Layouts.column();
    Map<Book, CheckBox> books = new EnumMap<>(Book.class);
    content.add(sectionTitle("Rulebooks"), Layouts.filling());
    for (Book book : Book.values()) {
      CheckBox box = new CheckBox(book.title());
      box.setStyle(BOLD);
      box.setSelected(character.getEnabledBooks().contains(book));
      box.setDisable(book == Book.CRB);
      books.put(book, box);
      content.add(box, Layouts.spaced());
      content.add(text(book.summary()), Layouts.indented(1));
    }
    Map<HouseRule, CheckBox> rules = new EnumMap<>(HouseRule.class);
    content.add(sectionTitle("Custom-/Houserules"), Layouts.sectionStart(Layouts.filling()));
    for (HouseRule rule : HouseRule.values()) {
      CheckBox box = new CheckBox(rule.displayName());
      box.setSelected(character.isOn(rule));
      rules.put(rule, box);
      Label description = text(rule.description());
      description.setVisible(false);
      description.setManaged(false);
      MigPane row = Layouts.row();
      row.add(box);
      row.add(foldButton(description));
      content.add(row, Layouts.spaced());
      content.add(description, Layouts.indented(1));
    }

    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle("Rules");
    dialog.setHeaderText("Rulebooks and house rules of " + (character.getName().isBlank() ? "this character"
        : character.getName()));
    dialog.getDialogPane().setContent(content);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    if (dialog.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
      return false;
    }
    boolean changed = false;
    for (Map.Entry<Book, CheckBox> entry : books.entrySet()) {
      boolean on = entry.getValue().isSelected();
      if (on != character.getEnabledBooks().contains(entry.getKey())) {
        character.setBookEnabled(entry.getKey(), on);
        changed = true;
      }
    }
    for (Map.Entry<HouseRule, CheckBox> entry : rules.entrySet()) {
      boolean on = entry.getValue().isSelected();
      if (on != character.isOn(entry.getKey())) {
        character.setHouseRule(entry.getKey(), on);
        changed = true;
      }
    }
    return changed;
  }

  /** A bold title followed by a line over the rest of the width. */
  private static @NonNull MigPane sectionTitle(@NonNull String title) {
    Label label = new Label(title);
    label.setStyle(BOLD);
    MigPane row = Layouts.row();
    row.add(label);
    row.add(new Separator(), Layouts.filling());
    return row;
  }

  private static @NonNull Label text(@NonNull String value) {
    Label label = new Label(value);
    label.setWrapText(true);
    label.setPrefWidth(TEXT_WIDTH);
    label.setMaxWidth(TEXT_WIDTH);
    return label;
  }

  /** A small button that shows or hides the description below the rule. */
  private static @NonNull Button foldButton(@NonNull Label description) {
    FontIcon icon = new FontIcon(FOLDED);
    Button button = new Button(null, icon);
    button.setTooltip(new Tooltip("Show or hide the description"));
    button.setFocusTraversable(false);
    button.setOnAction(_ -> {
      boolean show = !description.isVisible();
      description.setVisible(show);
      description.setManaged(show);
      icon.setIconLiteral(show ? UNFOLDED : FOLDED);
      if (description.getScene() != null && description.getScene().getWindow() != null) {
        description.getScene().getWindow().sizeToScene();
      }
    });
    return button;
  }
}
