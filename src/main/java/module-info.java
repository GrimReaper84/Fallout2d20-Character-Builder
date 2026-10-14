module de.fallout2d20.charbuilder {
  requires javafx.controls;
  requires javafx.fxml;
  requires org.controlsfx.controls;
  requires com.fasterxml.jackson.databind;
  requires com.miglayout.javafx;
  requires org.kordamp.ikonli.javafx;
  requires org.kordamp.ikonli.fontawesome6;
  requires static org.jspecify;

  opens de.fallout2d20.charbuilder to javafx.fxml;
  exports de.fallout2d20.charbuilder;
}
