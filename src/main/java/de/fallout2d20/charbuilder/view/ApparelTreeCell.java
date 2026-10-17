package de.fallout2d20.charbuilder.view;

import javafx.scene.control.TreeCell;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A cell of the apparel tree: groups in bold; worn pieces bold with a check icon, carried pieces plain; a
 * piece whose arm or leg is still to be chosen in the hint colour.
 */
final class ApparelTreeCell extends TreeCell<ApparelTreeNode> {

  @Override
  protected void updateItem(@Nullable ApparelTreeNode node, boolean empty) {
    super.updateItem(node, empty);
    setGraphic(null);
    if (empty || node == null) {
      setText(null);
      setStyle("");
      return;
    }
    switch (node) {
      case ApparelGroup group -> {
        setText(group.displayName());
        setStyle("-fx-font-weight: bold;");
      }
      case ApparelEntry entry -> {
        boolean worn = !entry.item().carried();
        setText(entry.label());
        if (worn) {
          setGraphic(new FontIcon("fas-check"));
        }
        setStyle((worn ? "-fx-font-weight: bold;" : "") + (entry.needsLocation() ? OverviewParts.HINT_STYLE : ""));
      }
    }
  }
}
