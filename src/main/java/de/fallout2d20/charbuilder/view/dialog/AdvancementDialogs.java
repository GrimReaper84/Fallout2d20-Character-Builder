package de.fallout2d20.charbuilder.view.dialog;

import javafx.stage.Window;
import org.jspecify.annotations.Nullable;

/**
 * Confirmations of the one-way advancement steps (user decisions): gaining levels, guarding against
 * typos in the XP amount as long as there is no GM free edit, and applying the staged choices.
 */
public final class AdvancementDialogs {

  private AdvancementDialogs() {}

  /** Asks before gaining levels; names what they grant (CRB p. 49). */
  public static boolean confirmLevelUp(@Nullable Window owner, int levels) {
    String plural = levels == 1 ? "" : "s";
    return ConfirmDialog.confirm(owner, "Level up", "Gain " + levels + " level" + plural + "?",
        "Each level grants one skill rank, one perk and +1 maximum HP: " + levels + " skill rank" + plural
            + ", " + levels + " perk" + plural + ", +" + levels + " HP. Levels cannot be taken back.",
        "Level up");
  }

  /** Asks before applying the staged skill ranks and perks. */
  public static boolean confirmApply(@Nullable Window owner) {
    return ConfirmDialog.confirm(owner, "Apply advancement", "Apply the staged choices?",
        "Applied skill ranks and perks can no longer be taken back.", "Apply");
  }
}
