package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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

  /** A four-column grid as used by every tab: label, value, label, value. */
  static @NonNull MigPane grid() {
    return new MigPane("insets 16, gap 8 4, wrap 4", "[right][grow,fill][right][grow,fill]");
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
    pane.add(hintLabel(problems), "span 3");
  }

  static @NonNull Label hintLabel(@NonNull List<CreationProblem> problems) {
    Label hints = new Label(problems.stream().map(CreationProblem::message).collect(Collectors.joining("\n")));
    hints.setStyle(HINT_STYLE);
    hints.setWrapText(true);
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

  /** Marks a trait or perk imported from a legacy file. */
  static @NonNull Label importMarked(@NonNull Label label, boolean imported) {
    if (imported) {
      label.setText(label.getText() + "  (imported)");
      label.setTooltip(new Tooltip(IMPORTED_TOOLTIP));
    }
    return label;
  }
}
