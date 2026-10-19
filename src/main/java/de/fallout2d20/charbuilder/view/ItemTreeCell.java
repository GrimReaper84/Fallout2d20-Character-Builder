package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.equipment.StartingSource;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

/** A cell of the item tree: groups in bold; items of the starting equipment with a box icon and its source as tooltip. */
final class ItemTreeCell extends TreeCell<ItemTreeNode> {

  @Override
  protected void updateItem(@Nullable ItemTreeNode node, boolean empty) {
    super.updateItem(node, empty);
    setGraphic(null);
    setTooltip(null);
    if (empty || node == null) {
      setText(null);
      setStyle("");
      return;
    }
    switch (node) {
      case ItemGroup group -> {
        setText(group.displayName());
        setStyle("-fx-font-weight: bold;");
      }
      case ItemEntry entry -> {
        setText(entry.label());
        setStyle("");
        StartingSource source = entry.item().startingSource();
        if (source != null) {
          setGraphic(new FontIcon("fas-box-open"));
          setTooltip(new Tooltip(source.description()));
        }
      }
    }
  }
}
