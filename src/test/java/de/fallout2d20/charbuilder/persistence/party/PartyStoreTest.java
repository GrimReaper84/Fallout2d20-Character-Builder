package de.fallout2d20.charbuilder.persistence.party;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.model.party.Party;
import de.fallout2d20.charbuilder.model.party.PartyMember;
import de.fallout2d20.charbuilder.persistence.SaveFormatException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PartyStoreTest {

  @TempDir Path dir;

  private final PartyStore store = new PartyStore("1.5.0");

  @Test
  void writesAndReadsTheParty() throws Exception {
    Party party = new Party("Commonwealth", List.of(new PartyMember("Nora", "Anna", 3, 9, 12, 1, 13, "Left arm"),
        new PartyMember("Codsworth", "", 2, 10, 10, 0, 9, "")), "1.4.0");
    Path file = dir.resolve("party.json");
    store.save(party, file);

    String json = Files.readString(file, StandardCharsets.UTF_8);
    assertTrue(json.contains("\"format\" : \"fallout2d20-party\""), json);
    assertTrue(json.contains("\"savedWith\" : \"1.5.0\""), json);
    Party loaded = store.load(file);
    assertEquals("Commonwealth", loaded.getName());
    assertEquals(party.members(), loaded.members());
    assertEquals("1.4.0", loaded.getCreatedWith());
  }

  @Test
  void ignoresUnknownFieldsAndReadsMissingValuesAsEmpty() throws Exception {
    Path file = write("""
        {"format": "fallout2d20-party", "formatVersion": 1, "future": true,
         "members": [{"name": "Nora", "maxHp": 12, "colour": "red"}, null]}""");
    Party loaded = store.load(file);
    assertEquals("", loaded.getName());
    assertEquals(List.of(new PartyMember("Nora", "", 0, 0, 12, 0, 0, "")), loaded.members());
  }

  @Test
  void rejectsOtherFilesAndNewerVersions() throws Exception {
    Path character = write("{\"format\": \"fallout2d20-character-builder\", \"formatVersion\": 1}");
    assertThrows(SaveFormatException.class, () -> store.load(character));
    Path newer = write("{\"format\": \"fallout2d20-party\", \"formatVersion\": 2, \"savedWith\": \"9.0\"}");
    SaveFormatException error = assertThrows(SaveFormatException.class, () -> store.load(newer));
    assertTrue(error.getMessage().contains("9.0"), error.getMessage());
    Path broken = write("not json");
    assertThrows(SaveFormatException.class, () -> store.load(broken));
    Path noVersion = write("{\"format\": \"fallout2d20-party\"}");
    assertThrows(SaveFormatException.class, () -> store.load(noVersion));
  }

  private Path write(String json) throws Exception {
    Path file = Files.createTempFile(dir, "party", ".json");
    Files.writeString(file, json, StandardCharsets.UTF_8);
    return file;
  }
}
