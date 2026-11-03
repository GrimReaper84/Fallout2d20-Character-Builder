package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.view.Layouts;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The detail panel of a compendium entry (H2): its name, one row per detail line, and links that open the referenced
 * entry. A target the book filter hides is dimmed text instead of a link.
 */
final class DetailPanel {

  private static final double TEXT_WIDTH = 420;
  private static final double HIDDEN_OPACITY = 0.6;

  private DetailPanel() {}

  static @NonNull Node build(@NonNull CompendiumEntry entry, @NonNull Predicate<Reference> available,
      @NonNull Consumer<Reference> open) {
    MigPane pane = Layouts.details(0);
    Label title = new Label(entry.name());
    title.setStyle("-fx-font-size: 1.2em; -fx-font-weight: bold;");
    pane.add(title, Layouts.span(2));
    for (DetailLine line : entry.details()) {
      Label label = new Label(line.label());
      // The heading column is as wide as its widest heading; only the values wrap.
      label.setMinWidth(Region.USE_PREF_SIZE);
      pane.add(label, Layouts.top());
      pane.add(switch (line) {
        case DetailValue value -> text(value.text());
        case DetailLinks links -> links(links, available, open);
      });
    }
    return pane;
  }

  /** An empty panel while nothing is selected. */
  static @NonNull Node placeholder() {
    MigPane pane = Layouts.details(0);
    pane.add(new Label("Select an entry to see its details."), Layouts.span(2));
    return pane;
  }

  private static @NonNull Label text(@NonNull String text) {
    Label label = new Label(text);
    label.setWrapText(true);
    label.setMaxWidth(TEXT_WIDTH);
    return label;
  }

  private static @NonNull FlowPane links(@NonNull DetailLinks links, @NonNull Predicate<Reference> available,
      @NonNull Consumer<Reference> open) {
    FlowPane flow = new FlowPane();
    flow.setPrefWrapLength(TEXT_WIDTH);
    for (Reference reference : links.references()) {
      if (available.test(reference)) {
        Hyperlink link = new Hyperlink(reference.name());
        link.setPadding(Insets.EMPTY);
        link.setOnAction(_ -> open.accept(reference));
        link.setTooltip(new Tooltip("Open in " + reference.section().title()));
        flow.getChildren().add(link);
      } else {
        // A disabled control shows no tooltip, so a hidden target is plain, dimmed text.
        Label hidden = new Label(reference.name());
        hidden.setOpacity(HIDDEN_OPACITY);
        hidden.setTooltip(new Tooltip("Not in the enabled books"));
        flow.getChildren().add(hidden);
      }
      flow.getChildren().add(new Label("  "));
    }
    flow.getChildren().removeLast();
    return flow;
  }
}
