package de.fallout2d20.charbuilder;

import de.fallout2d20.charbuilder.view.MainWindowController;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.jspecify.annotations.NonNull;

/** Entry point of the Fallout 2d20 character builder. */
public class App extends Application {

  /**
   * Sizes of the window icons (resources {@code icon-<size>.png}) and of the Windows installer icon, drawn by the
   * test task generateIcons.
   */
  public static final @NonNull List<Integer> ICON_SIZES = List.of(16, 24, 32, 48, 64, 128, 256);

  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader loader = new FXMLLoader(App.class.getResource("main-window.fxml"));
    stage.setScene(new Scene(loader.load(), 1024, 768));
    MainWindowController controller = loader.getController();
    controller.attach(stage);
    addIcons(stage);
    stage.show();
  }

  /** The radiation icon in every size, so the window system picks the fitting one; owned dialogs inherit it. */
  private static void addIcons(@NonNull Stage stage) throws IOException {
    for (int size : ICON_SIZES) {
      try (InputStream in = App.class.getResourceAsStream("icon-" + size + ".png")) {
        if (in == null) {
          throw new IOException("Missing window icon icon-" + size + ".png");
        }
        stage.getIcons().add(new Image(in));
      }
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}
