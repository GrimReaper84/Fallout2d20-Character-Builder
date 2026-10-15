module de.fallout2d20.charbuilder {
  requires javafx.controls;
  requires javafx.fxml;
  requires org.controlsfx.controls;
  requires com.fasterxml.jackson.databind;
  requires com.miglayout.javafx;
  requires org.kordamp.ikonli.javafx;
  requires org.kordamp.ikonli.fontawesome6;
  requires static org.jspecify;

  opens de.fallout2d20.charbuilder.view to javafx.fxml;
  opens de.fallout2d20.charbuilder.persistence to com.fasterxml.jackson.databind;
  opens de.fallout2d20.charbuilder.catalog to com.fasterxml.jackson.databind;
  opens de.fallout2d20.charbuilder.catalog.origin to com.fasterxml.jackson.databind;
  opens de.fallout2d20.charbuilder.catalog.trait to com.fasterxml.jackson.databind;
  opens de.fallout2d20.charbuilder.catalog.perk to com.fasterxml.jackson.databind;
  opens de.fallout2d20.charbuilder.catalog.equipment to com.fasterxml.jackson.databind;
  // Attribute and Skill are read as enum values inside catalog records.
  opens de.fallout2d20.charbuilder.model.character to com.fasterxml.jackson.databind;
  exports de.fallout2d20.charbuilder;
}
