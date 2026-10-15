package de.fallout2d20.charbuilder.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CharacterStoreTest {

  private static final ObjectMapper JSON = new ObjectMapper();
  private final CharacterStore store =
      new CharacterStore(Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneOffset.UTC));

  @TempDir Path dir;

  @Test
  void roundTripsANewCharacter() throws Exception {
    Character character = Character.createNew();
    character.setName("Nora");
    Path file = dir.resolve("nora.json");
    store.save(character, file);

    CharacterFile loaded = store.load(file);
    assertNull(loaded.importReport());
    assertEquals("Nora", loaded.character().getName());
    assertEquals(5, loaded.character().attributeValue(Attribute.LUCK));
    JsonNode written = JSON.readTree(file.toFile());
    assertEquals(3, written.get("schemaVersion").asInt());
    assertEquals(7, written.get("attributeAllocation").size());
    assertEquals(17, written.get("skills").size());
    assertFalse(written.has("legacyBaseline"));
    assertFalse(written.has("legacyImport"));
  }

  @Test
  void importsALegacyFileAndRoundTripsIt() throws Exception {
    Path legacyFile = copyFixture("test-survivor-v2.json");
    CharacterFile imported = store.load(legacyFile);
    assertNotNull(imported.importReport());

    Path v3 = dir.resolve("survivor.json");
    store.save(imported.character(), v3);
    CharacterFile reloaded = store.load(v3);
    Character before = imported.character();
    Character after = reloaded.character();

    assertNull(reloaded.importReport());
    assertTrue(after.isLegacyImport());
    for (Attribute attribute : Attribute.values()) {
      assertEquals(before.attributeValue(attribute), after.attributeValue(attribute));
    }
    for (Skill skill : Skill.values()) {
      assertEquals(before.skillRank(skill), after.skillRank(skill));
      assertEquals(before.skillState(skill), after.skillState(skill));
    }
    assertEquals(before.getPerks(), after.getPerks());
    assertEquals(before.getTraits(), after.getTraits());
    assertEquals(before.getVitals(), after.getVitals());
    assertEquals(before.getOrigin(), after.getOrigin());
    assertEquals(before.getLegacyImport().payload(), after.getLegacyImport().payload());
    assertEquals("2026-10-15", after.getLegacyImport().importedOn());
  }

  @Test
  void rejectsMissingRequiredParts() throws Exception {
    Path file = dir.resolve("broken.json");
    store.save(Character.createNew(), file);
    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());
    node.remove("skills");
    Files.writeString(file, node.toString());
    assertThrows(SaveFormatException.class, () -> store.load(file));
  }

  @Test
  void rejectsMalformedNestedValuesWithoutCrashing() throws Exception {
    Path legacyFile = copyFixture("test-survivor-v2.json");
    Path file = dir.resolve("nested.json");
    store.save(store.load(legacyFile).character(), file);
    ObjectNode valid = (ObjectNode) JSON.readTree(file.toFile());

    ObjectNode perkWithoutRef = valid.deepCopy();
    ((ObjectNode) perkWithoutRef.get("perks").get(0)).remove("ref");
    assertUnreadable(file, perkWithoutRef);

    ObjectNode nullSkill = valid.deepCopy();
    ((ObjectNode) nullSkill.get("skills")).putNull("athletics");
    assertUnreadable(file, nullSkill);

    ObjectNode baselineWithoutSkill = valid.deepCopy();
    ((ObjectNode) baselineWithoutSkill.get("legacyBaseline").get("skills")).remove("sneak");
    assertUnreadable(file, baselineWithoutSkill);
  }

  private void assertUnreadable(Path file, ObjectNode content) throws IOException {
    Files.writeString(file, content.toString());
    assertThrows(SaveFormatException.class, () -> store.load(file));
  }

  /** CRB is always enabled, so a file listing only WOA loads (and saves) as CRB + WOA. */
  @Test
  void roundTripsEnabledBooks() throws Exception {
    Path file = dir.resolve("books.json");
    store.save(Character.createNew(), file);
    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());
    assertEquals("[\"CRB\",\"WOA\",\"WG\"]", node.get("books").toString());

    node.putArray("books").add("WOA");
    Files.writeString(file, node.toString());
    Character loaded = store.load(file).character();
    assertEquals(EnumSet.of(Book.CRB, Book.WOA), loaded.getEnabledBooks());
    store.save(loaded, file);
    assertEquals("[\"CRB\",\"WOA\"]", JSON.readTree(file.toFile()).get("books").toString());
  }

  /** Files written before the books field existed enable all books. */
  @Test
  void enablesAllBooksWhenTheFieldIsMissing() throws Exception {
    Path file = dir.resolve("old.json");
    store.save(Character.createNew(), file);
    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());
    node.remove("books");
    Files.writeString(file, node.toString());
    assertEquals(EnumSet.allOf(Book.class), store.load(file).character().getEnabledBooks());
  }

  @Test
  void rejectsUnknownBooks() throws Exception {
    Path file = dir.resolve("unknown-book.json");
    store.save(Character.createNew(), file);
    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());
    node.putArray("books").add("SG");
    assertUnreadable(file, node);
  }

  @Test
  void ignoresUnknownKeys() throws Exception {
    Path file = dir.resolve("extra.json");
    store.save(Character.createNew(), file);
    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());
    node.put("somethingNew", true);
    Files.writeString(file, node.toString());
    assertNull(store.load(file).importReport());
  }

  @Test
  void rejectsUnsupportedVersionsAndNonObjects() throws Exception {
    Path future = dir.resolve("future.json");
    Files.writeString(future, "{\"schemaVersion\": 4, \"origin\": \"Survivor\", \"attributes\": {}}");
    assertThrows(SaveFormatException.class, () -> store.load(future));
    Path array = dir.resolve("array.json");
    Files.writeString(array, "[]");
    assertThrows(SaveFormatException.class, () -> store.load(array));
    Path garbage = dir.resolve("garbage.json");
    Files.writeString(garbage, "not json");
    assertThrows(SaveFormatException.class, () -> store.load(garbage));
  }

  private Path copyFixture(String name) throws IOException {
    Path target = dir.resolve(name);
    try (InputStream in = getClass().getResourceAsStream("/legacy/" + name)) {
      assertNotNull(in, name);
      Files.copy(in, target);
    }
    return target;
  }
}
