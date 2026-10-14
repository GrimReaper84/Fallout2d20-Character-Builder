package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.persistence.legacy.ImportReport;
import de.fallout2d20.charbuilder.persistence.legacy.ImportWarning;
import java.nio.file.Path;
import java.util.stream.Collectors;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Window;

/** Shows the result of a legacy import. */
public final class ImportReportDialog {

  private ImportReportDialog() {}

  public static void show(Window owner, Path source, ImportReport report) {
    Alert alert = new Alert(report.hasWarnings() ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
    alert.initOwner(owner);
    alert.setTitle("Legacy import");
    alert.setHeaderText("Imported " + source.getFileName() + " from legacy format v" + report.sourceSchemaVersion());
    alert.setContentText("The character was converted. Saving asks for a new file; the legacy file "
        + "is not changed." + (report.warnings().isEmpty() ? "" : " Details:"));
    if (!report.warnings().isEmpty()) {
      TextArea details = new TextArea(report.warnings().stream()
          .map(ImportReportDialog::line)
          .collect(Collectors.joining("\n")));
      details.setEditable(false);
      details.setWrapText(true);
      details.setPrefRowCount(10);
      alert.getDialogPane().setExpandableContent(details);
      alert.getDialogPane().setExpanded(true);
    }
    alert.showAndWait();
  }

  private static String line(ImportWarning warning) {
    return warning.severity() + " – " + warning.field() + ": " + warning.message();
  }
}
