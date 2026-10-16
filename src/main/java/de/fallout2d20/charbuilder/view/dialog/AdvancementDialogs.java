package de.fallout2d20.charbuilder.view.dialog;

import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Confirmations of the one-way advancement steps (user decisions): gaining levels, guarding against
 * typos in the XP amount as long as there is no GM free edit, and applying the staged choices. Cancel
 * is the default button, so pressing Enter changes nothing by accident.
 */
public final class AdvancementDialogs {

  private AdvancementDialogs() {}

  /** Asks before gaining levels; names what they grant (CRB p. 49). */
  public static boolean confirmLevelUp(@Nullable Window owner, int levels) {
    String plural = levels == 1 ? "" : "s";
    return confirm(owner, "Level up", "Gain " + levels + " level" + plural + "?",
        "Each level grants one skill rank, one perk and +1 maximum HP: " + levels + " skill rank" + plural
            + ", " + levels + " perk" + plural + ", +" + levels + " HP. Levels cannot be taken back.",
        "Level up");
  }

  /** Asks before applying the staged skill ranks and perks. */
  public static boolean confirmApply(@Nullable Window owner) {
    return confirm(owner, "Apply advancement", "Apply the staged choices?",
        "Applied skill ranks and perks can no longer be taken back.", "Apply");
  }

  private static boolean confirm(@Nullable Window owner, @NonNull String title, @NonNull String header,
      @NonNull String text, @NonNull String action) {
    ButtonType ok = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ok, ButtonType.CANCEL);
    alert.initOwner(owner);
    alert.setTitle(title);
    alert.setHeaderText(header);
    ((Button) alert.getDialogPane().lookupButton(ok)).setDefaultButton(false);
    ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);
    Optional<ButtonType> answer = alert.showAndWait();
    return answer.isPresent() && answer.get() == ok;
  }
}
