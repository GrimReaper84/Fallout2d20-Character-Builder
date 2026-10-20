package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tbee.javafx.scene.layout.MigPane;

/** Small building blocks shared by the header bar and the tabs of the character view. */
final class OverviewParts {

  static final @NonNull String HINT_STYLE = "-fx-text-fill: #a35200;";
  private static final @NonNull String LEGACY_BASELINE_TOOLTIP =
      "Imported from a legacy file; used as the base value for further advancement.";
  private static final @NonNull String IMPORTED_TOOLTIP =
      "Imported from a legacy file; its effect is already included in the imported values.";
  private static final @NonNull String UNRESOLVED_TOOLTIP =
      "Not yet checked against the rulebooks (shown with its legacy name).";

  private OverviewParts() {}

  /** Width at which long texts (hints, starting equipment, notes) wrap, so they do not stretch the grid. */
  static final double TEXT_WIDTH = 640;

  /** A bold heading with a separator below it over the whole width of a {@link Layouts#column()}. */
  static void sectionHeading(@NonNull MigPane half, @NonNull String text) {
    Label label = new Label(text);
    label.setStyle("-fx-font-weight: bold; -fx-font-size: 1.15em;");
    half.add(label, half.getChildren().isEmpty() ? Layouts.cell() : Layouts.sectionStart());
    half.add(new Separator(), Layouts.cell().growX());
  }

  /** Preferred width of a label that wraps at its cell; small, so the text never decides the column width. */
  private static final double WRAPPING_PREF_WIDTH = 200;

  /**
   * Makes a long label wrap at the width of its cell (added with {@code growx}). Its small preferred width
   * keeps the text from widening the column; the cell then gives it the width the layout has.
   */
  static @NonNull Label wrappingInCell(@NonNull Label label) {
    label.setWrapText(true);
    label.setMinWidth(0);
    label.setPrefWidth(WRAPPING_PREF_WIDTH);
    label.setMaxWidth(Double.MAX_VALUE);
    return label;
  }

  /** Makes a long label wrap at {@link #TEXT_WIDTH} instead of widening the grid to its whole line. */
  static @NonNull Label wrapping(@NonNull Label label) {
    label.setWrapText(true);
    label.setPrefWidth(TEXT_WIDTH);
    label.setMaxWidth(TEXT_WIDTH);
    return label;
  }

  /** Rows of the notes field in a details panel. */
  private static final int NOTES_ROWS = 4;

  /** A name/value row of a details panel (a two-column grid). */
  static void detailRow(@NonNull MigPane rows, @NonNull String name, @NonNull Node value) {
    rows.add(rowName(name));
    rows.add(value);
  }

  /** The note row of a details panel; every typed change goes to {@code onText}. */
  static void notesRow(@NonNull MigPane rows, @NonNull String text, @NonNull Consumer<String> onText) {
    TextArea notes = new TextArea(text);
    notes.setPromptText("Notes, e.g. mods");
    notes.setWrapText(true);
    notes.setPrefRowCount(NOTES_ROWS);
    notes.textProperty().addListener((_, _, typed) -> onText.accept(typed));
    rows.add(rowName("Notes"), Layouts.top());
    rows.add(notes);
  }

  /** A row name that never shrinks below its text (it was cut to "…" while the values took the width). */
  static @NonNull Label rowName(@NonNull String name) {
    Label label = new Label(name);
    label.setMinWidth(Region.USE_PREF_SIZE);
    return label;
  }

  /** The value as text, or a dash for none or blank. */
  static @NonNull String orDash(@Nullable Object value) {
    return value == null || value.toString().isBlank() ? "–" : value.toString();
  }

  /** Rule hints at the top of a tab; soft, they never block editing or saving. Nothing if there are none. */
  static void addHints(@NonNull MigPane pane, @NonNull List<CreationProblem> problems) {
    if (problems.isEmpty()) {
      return;
    }
    pane.add(new Label("Rule hints"), Layouts.top());
    pane.add(wrapping(hintLabel(problems)), Layouts.span(3));
  }

  /** The hints one per line in the hint style; the caller decides how the label wraps. */
  static @NonNull Label hintLabel(@NonNull List<CreationProblem> problems) {
    Label hints = new Label(problems.stream().map(CreationProblem::message).collect(Collectors.joining("\n")));
    hints.setStyle(HINT_STYLE);
    return hints;
  }

  /** A catalog reference that could not be resolved, with a hint for unresolved legacy names. */
  static @NonNull Label reference(@NonNull CatalogRef ref) {
    Label label = new Label(ref.displayName());
    if (ref.legacyName() != null) {
      label.setTooltip(new Tooltip(UNRESOLVED_TOOLTIP));
    }
    return label;
  }

  /** A value taken from a legacy baseline, marked as such. */
  static @NonNull Label baselineMarked(@NonNull String text, boolean legacy) {
    Label label = new Label(legacy ? text + "  (legacy)" : text);
    if (legacy) {
      label.setTooltip(new Tooltip(LEGACY_BASELINE_TOOLTIP));
    }
    return label;
  }

  /** Marks a trait or perk (label or checkbox) imported from a legacy file. */
  static <T extends Labeled> @NonNull T importMarked(@NonNull T label, boolean imported) {
    if (imported) {
      label.setText(label.getText() + "  (imported)");
      label.setTooltip(new Tooltip(IMPORTED_TOOLTIP));
    }
    return label;
  }
}
