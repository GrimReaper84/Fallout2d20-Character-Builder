module de.fallout2d20.fallout2d20characterbuilder {
  requires javafx.controls;
  requires javafx.fxml;

  requires org.controlsfx.controls;

  opens de.fallout2d20.fallout2d20characterbuilder to javafx.fxml;
  exports de.fallout2d20.fallout2d20characterbuilder;
}