package de.fallout2d20.charbuilder.persistence.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.LegacyImport;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.persistence.CharacterFile;
import de.fallout2d20.charbuilder.persistence.SaveFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegacyCharacterImporterTest {

  private static final ObjectMapper JSON = new ObjectMapper();
  private final LegacyCharacterImporter importer = new LegacyCharacterImporter(
      Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneOffset.UTC));

  @Test
  void importsBaseDataAndAttributesOfTheV2SurvivorFixture() throws Exception {
    Character character = importer.importCharacter(fixture("test-survivor-v2.json")).character();

    assertEquals("Test Survivor", character.getName());
    assertEquals("survivor", character.getOrigin().id());
    assertEquals(2, character.getLevel());
    assertEquals(100, character.getXp());
    assertTrue(character.isCreationComplete());
    Map<Attribute, Integer> expected = Map.of(Attribute.STRENGTH, 4, Attribute.PERCEPTION, 7,
        Attribute.ENDURANCE, 5, Attribute.CHARISMA, 5, Attribute.INTELLIGENCE, 8, Attribute.AGILITY, 6,
        Attribute.LUCK, 5);
    expected.forEach((attribute, value) -> {
      assertEquals(value, character.attributeValue(attribute), attribute.id());
      assertEquals(0, character.attributeAllocation(attribute));
    });
    assertEquals(11, character.getVitals().currentHp());
  }

  @Test
  void importsSkillsPerksAndTraitsOfTheV2SurvivorFixture() throws Exception {
    Character character = importer.importCharacter(fixture("test-survivor-v2.json")).character();

    assertTag(character, Skill.LOCKPICK, 3);
    assertTag(character, Skill.REPAIR, 4);
    assertTag(character, Skill.SCIENCE, 3);
    assertEquals(3, character.skillRank(Skill.SURVIVAL));
    assertFalse(character.skillState(Skill.SURVIVAL).tagged());
    assertEquals(List.of("jury-rigging", "comprehension", "gun-nut"),
        character.getPerks().stream().map(p -> p.perk().id()).toList());
    for (PerkSelection perk : character.getPerks()) {
      assertEquals(1, perk.rank());
      assertEquals(1, perk.importedRanks());
    }
    assertEquals("small-frame", character.getTraits().get(0).trait().id());
    assertTrue(character.getTraits().get(0).imported());
  }

  @Test
  void keepsUnportedFieldsOfTheV2SurvivorFixtureWithoutWarnings() throws Exception {
    CharacterFile result = importer.importCharacter(fixture("test-survivor-v2.json"));

    LegacyImport legacy = result.character().getLegacyImport();
    assertNotNull(legacy);
    assertEquals(2, legacy.sourceSchemaVersion());
    assertEquals("2026-10-15", legacy.importedOn());
    for (String field : List.of("equipment", "equipmentPack", "trinket", "weapons", "armor",
        "originOptions")) {
      assertTrue(legacy.payload().has(field), field);
    }
    assertFalse(legacy.payload().has("derived"), "derived values are recomputed, not kept");
    assertFalse(hasInfo(result, "derived"), "JC's stored derived values match the rules");
    assertEquals("Laser Gun", legacy.payload().get("weapons").get(0).get("name").asText());
    assertFalse(result.importReport().hasWarnings(), result.importReport().warnings().toString());
  }

  @Test
  void importsAnUnversionedFileAndMergesRepeatedPerks() throws Exception {
    CharacterFile result = importer.importCharacter(fixture("test-v1.json"));
    Character character = result.character();

    assertEquals(1, result.importReport().sourceSchemaVersion());
    PerkSelection lifeGiver = character.getPerks().get(0);
    assertEquals("life-giver", lifeGiver.perk().id());
    assertEquals(2, lifeGiver.rank());
    assertEquals(2, lifeGiver.importedRanks());
    assertEquals(1, character.getPerks().get(1).rank());
    assertTrue(hasInfo(result, "perks"));
    assertEquals(2, character.skillRank(Skill.MEDICINE));
    assertEquals(0, character.skillRank(Skill.SNEAK));
    assertTrue(hasInfo(result, "skills"));
    assertNull(character.getVitals().currentHp(), "no vitals in the file: health at maximum");
    assertEquals(0, character.getProgression().pendingPerks());
  }

  /** test-v1.json stores only HP 15; the rules give 6 + 6 + 2 + 2 × 6 (Life Giver 2) = 26. */
  @Test
  void reportsStoredDerivedValuesThatDifferFromTheRules() throws Exception {
    List<ImportWarning> derived = derivedWarnings(importer.importCharacter(fixture("test-v1.json")));

    assertEquals(1, derived.size());
    assertEquals(WarningSeverity.INFO, derived.get(0).severity());
    assertEquals("Stored values differ from the rules and were recomputed: HP 15 → 26.",
        derived.get(0).message());
  }

  @Test
  void readsLegacyMeleeTextAndImmunities() throws Exception {
    ObjectNode root = minimal();
    root.put("origin", "Super Mutant");
    ObjectNode attributes = (ObjectNode) root.get("attributes");
    attributes.put("str", 7);
    root.putArray("traits").add("Heavy Handed");
    ObjectNode derived = root.putObject("derived");
    derived.put("melee", "+1 CD (+1 CD Heavy Handed)");
    derived.put("radDR", "Immune");
    derived.put("poisDR", "Immune");
    derived.put("physDR", 0);
    assertFalse(hasInfo(importer.importCharacter(root), "derived"));

    derived.put("melee", "+0");
    derived.put("radDR", 0);
    assertEquals(List.of("Stored values differ from the rules and were recomputed: melee bonus dice "
        + "0 → 2, radiation DR 0 → Immune."),
        derivedWarnings(importer.importCharacter(root)).stream().map(ImportWarning::message).toList());
  }

  /** The legacy app stored "Heals" as radiation DR of a ghoul; it counts as immunity. */
  @Test
  void readsLegacyHealsAsRadiationImmunity() throws Exception {
    ObjectNode root = minimal();
    root.put("origin", "Ghoul");
    root.putObject("derived").put("radDR", "Heals");
    assertTrue(derivedWarnings(importer.importCharacter(root)).isEmpty());
  }

  @Test
  void keepsOutOfScopeOriginsAsLegacyEntries() throws Exception {
    ObjectNode root = minimal();
    root.put("origin", "NCR");
    Character character = importer.importCharacter(root).character();
    assertEquals("legacy-ncr", character.getOrigin().id());
    assertEquals("NCR", character.getOrigin().legacyName());

    root.put("origin", "FollowerApocalypse");
    assertEquals("legacy-follower-apocalypse", importer.importCharacter(root).character().getOrigin().id());
  }

  @Test
  void mapsInternalOriginIds() throws Exception {
    ObjectNode root = minimal();
    root.put("origin", "Brotherhood");
    assertEquals("brotherhood-initiate", importer.importCharacter(root).character().getOrigin().id());
    root.put("origin", "Gen3Synth");
    assertEquals("generation-3-synth", importer.importCharacter(root).character().getOrigin().id());
  }

  @Test
  void warnsAboutUnknownOrigin() throws Exception {
    ObjectNode root = minimal();
    root.put("origin", "Space Pirate");
    CharacterFile result = importer.importCharacter(root);
    assertEquals("legacy-space-pirate", result.character().getOrigin().id());
    assertTrue(result.importReport().hasWarnings());
  }

  @Test
  void keepsUnknownSkillsAndInvalidPerksInThePayload() throws Exception {
    ObjectNode root = minimal();
    root.putObject("skills").putObject("Hacking").put("tag", false).put("rank", 2);
    root.putArray("perks").add(42).addObject().put("name", "Intense Training").put("rank", 2)
        .putArray("choices").add("agi").add("per");
    CharacterFile result = importer.importCharacter(root);
    ObjectNode payload = result.character().getLegacyImport().payload();

    assertTrue(payload.get("unknownSkills").has("Hacking"));
    assertEquals(42, payload.get("invalidPerks").get(0).asInt());
    assertEquals(List.of(Attribute.AGILITY, Attribute.PERCEPTION),
        result.character().getPerks().get(0).attributeChoices());
    assertTrue(result.importReport().hasWarnings());
  }

  @Test
  void keepsInvalidTraitsAndChoicesInsteadOfCrashing() throws Exception {
    ObjectNode root = minimal();
    root.putArray("traits").add("!").add("Gifted");
    root.putArray("perks").add("?").addObject().put("name", "Intense Training").put("rank", 1)
        .putArray("choices").add("xyz");
    CharacterFile result = importer.importCharacter(root);
    ObjectNode payload = result.character().getLegacyImport().payload();

    assertEquals(1, result.character().getTraits().size());
    assertEquals("!", payload.get("invalidTraits").get(0).asText());
    assertEquals("?", payload.get("invalidPerks").get(0).asText());
    assertEquals("xyz", payload.get("invalidPerkChoices").get("Intense Training").get(0).asText());
    assertTrue(result.character().getPerks().get(0).attributeChoices().isEmpty());
    assertTrue(result.importReport().hasWarnings());
  }

  @Test
  void supportedBookOriginsNeedNoReportEntry() throws Exception {
    for (String origin : List.of("Tribal", "ChildOfAtom", "Mister Handy")) {
      ObjectNode root = minimal();
      root.put("origin", origin);
      CharacterFile result = importer.importCharacter(root);
      assertTrue(result.importReport().warnings().stream().noneMatch(w -> w.field().equals("origin")), origin);
    }
  }

  @Test
  void clampsRadiationPoints() throws Exception {
    ObjectNode root = minimal();
    root.putObject("vitals").put("currentHP", 8).put("rads", 3).put("radiationPoints", 9);
    CharacterFile result = importer.importCharacter(root);
    assertEquals(5, result.character().getVitals().radiationPoints());
    assertEquals(3, result.character().getVitals().rads());
    assertTrue(hasInfo(result, "vitals"));
  }

  @Test
  void rejectsUnsupportedFiles() {
    ObjectNode future = minimal();
    future.put("schemaVersion", 7);
    assertThrows(SaveFormatException.class, () -> importer.importCharacter(future));
    ObjectNode noAttributes = minimal();
    noAttributes.remove("attributes");
    assertThrows(SaveFormatException.class, () -> importer.importCharacter(noAttributes));
    ObjectNode noOrigin = minimal();
    noOrigin.remove("origin");
    assertThrows(SaveFormatException.class, () -> importer.importCharacter(noOrigin));
    ObjectNode emptyOrigin = minimal();
    emptyOrigin.put("origin", "");
    assertThrows(SaveFormatException.class, () -> importer.importCharacter(emptyOrigin));
  }

  private static void assertTag(Character character, Skill skill, int rank) {
    assertTrue(character.skillState(skill).tagged(), skill.id());
    assertEquals(rank, character.skillRank(skill), skill.id());
  }

  private static List<ImportWarning> derivedWarnings(CharacterFile result) {
    return result.importReport().warnings().stream().filter(w -> w.field().equals("derived")).toList();
  }

  private static boolean hasInfo(CharacterFile result, String field) {
    return result.importReport().warnings().stream()
        .anyMatch(w -> w.severity() == WarningSeverity.INFO && w.field().equals(field));
  }

  private static ObjectNode minimal() {
    ObjectNode root = JSON.createObjectNode();
    root.put("schemaVersion", 2);
    root.put("origin", "Survivor");
    ObjectNode attributes = root.putObject("attributes");
    for (Attribute attribute : Attribute.values()) {
      attributes.put(attribute.id(), 5);
    }
    return root;
  }

  static ObjectNode fixture(String name) throws IOException {
    try (InputStream in = LegacyCharacterImporterTest.class.getResourceAsStream("/legacy/" + name)) {
      assertNotNull(in, name);
      return (ObjectNode) JSON.readTree(in);
    }
  }
}
