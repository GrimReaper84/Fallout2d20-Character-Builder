package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.ExperienceTable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Level and XP of a complete character in GM mode (D2): both set freely, also downwards. A new level sets
 * the XP to that level's minimum (CRB p. 49 table); the XP field can then be corrected.
 */
final class GmLevelSection {

  private GmLevelSection() {}

  /** Adds the level and XP rows to a four-column overview grid. */
  static void addTo(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onChanged) {
    TextField level = NumberField.of(character.getLevel(), 1, value -> {
      long xp;
      try {
        xp = ExperienceTable.xpForLevel(value);
      } catch (ArithmeticException e) {
        xp = character.getXp();
      }
      character.setLevelAsGm(value, xp);
      onChanged.run();
    });
    level.setTooltip(new Tooltip("Sets the level directly; XP becomes the minimum of that level."));
    pane.add(new Label("Level"));
    pane.add(level);
    TextField xp = NumberField.of((int) Math.min(Integer.MAX_VALUE, character.getXp()), 0, value -> {
      character.setLevelAsGm(character.getLevel(), value);
      onChanged.run();
    });
    xp.setTooltip(new Tooltip("Sets the XP directly; the level stays."));
    pane.add(new Label("XP"));
    pane.add(xp);
  }
}
