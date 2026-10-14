package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.persistence.legacy.LegacyCharacterImporter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.ZoneId;

/**
 * Reads and writes character files.
 *
 * <p>Format detection: {@code schemaVersion} 3 is the current format; a missing
 * {@code schemaVersion} (legacy v1) or 2 is passed to the {@link LegacyCharacterImporter}; any other
 * version is rejected. Saving always writes the current format; the UI never saves over a legacy
 * file.
 */
public class CharacterStore {

  private final ObjectMapper mapper = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private final LegacyCharacterImporter legacyImporter;

  public CharacterStore() {
    this(Clock.system(ZoneId.systemDefault()));
  }

  /** @param clock source of the import date of legacy files */
  public CharacterStore(Clock clock) {
    this.legacyImporter = new LegacyCharacterImporter(clock);
  }

  public CharacterFile load(Path file) throws IOException, SaveFormatException {
    JsonNode root;
    try {
      root = mapper.readTree(Files.readString(file, StandardCharsets.UTF_8));
    } catch (JsonProcessingException e) {
      throw new SaveFormatException("Not a valid JSON file: " + e.getOriginalMessage(), e);
    }
    if (!(root instanceof ObjectNode object)) {
      throw new SaveFormatException("A character file must contain a JSON object");
    }
    JsonNode version = object.get("schemaVersion");
    try {
      if (version != null && version.isInt() && version.asInt() == CharacterMapper.SCHEMA_VERSION) {
        return new CharacterFile(readCurrent(object), null);
      }
      return legacyImporter.importCharacter(object);
    } catch (RuntimeException e) {
      // Malformed content that slipped past the checks (e.g. null in a nested value) must reach the
      // user as an unreadable file, not as a crash.
      throw new SaveFormatException("Invalid character file: " + e, e);
    }
  }

  /** Writes the character in the current format, replacing the target file only after success. */
  public void save(Character character, Path file) throws IOException {
    Path absolute = file.toAbsolutePath();
    Path directory = absolute.getParent();
    Path temp = Files.createTempFile(directory, ".character", ".tmp");
    try {
      Files.writeString(temp, mapper.writeValueAsString(CharacterMapper.toData(character)),
          StandardCharsets.UTF_8);
      Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  private Character readCurrent(ObjectNode object) throws SaveFormatException {
    try {
      return CharacterMapper.toModel(mapper.treeToValue(object, CharacterData.class));
    } catch (JsonProcessingException e) {
      throw new SaveFormatException("Invalid character file: " + e.getOriginalMessage(), e);
    }
  }
}
