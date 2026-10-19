module de.fallout2d20.charbuilder {
  requires javafx.controls;
  requires javafx.fxml;
  requires java.prefs;
  requires org.controlsfx.controls;
  requires com.fasterxml.jackson.databind;
  requires com.miglayout.javafx;
  // Typed layout constraints (LC, AC, CC) for the standard panes in view.Layouts.
  requires com.miglayout.core;
  requires org.kordamp.ikonli.javafx;
  requires org.kordamp.ikonli.fontawesome6;
  requires org.apache.pdfbox;
  // PDFBox is an automatic module and declares no requires; commons-logging is a named module, so it is
  // only resolved on the module path (./gradlew run, IDE) when required here. The jlink image hid this.
  requires org.apache.commons.logging;
  requires static org.jspecify;

  opens de.fallout2d20.charbuilder.view to javafx.fxml;
  opens de.fallout2d20.charbuilder.persistence to com.fasterxml.jackson.databind;
  opens de.fallout2d20.charbuilder.catalog to com.fasterxml.jackson.databind;
  opens de.fallout2d20.charbuilder.catalog.origin to com.fasterxml.jackson.databind;
  opens de.fallout2d20.charbuilder.catalog.trait to com.fasterxml.jackson.databind;
  opens de.fallout2d20.charbuilder.catalog.perk to com.fasterxml.jackson.databind;
  opens de.fallout2d20.charbuilder.catalog.equipment to com.fasterxml.jackson.databind;
  opens de.fallout2d20.charbuilder.catalog.weapon to com.fasterxml.jackson.databind;
  opens de.fallout2d20.charbuilder.catalog.apparel to com.fasterxml.jackson.databind;
  opens de.fallout2d20.charbuilder.catalog.item to com.fasterxml.jackson.databind;
  // HitLocation is read as enum value in save files.
  opens de.fallout2d20.charbuilder.model.character.apparel to com.fasterxml.jackson.databind;
  // Attribute and Skill are read as enum values inside catalog records.
  opens de.fallout2d20.charbuilder.model.character to com.fasterxml.jackson.databind;
  exports de.fallout2d20.charbuilder;
}
