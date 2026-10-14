package de.fallout2d20.charbuilder.persistence.legacy;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fallout2d20.charbuilder.model.character.LegacyBaseline;
import de.fallout2d20.charbuilder.persistence.CharacterFile;
import de.fallout2d20.charbuilder.persistence.SaveFormatException;
import java.time.Clock;

/**
 * Imports character files of the legacy app (v0.4.5): unversioned files ("v1") and
 * {@code schemaVersion: 2}.
 *
 * <p>Reference behavior is {@code migrateCharacterData} of the legacy app (string perks are merged
 * into ranks, lenient number parsing, defaults for missing parts). Deliberate deviation: nothing is
 * dropped silently; every adaptation is reported, and data no feature handles yet is kept verbatim
 * in the legacy payload.
 *
 * <p>Imported attribute values and skill ranks become the {@link LegacyBaseline}; imported traits
 * and perk ranks are marked, because their effects on attributes and skills are already included.
 */
public class LegacyCharacterImporter {

  private final Clock clock;

  public LegacyCharacterImporter(Clock clock) {
    this.clock = clock;
  }

  /** Imports one legacy file; the result always carries an import report. */
  public CharacterFile importCharacter(ObjectNode root) throws SaveFormatException {
    return new LegacyImportRun(root, clock).execute();
  }
}
