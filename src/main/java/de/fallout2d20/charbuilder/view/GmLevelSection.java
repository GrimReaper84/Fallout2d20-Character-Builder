package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.ExperienceTable;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.NonNull;

/**
 * Level and XP of a complete character in GM mode (D2): both set freely, also downwards. A new level sets
 * the XP to that level's minimum (CRB p. 49 table); the XP field can then be corrected.
 */
final class GmLevelSection {

  private GmLevelSection() {}

  /** The level field; a new level sets the XP to its minimum. */
  static @NonNull TextField levelField(@NonNull PlayerCharacter character, @NonNull Runnable onChanged) {
    TextField level = NumberField.of(character.advancement().getLevel(), 1, value -> {
      long xp;
      try {
        xp = ExperienceTable.xpForLevel(value);
      } catch (ArithmeticException e) {
        xp = character.advancement().getXp();
      }
      character.advancement().setLevelAsGm(value, xp);
      onChanged.run();
    });
    level.setTooltip(new Tooltip("Sets the level directly; XP becomes the minimum of that level."));
    return level;
  }

  /** The XP field; the level stays. */
  static @NonNull TextField xpField(@NonNull PlayerCharacter character, @NonNull Runnable onChanged) {
    TextField xp = NumberField.of((int) Math.min(Integer.MAX_VALUE, character.advancement().getXp()), 0, value -> {
      character.advancement().setLevelAsGm(character.advancement().getLevel(), value);
      onChanged.run();
    });
    xp.setTooltip(new Tooltip("Sets the XP directly; the level stays."));
    return xp;
  }
}
