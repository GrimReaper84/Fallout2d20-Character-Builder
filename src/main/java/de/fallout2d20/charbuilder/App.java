package de.fallout2d20.charbuilder;

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
    stage.setTitle("Fallout 2d20 Character Builder");
    stage.setScene(new Scene(loader.load(), 1024, 768));
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
