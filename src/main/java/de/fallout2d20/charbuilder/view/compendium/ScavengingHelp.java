package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.view.Layouts;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * The help of the Scavenging section: a button opening a dialog that leaves the section usable, so the GM can read
 * along while rolling. In own words, with page references.
 */
final class ScavengingHelp {

  private static final double WIDTH = 560;
  private static final double HEIGHT = 520;
  private static final double SPACING = 3;
  private static final double BULLET_GAP = 6;
  /** Each part of the help: the heading, then its bullet points. */
  private static final @NonNull List<List<String>> PARTS = List.of(
      List.of("Overview",
          "Rolls finds on the book's loot tables, for a location or for defeated NPCs.",
          "Nothing is saved or added to a character."),
      List.of("Before the search",
          "Scale: sets the time.",
          "Degree: sets the difficulty.",
          "Level: how far Luck can move a roll.",
          "Successes: the result of the group's PER + Survival test, rolled at the table.",
          "Complications: only those you turn into lost time. Each adds half the time.",
          "Group AP: what the group may spend from its pool (normally at most 6, CRB p. 19).",
          "LCK: the score of the character spending Luck.",
          "Junk: a count (Core Rulebook) or single items (Wanderer's Guide)."),
      List.of("Min and Max",
          "Min: rolls every successful search gets automatically.",
          "Max: the most rolls in that category.",
          "Between Min and Max: each roll costs 1 AP (+ AP button).",
          "Min 0: nothing is rolled unless AP are spent on it.",
          "Max 0: the category is not part of the location.",
          "The values are suggestions (house rule). Set your own or those of your adventure."),
      List.of("Reading the result",
          "Top line: difficulty, AP left, time, Luck spent.",
          "Too few successes: nothing found. Another attempt costs the time again.",
          "3 / 4 beside a category: 3 of at most 4 rolls are made.",
          "Small text below a find: the dice behind it, and notes for the GM.",
          "Link: opens the compendium entry. Back returns with the result intact.",
          "No link: no entry exists (Power Armor pieces, keys, containers).",
          "AP left at the end can go to the group pool."),
      List.of("Changing the result",
          "+ AP: one more roll for 1 AP, up to Max.",
          "Halve time: 2 AP.",
          "Luck on a find: 1 Luck, choose a row within the level (CRB p. 200).",
          "Luck on a junk count: 1 Luck, adds LCK (CRB p. 208).",
          "Rare Book: 1 Luck, replaces a magazine (WG p. 108).",
          "Adjust on a junk item: free, moves it by up to LCK (WG p. 120).",
          "Each find can be changed once.",
          "+ free: outside AP and Max, for Can Do!, Pharma Farma or a GM gift. Marked on the find.",
          "Roll to maximum: fills every category without AP. Marked on the find."),
      List.of("Good to know",
          "Scrounger and Fortune Finder are not computed; add them yourself.",
          "Ranged weapons come with ammunition (CRB p. 204).",
          "Armor from a set is one piece with a rolled location (CRB p. 201).",
          "Ranged weapon rolls of 10 and 19 are repeated: the book has no such rows.",
          "Changing the books resets this section."),
      List.of("NPC (CRB p. 335)",
          "NPCs: how many equal NPCs are looted.",
          "Wealth: d20s of caps per NPC, normally 1 to 5, 0 for none.",
          "Ammunition: the type of their ranged weapon, or none.",
          "Prolific scavengers: 2 CD for junk instead of 1.",
          "Each Effect is one junk item of your choice.",
          "Inventory, unused consumables, butchery and salvage: see the NPC's description."),
      List.of("Copy as text",
          "Puts the top line and the finds on the clipboard, without dice and notes."));

  private @Nullable Dialog<Void> open;

  /** The button; a second click brings the open dialog to the front. */
  @NonNull Button button() {
    Button button = new Button();
    button.setGraphic(new FontIcon("far-question-circle"));
    button.setTooltip(new Tooltip("How to use scavenging and read its results"));
    button.setOnAction(_ -> show(button));
    return button;
  }

  private void show(@NonNull Node owner) {
    if (open != null && open.isShowing()) {
      open.getDialogPane().getScene().getWindow().requestFocus();
      return;
    }
    Dialog<Void> dialog = new Dialog<>();
    dialog.initOwner(owner.getScene().getWindow());
    dialog.initModality(Modality.NONE);
    dialog.setTitle("Scavenging help");
    dialog.setResizable(true);
    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    dialog.getDialogPane().setContent(content());
    dialog.getDialogPane().setPrefSize(WIDTH, HEIGHT);
    open = dialog;
    dialog.show();
  }

  private static @NonNull Node content() {
    VBox text = new VBox(SPACING);
    for (List<String> part : PARTS) {
      Label heading = new Label(part.getFirst());
      heading.setStyle("-fx-font-weight: bold;");
      text.getChildren().add(heading);
      part.stream().skip(1).forEach(point -> text.getChildren().add(bullet(point)));
    }
    ScrollPane scroll = new ScrollPane(text);
    scroll.setFitToWidth(true);
    return scroll;
  }

  private static @NonNull Node bullet(@NonNull String point) {
    Label body = new Label(point);
    body.setWrapText(true);
    HBox line = new HBox(BULLET_GAP, new Label("•"), body);
    line.setPadding(new Insets(0, 0, 0, Layouts.INDENT));
    return line;
  }
}
