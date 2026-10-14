package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.databind.node.ObjectNode;

/** Save-file form of the legacy import metadata and the untouched legacy payload. */
public record LegacyImportData(int sourceSchemaVersion, String importedOn, ObjectNode payload) {}
