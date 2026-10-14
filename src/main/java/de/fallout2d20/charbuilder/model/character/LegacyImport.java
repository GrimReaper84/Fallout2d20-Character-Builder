package de.fallout2d20.charbuilder.model.character;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Origin of an imported character and the legacy data no feature has taken over yet.
 *
 * <p>The payload keeps legacy fields such as the free-text equipment, weapons, hand-typed armor DR
 * and Mister Handy options unchanged; later features move their data out of it.
 *
 * @param sourceSchemaVersion 1 for unversioned files, otherwise the legacy schema version
 * @param importedOn ISO date of the import
 * @param payload legacy fields kept verbatim
 */
public record LegacyImport(int sourceSchemaVersion, String importedOn, ObjectNode payload) {

  public LegacyImport {
    payload = payload.deepCopy();
  }

  @Override
  public ObjectNode payload() {
    return payload.deepCopy();
  }
}
