package de.fallout2d20.charbuilder;

import de.fallout2d20.charbuilder.view.MainWindowController;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Entry point of the Fallout 2d20 character builder. */
public class App extends Application {

  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader loader = new FXMLLoader(App.class.getResource("main-window.fxml"));
    stage.setScene(new Scene(loader.load(), 1024, 768));
    MainWindowController controller = loader.getController();
    controller.attach(stage);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
