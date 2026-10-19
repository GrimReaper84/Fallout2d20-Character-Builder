package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fallout2d20.charbuilder.AppInfo;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.persistence.legacy.LegacyCharacterImporter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.ZoneId;
import org.jspecify.annotations.NonNull;

/**
 * Reads and writes character files.
 *
 * <p>Format detection: a file with the {@code format} marker of this app is read if its
 * {@code formatVersion} is known, a newer one is rejected with a hint to update the app; a file of the
 * 1.0.0 test builds ({@code schemaVersion: 3}, no marker) is read the same way; anything else (no
 * {@code schemaVersion} = legacy v1, or 2) is passed to the {@link LegacyCharacterImporter}, which rejects
 * other versions. Saving always writes the current format with the app version; the UI never saves over
 * a legacy file.
 */
public class CharacterStore {

  private final ObjectMapper mapper = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private final LegacyCharacterImporter legacyImporter;
  private final @NonNull String appVersion;

  public CharacterStore() {
    this(Clock.system(ZoneId.systemDefault()), AppInfo.version());
  }

  /**
   * @param clock source of the import date of legacy files
   * @param appVersion the version written as {@code savedWith}
   */
  public CharacterStore(Clock clock, @NonNull String appVersion) {
    this.legacyImporter = new LegacyCharacterImporter(clock);
    this.appVersion = appVersion;
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
    try {
      if (object.has("format")) {
        checkFormat(object);
        return new CharacterFile(readCurrent(object), null);
      }
      JsonNode schemaVersion = object.get("schemaVersion");
      if (schemaVersion != null && schemaVersion.isInt()
          && schemaVersion.asInt() == CharacterMapper.TEST_BUILD_SCHEMA_VERSION) {
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
  public void save(PlayerCharacter character, Path file) throws IOException {
    Path absolute = file.toAbsolutePath();
    Path directory = absolute.getParent();
    Path temp = Files.createTempFile(directory, ".character", ".tmp");
    try {
      Files.writeString(temp, mapper.writeValueAsString(CharacterMapper.toData(character, appVersion)),
          StandardCharsets.UTF_8);
      Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  /** Accepts this app's marker with a known format version; a newer version asks for an app update. */
  private static void checkFormat(@NonNull ObjectNode object) throws SaveFormatException {
    if (!CharacterMapper.FORMAT.equals(object.get("format").asText())) {
      throw new SaveFormatException("Not a character file of " + AppInfo.NAME + ".");
    }
    JsonNode version = object.get("formatVersion");
    if (version == null || !version.isInt() || version.asInt() < 1) {
      throw new SaveFormatException("The character file has no valid format version.");
    }
    if (version.asInt() > CharacterMapper.FORMAT_VERSION) {
      JsonNode savedWith = object.get("savedWith");
      String by = savedWith == null || !savedWith.isTextual() ? "a newer version" : "version " + savedWith.asText();
      throw new SaveFormatException("This character was saved with " + by + " of " + AppInfo.NAME
          + " (file format " + version.asInt() + "). Please update the app to open it.");
    }
  }

  private PlayerCharacter readCurrent(ObjectNode object) throws SaveFormatException {
    try {
      return CharacterMapper.toModel(mapper.treeToValue(object, CharacterData.class));
    } catch (JsonProcessingException e) {
      throw new SaveFormatException("Invalid character file: " + e.getOriginalMessage(), e);
    }
  }
}
