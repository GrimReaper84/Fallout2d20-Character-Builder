package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.AppInfo;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Help → About: name, version, license, repository and the fan-project disclaimer. */
public final class AboutDialog {

  private static final @NonNull String DISCLAIMER = "Unofficial fan project, not affiliated with or endorsed by "
      + "Bethesda Softworks, ZeniMax Media or Modiphius Entertainment. Fallout is a trademark of Bethesda "
      + "Softworks LLC. Fallout: The Roleplaying Game is published by Modiphius Entertainment. This program "
      + "contains no rules text; you need the rulebooks to play.";

  private AboutDialog() {}

  public static void show(@Nullable Window owner) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.initOwner(owner);
    alert.setTitle("About");
    alert.setHeaderText(AppInfo.NAME + " " + AppInfo.version());
    // A text area, so the repository link can be selected and copied.
    TextArea text = new TextArea(AppInfo.LICENSE + "\n" + AppInfo.REPOSITORY + "\n\n" + DISCLAIMER
        + "\n\nBuilt with AI assistance (Claude Opus 5.5 via Claude Code).");
    text.setEditable(false);
    text.setWrapText(true);
    text.setPrefRowCount(9);
    alert.getDialogPane().setContent(text);
    alert.showAndWait();
  }
}
