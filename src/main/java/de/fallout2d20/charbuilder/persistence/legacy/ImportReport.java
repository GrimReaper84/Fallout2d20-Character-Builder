package de.fallout2d20.charbuilder.persistence.legacy;

import java.util.List;

/**
 * Result report of a legacy import.
 *
 * @param sourceSchemaVersion 1 for unversioned files, otherwise the legacy schema version
 * @param warnings all findings in the order they were found
 */
public record ImportReport(int sourceSchemaVersion, List<ImportWarning> warnings) {

  public ImportReport {
    warnings = List.copyOf(warnings);
  }

  public boolean hasWarnings() {
    return warnings.stream().anyMatch(w -> w.severity() == WarningSeverity.WARNING);
  }
}
