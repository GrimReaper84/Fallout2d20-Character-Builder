package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import org.jspecify.annotations.NonNull;
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

  /**
   * A four-column grid as used by the tabs: label, value, label, value. The columns keep their natural
   * width and the content stays on the left, so values and tables are not pulled apart in a wide window.
   */
  static @NonNull MigPane grid() {
    MigPane pane = new MigPane("insets 16, gap 8 4, wrap 4", "[right][left][right][left]");
    // Never squeezed below its natural width (that cut the row names to "…"); a too narrow window scrolls.
    pane.setMinWidth(Region.USE_PREF_SIZE);
    return pane;
  }

  /** Like {@link #grid()}, but the value columns share the window width (for the flowing perk cards). */
  static @NonNull MigPane fillingGrid() {
    return new MigPane("insets 16, gap 8 4, wrap 4", "[right][grow,fill][right][grow,fill]");
  }

  /**
   * One half of a tab split into two halves of equal width (Stats tab): a single column that fills the
   * half, holding section headings and the sections' own grids.
   */
  static @NonNull MigPane half() {
    return new MigPane("insets 0, gap 0 4, wrap 1", "[grow, fill]");
  }

  /** A bold heading with a separator below it over the whole width of a {@link #half()}. */
  static void sectionHeading(@NonNull MigPane half, @NonNull String text) {
    Label label = new Label(text);
    label.setStyle("-fx-font-weight: bold; -fx-font-size: 1.15em;");
    half.add(label, half.getChildren().isEmpty() ? "" : "gaptop 16");
    half.add(new Separator(), "growx");
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

  /** A notes area {@link #TEXT_WIDTH} wide by preference; added with "span 3", it may get narrower. */
  static @NonNull TextArea notesArea(@NonNull TextArea area) {
    area.setPrefWidth(TEXT_WIDTH);
    area.setMinWidth(0);
    return area;
  }

  static void heading(@NonNull MigPane pane, @NonNull String text) {
    Label label = new Label(text);
    label.setStyle("-fx-font-weight: bold; -fx-font-size: 1.15em;");
    pane.add(label, "span 4, gaptop 12, left");
  }

  /** Rule hints at the top of a tab; soft, they never block editing or saving. Nothing if there are none. */
  static void addHints(@NonNull MigPane pane, @NonNull List<CreationProblem> problems) {
    if (problems.isEmpty()) {
      return;
    }
    pane.add(new Label("Rule hints"), "top");
    pane.add(wrapping(hintLabel(problems)), "span 3");
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
