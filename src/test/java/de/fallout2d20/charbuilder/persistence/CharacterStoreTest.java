package de.fallout2d20.charbuilder.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.equipment.ArmAttachment;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.Vitals;
import de.fallout2d20.charbuilder.model.character.equipment.ArmSelection;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
  void roundTripsOriginChoices() throws Exception {
    Character character = Character.createNew();
    Path file = dir.resolve("origin.json");
    store.save(character, file);
    assertFalse(JSON.readTree(file.toFile()).has("originChoices"), "omitted when nothing is chosen");

    character.chooseOrigin(CatalogRef.of("vault-dweller"));
    character.chooseOriginTagSkill(Skill.SNEAK);
    character.setPurpose("Scout");
    store.save(character, file);
    Character loaded = store.load(file).character();
    assertEquals(new OriginChoices(Skill.SNEAK, "Scout"), loaded.getOriginChoices());
    assertEquals("sneak", JSON.readTree(file.toFile()).get("originChoices").get("tagSkill").asText());

    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());
    ((ObjectNode) node.get("originChoices")).put("tagSkill", "hacking");
    assertUnreadable(file, node);
  }

  @Test
  void roundTripsStartingEquipmentAndInventoryNotes() throws Exception {
    Character settler = Character.createNew();
    Path file = dir.resolve("settler.json");
    store.save(settler, file);
    JsonNode empty = JSON.readTree(file.toFile());
    assertFalse(empty.has("startingEquipment"), "omitted when nothing is chosen");
    assertFalse(empty.has("inventoryNotes"), "omitted when blank");

    settler.chooseOrigin(CatalogRef.of("survivor"));
    settler.setFreeTag(Skill.THROWING, true);
    settler.chooseEquipmentPack(CatalogRef.of("raider"));
    settler.chooseEquipmentOption(1, "1 dose of RadAway");
    settler.chooseTagSkillItem(Skill.THROWING, "2 + 1 CD tomahawks");
    settler.setInventoryNotes("Owes 20 caps to the Diamond City bar");
    store.save(settler, file);
    Character loaded = store.load(file).character();
    assertEquals(settler.getStartingEquipment(), loaded.getStartingEquipment());
    assertEquals("Owes 20 caps to the Diamond City bar", loaded.getInventoryNotes());
    JsonNode written = JSON.readTree(file.toFile()).get("startingEquipment");
    assertEquals("[null,\"1 dose of RadAway\",null]", written.get("choices").toString());
    assertEquals("2 + 1 CD tomahawks", written.get("tagSkillItems").get("throwing").asText());
    assertFalse(written.has("arms"));
  }

  /** Free arms (house-rule option) with a weapon text survive saving. */
  @Test
  void roundTripsMisterHandyArms() throws Exception {
    Character handy = Character.createNew();
    handy.chooseOrigin(CatalogRef.of("mister-handy"));
    handy.chooseEquipmentPack(CatalogRef.of("nurse-handy"));
    handy.setFreeArms(true);
    handy.chooseArm(0, ArmSelection.of(ArmAttachment.PINCER));
    handy.chooseArm(2, ArmSelection.ofWeapon("Hunting rifle"));
    Path file = dir.resolve("handy.json");
    store.save(handy, file);
    assertEquals(handy.getStartingEquipment(), store.load(file).character().getStartingEquipment());
    assertEquals("[{\"attachment\":\"PINCER\"},{\"attachment\":\"BUZZ_SAW\"},{\"weapon\":\"Hunting rifle\"}]",
        JSON.readTree(file.toFile()).get("startingEquipment").get("arms").toString());
  }

  /** Picks that no longer fit the catalog (e.g. after a text correction) are opened, not rejected. */
  @Test
  void opensPicksThatNoLongerFitTheCatalog() throws Exception {
    Character handy = Character.createNew();
    handy.chooseOrigin(CatalogRef.of("mister-handy"));
    handy.setFreeTag(Skill.EXPLOSIVES, true);
    handy.chooseEquipmentPack(CatalogRef.of("mister-gutsy"));
    Path file = dir.resolve("stale.json");
    store.save(handy, file);
    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());
    ObjectNode equipment = (ObjectNode) node.get("startingEquipment");
    equipment.putArray("choices").add("Spoon");
    equipment.putObject("tagSkillItems").put("explosives", "3 Molotov cocktails").put("repair", "Multi-Tool");
    ((ArrayNode) equipment.get("arms")).remove(2);
    Files.writeString(file, node.toString());

    StartingEquipment loaded = store.load(file).character().getStartingEquipment();
    assertEquals(List.of(), loaded.choices(), "Mister Gutsy has no choices");
    assertEquals(Map.of(), loaded.tagSkillItems());
    assertEquals(List.of(ArmSelection.of(ArmAttachment.AUTO_PISTOL_10MM), ArmSelection.of(ArmAttachment.BUZZ_SAW),
        ArmSelection.of(ArmAttachment.LASER_EMITTER)), loaded.arms(), "the pack's arms");
  }

  /** A complete character cannot pick again, so stale picks keep their text. */
  @Test
  void completeCharactersKeepStalePicks() throws Exception {
    StartingEquipment settler = new StartingEquipment(CatalogRef.of("settler"), List.of("Spoon"), Map.of(),
        null, false, List.of());
    Character complete = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 1, 0, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(), Progression.NONE, Vitals.FULL,
        null, null, settler, "");
    Path file = dir.resolve("complete.json");
    store.save(complete, file);
    assertEquals(List.of("Spoon"), store.load(file).character().getStartingEquipment().choices());
  }

  @Test
  void rejectsUnknownAttachmentsAndSkills() throws Exception {
    Character handy = Character.createNew();
    handy.chooseOrigin(CatalogRef.of("mister-handy"));
    handy.chooseEquipmentPack(CatalogRef.of("mister-gutsy"));
    Path file = dir.resolve("arms.json");
    store.save(handy, file);
    ObjectNode valid = (ObjectNode) JSON.readTree(file.toFile());

    ObjectNode unknownArm = valid.deepCopy();
    ((ObjectNode) unknownArm.get("startingEquipment").get("arms").get(0)).put("attachment", "CLAW");
    assertUnreadable(file, unknownArm);

    ObjectNode bothOnOneArm = valid.deepCopy();
    ((ObjectNode) bothOnOneArm.get("startingEquipment").get("arms").get(0)).put("weapon", "Hunting rifle");
    assertUnreadable(file, bothOnOneArm);

    ObjectNode unknownSkill = valid.deepCopy();
    ((ObjectNode) unknownSkill.get("startingEquipment")).putObject("tagSkillItems").put("hacking", "Terminal");
    assertUnreadable(file, unknownSkill);
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
