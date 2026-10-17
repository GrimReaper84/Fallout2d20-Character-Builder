package de.fallout2d20.charbuilder.view;

import javafx.scene.control.TreeCell;
import org.jspecify.annotations.Nullable;

/** A cell of the weapon tree: groups in bold, weapons by their label, a weapon still needing a skill in the hint colour. */
final class WeaponTreeCell extends TreeCell<WeaponTreeNode> {

  @Override
  protected void updateItem(@Nullable WeaponTreeNode node, boolean empty) {
    super.updateItem(node, empty);
    if (empty || node == null) {
      setText(null);
      setStyle("");
      return;
    }
    switch (node) {
      case RangeGroup group -> {
        setText(group.displayName());
        setStyle("-fx-font-weight: bold;");
      }
      case WeaponEntry entry -> {
        setText(entry.label());
        setStyle(entry.needsSkill() ? OverviewParts.HINT_STYLE : "");
      }
    }
  }
}
