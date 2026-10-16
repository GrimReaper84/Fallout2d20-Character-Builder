package de.fallout2d20.charbuilder.persistence.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.equipment.ArmAttachment;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.LegacyImport;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.apparel.CustomApparel;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.equipment.ArmSelection;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.CustomWeapon;
import de.fallout2d20.charbuilder.persistence.CharacterFile;
import de.fallout2d20.charbuilder.persistence.SaveFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    assertNull(character.getVitals().currentHp(), "current HP 11 = legacy maximum 11: at maximum");
    assertEquals(EnumSet.allOf(Book.class), character.getEnabledBooks(), "legacy saves know no books");
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
    assertEquals("small-frame", character.getTraits().getFirst().trait().id());
    assertTrue(character.getTraits().getFirst().imported());
  }

  @Test
  void keepsUnportedFieldsOfTheV2SurvivorFixtureWithoutWarnings() throws Exception {
    CharacterFile result = importer.importCharacter(fixture("test-survivor-v2.json"));

    LegacyImport legacy = result.character().getLegacyImport();
    assertNotNull(legacy);
    assertEquals(2, legacy.sourceSchemaVersion());
    assertEquals("2026-10-15", legacy.importedOn());
    for (String field : List.of("equipment", "equipmentPack", "trinket", "originOptions", "weapons", "armor")) {
      assertFalse(legacy.payload().has(field), field + " is imported");
    }
    assertFalse(legacy.payload().has("derived"), "derived values are recomputed, not kept");
    assertFalse(hasInfo(result, "derived"), "JC's stored derived values match the rules");
    assertFalse(result.importReport().hasWarnings(), result.importReport().warnings().toString());
  }

  /**
   * Like JC.json: a Laser Gun with 3 CD (book: 4 CD, CRB p. 101) and 16 shots in the magazine becomes the
   * catalog weapon with a note, and the shots become 16 fusion cells (CRB p. 91, its ammunition).
   */
  @Test
  void importsTheLaserGunAndItsShotsAsFusionCells() throws Exception {
    CharacterFile result = importer.importCharacter(fixture("test-survivor-v2.json"));
    Character character = result.character();

    CarriedWeapon laser = character.getWeapons().getFirst();
    assertEquals(CatalogRef.of("laser-gun"), laser.weapon());
    assertEquals("Legacy values: damage 3 CD", laser.notes());
    assertEquals(Map.of("fusion-cell", 16), character.getAmmunition());
    assertTrue(hasInfo(result, "weapons"));
  }

  /**
   * Unarmed Strike is skipped (every character has it); shots written with decimals still count; the
   * Gatling Laser's shots go to fusion cells with a hint that it also takes a fusion core (CRB p. 106).
   */
  @Test
  void skipsUnarmedStrikeReadsDecimalShotsAndHintsAtAlternativeAmmunition() throws Exception {
    ObjectNode root = minimal();
    ArrayNode weapons = root.putArray("weapons");
    weapons.addObject().put("name", "Unarmed Strike").put("dmg", "2");
    weapons.addObject().put("name", "Gatling Laser").put("dmg", "3").put("eff", "Burst, Piercing 1")
        .put("rate", "6").put("range", "M").put("mag", 100.0).put("pool", "50.0");
    CharacterFile result = importer.importCharacter(root);

    assertEquals(List.of("Gatling Laser"),
        result.character().getWeapons().stream().map(CarriedWeapon::displayName).toList());
    assertEquals(Map.of("fusion-cell", 150), result.character().getAmmunition());
    assertTrue(result.importReport().warnings().stream()
        .anyMatch(warning -> warning.message().contains("Fusion Core")));
    assertFalse(result.importReport().hasWarnings());
  }

  /** WOA pp. 22–23: a legacy Protectron keeps its pack (by book name) and its built-in weapon (legacy name). */
  @Test
  void importsWinterOfAtomPacksAndProtectronWeapons() throws Exception {
    ObjectNode root = minimal();
    root.put("origin", "Protectron");
    root.put("equipmentPack", "Protectron Medic");
    root.putArray("weapons").addObject().put("name", "Protectron Shock Hands").put("dmg", "4").put("type", "Energy");
    CharacterFile result = importer.importCharacter(root);

    CatalogRef pack = result.character().getStartingEquipment().pack();
    assertEquals("protectron-medic", pack == null ? null : pack.id());
    assertEquals(List.of("shock-hands"), result.character().getWeapons().stream()
        .map(weapon -> weapon.data().map(WeaponData::id).orElse("custom")).toList());
  }

  /** The hand-typed DR per location becomes one custom item per location with a value (user decision). */
  @Test
  void legacyArmorValuesBecomeCustomItems() throws Exception {
    ObjectNode root = minimal();
    ObjectNode armor = root.putObject("armor");
    armor.putObject("Head").put("physical", 2).put("energy", 1).put("radiation", 0);
    armor.putObject("Torso").put("physical", 0).put("energy", 0).put("radiation", 0);
    armor.putObject("Tail").put("physical", 1);
    CharacterFile result = importer.importCharacter(root);

    List<WornApparel> apparel = result.character().getApparel();
    assertEquals(1, apparel.size());
    assertEquals(new CustomApparel("Legacy armor (Head)", ApparelKind.ARMOR, Set.of(HitLocation.HEAD), 2, 1, 0,
        false, 0), apparel.getFirst().custom());
    assertTrue(result.character().getLegacyImport().payload().has("unknownArmorLocations"));
    assertTrue(result.importReport().hasWarnings());
  }

  /** A Mister Handy location of the legacy map becomes plating for that location (CRB p. 146). */
  @Test
  void legacyRobotLocationsBecomePlating() throws Exception {
    ObjectNode root = minimal();
    root.putObject("armor").putObject("Optics").put("physical", "2").put("energy", 1.0);
    CustomApparel optics = importer.importCharacter(root).character().getApparel().getFirst().custom();
    assertEquals(new CustomApparel("Legacy armor (Optics)", ApparelKind.ROBOT_PLATING, Set.of(HitLocation.OPTICS),
        2, 1, 0, false, 0), optics);
  }

  @Test
  void armorThatIsNotAMapStaysInThePayload() throws Exception {
    ObjectNode root = minimal();
    root.put("armor", "leather");
    CharacterFile result = importer.importCharacter(root);
    assertTrue(result.character().getApparel().isEmpty());
    assertEquals("leather", result.character().getLegacyImport().payload().get("armor").asText());
    assertTrue(result.importReport().hasWarnings());
  }

  @Test
  void unknownWeaponsBecomeCustomWeaponsAndKeepTheirShotsInTheNote() throws Exception {
    ObjectNode root = minimal();
    ObjectNode blaster = root.putArray("weapons").addObject();
    blaster.put("name", "Alien Blaster").put("dmg", "5").put("eff", "-").put("type", "Ener/Rad")
        .put("qual", "-").put("rate", "1").put("range", "M").put("mag", 4).put("pool", 6);
    root.withArray("weapons").addObject().put("dmg", "3");
    CharacterFile result = importer.importCharacter(root);

    CarriedWeapon weapon = result.character().getWeapons().getFirst();
    assertEquals(new CustomWeapon("Alien Blaster", null, 5, "", "Energy/Radiation", 1, WeaponRange.MEDIUM, "",
        null), weapon.custom());
    assertEquals("Legacy ammunition: 10 shots", weapon.notes());
    assertEquals(1, result.character().getWeapons().size());
    assertTrue(result.character().getAmmunition().isEmpty());
    assertTrue(result.importReport().hasWarnings());
    assertEquals(1, result.character().getLegacyImport().payload().get("invalidWeapons").size());
  }

  @Test
  void importsAnUnversionedFileAndMergesRepeatedPerks() throws Exception {
    CharacterFile result = importer.importCharacter(fixture("test-v1.json"));
    Character character = result.character();

    assertEquals(1, result.importReport().sourceSchemaVersion());
    PerkSelection lifeGiver = character.getPerks().getFirst();
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

  /** Current HP below the stored legacy maximum are kept (damage taken). */
  @Test
  void keepsCurrentHpBelowTheLegacyMaximum() throws Exception {
    ObjectNode root = fixture("test-survivor-v2.json");
    ((ObjectNode) root.get("vitals")).put("currentHP", 7);
    Character character = importer.importCharacter(root).character();
    assertEquals(7, character.getVitals().currentHp());
  }

  /** The imported Tags are the starting Tag skills (CRB p. 81 Tag skill items). */
  @Test
  void importedTagsAreTheStartingTagSkills() throws Exception {
    Character character = importer.importCharacter(fixture("test-survivor-v2.json")).character();
    for (Skill skill : Skill.values()) {
      assertEquals(character.skillState(skill).tagged(), character.getStartingTagSkills().contains(skill),
          skill.id());
    }
  }

  /** test-v1.json stores only HP 15; the rules give 6 + 6 + 2 + 2 × 6 (Life Giver 2) = 26. */
  @Test
  void reportsStoredDerivedValuesThatDifferFromTheRules() throws Exception {
    List<ImportWarning> derived = derivedWarnings(importer.importCharacter(fixture("test-v1.json")));

    assertEquals(1, derived.size());
    assertEquals(WarningSeverity.INFO, derived.getFirst().severity());
    assertEquals("Stored values differ from the rules and were recomputed: HP 15 → 26.",
        derived.getFirst().message());
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

  /** JC.json: Survivor with the Settler pack (CRB p. 79); the edited equipment text becomes the notes. */
  @Test
  void importsPackTrinketAndEquipmentTextOfTheV2SurvivorFixture() throws Exception {
    ObjectNode root = fixture("test-survivor-v2.json");
    Character character = importer.importCharacter(root).character();
    StartingEquipment equipment = character.getStartingEquipment();
    assertEquals(new CatalogRef("settler", "Settler"), equipment.pack());
    assertEquals(Arrays.asList((String) null), equipment.choices(), "choices only exist in the legacy text");
    assertNull(equipment.trinket(), "the fixture's trinket field is empty");
    assertEquals(List.of(), equipment.arms());
    assertEquals(root.get("equipment").asText(), character.getInventoryNotes());
  }

  /** CRB p. 77: legacy arms that fit the pack (in any order) keep the pack's arms. */
  @Test
  void importsMisterHandyArmsThatFitThePack() throws Exception {
    ObjectNode root = misterHandy("Nurse Handy", "Flamer", "Buzz-Saw", "Pincer");
    root.put("trinket", "  Oil can ");
    CharacterFile result = importer.importCharacter(root);
    StartingEquipment equipment = result.character().getStartingEquipment();
    assertFalse(equipment.freeArms());
    assertEquals(List.of(ArmSelection.of(ArmAttachment.PINCER), ArmSelection.of(ArmAttachment.BUZZ_SAW),
        ArmSelection.of(ArmAttachment.FLAMER)), equipment.arms());
    assertEquals("Oil can", equipment.trinket());
    assertFalse(result.character().getLegacyImport().payload().has("originOptions"));
    assertFalse(result.importReport().hasWarnings(), result.importReport().warnings().toString());
    assertFalse(hasInfo(result, "originOptions"));
  }

  /** CRB p. 77: a Miss Nanny whose third arm was never chosen keeps the pack's arms with that arm open. */
  @Test
  void importsIncompleteArmsThatFitThePack() throws Exception {
    CharacterFile result = importer.importCharacter(misterHandy("Miss Nanny", "Flamer", "Pincer"));
    StartingEquipment equipment = result.character().getStartingEquipment();
    assertFalse(equipment.freeArms());
    assertEquals(List.of(ArmSelection.of(ArmAttachment.PINCER), ArmSelection.of(ArmAttachment.FLAMER),
        ArmSelection.OPEN), equipment.arms());
    assertFalse(hasInfo(result, "originOptions"));
  }

  /** Arms the pack does not allow become freely configurable arms, keeping the legacy positions. */
  @Test
  void importsOtherArmsAsFreeArms() throws Exception {
    CharacterFile result = importer.importCharacter(misterHandy("Mister Handy", "Pincer", "", "Pincer"));
    StartingEquipment equipment = result.character().getStartingEquipment();
    assertTrue(equipment.freeArms());
    assertEquals(List.of(ArmSelection.of(ArmAttachment.PINCER), ArmSelection.OPEN,
        ArmSelection.of(ArmAttachment.PINCER)), equipment.arms());
    assertTrue(hasInfo(result, "originOptions"));

    StartingEquipment packArms = importer.importCharacter(misterHandy("Mister Gutsy")).character()
        .getStartingEquipment();
    assertEquals(List.of(ArmSelection.of(ArmAttachment.AUTO_PISTOL_10MM), ArmSelection.of(ArmAttachment.BUZZ_SAW),
        ArmSelection.of(ArmAttachment.LASER_EMITTER)), packArms.arms(), "no legacy names: the pack's arms");
  }

  /** CRB p. 55: "no pincer" now follows from the arms; a contradicting legacy flag is reported. */
  @Test
  void reportsANoPincerFlagThatContradictsTheArms() throws Exception {
    ObjectNode handy = misterHandy("Mister Handy", "Pincer", "Flamer", "Buzz-Saw");
    ((ObjectNode) handy.get("originOptions")).put("noPincer", true);
    CharacterFile result = importer.importCharacter(handy);
    assertTrue(result.importReport().warnings().stream().anyMatch(w -> w.severity() == WarningSeverity.INFO
        && w.field().equals("originOptions") && w.message().contains("the arms count")));
    assertFalse(result.character().getStartingEquipment().lacksPincer());

    ObjectNode gutsy = misterHandy("Mister Gutsy");
    ((ObjectNode) gutsy.get("originOptions")).put("noPincer", true);
    CharacterFile consistent = importer.importCharacter(gutsy);
    assertTrue(consistent.character().getStartingEquipment().lacksPincer());
    assertFalse(hasInfo(consistent, "originOptions"));
  }

  /** A pack outside the catalog (e.g. homebrew) stays in the legacy data together with its trinket. */
  @Test
  void keepsUnknownPacksWithTheirTrinket() throws Exception {
    ObjectNode unknownPack = minimal();
    unknownPack.put("equipmentPack", "Lone Wanderer");
    unknownPack.put("trinket", "Broken holotape");
    CharacterFile result = importer.importCharacter(unknownPack);
    assertEquals(StartingEquipment.NONE, result.character().getStartingEquipment());
    ObjectNode payload = result.character().getLegacyImport().payload();
    assertEquals("Lone Wanderer", payload.get("equipmentPack").asText());
    assertEquals("Broken holotape", payload.get("trinket").asText());
    assertTrue(has(result, WarningSeverity.WARNING, "equipmentPack"));

    ObjectNode numberPack = minimal();
    numberPack.put("equipmentPack", 7);
    CharacterFile odd = importer.importCharacter(numberPack);
    assertEquals(7, odd.character().getLegacyImport().payload().get("equipmentPack").asInt(), "kept, not dropped");
    assertTrue(odd.importReport().warnings().stream().anyMatch(w -> w.message().contains("equipmentPack")));
  }

  @Test
  void keepsUnknownArmAttachments() throws Exception {
    CharacterFile claw = importer.importCharacter(misterHandy("Mister Handy", "Claw", "Pincer", "Flamer"));
    assertEquals("[\"Claw\"]",
        claw.character().getLegacyImport().payload().get("originOptions").get("attachments").toString());
    assertTrue(has(claw, WarningSeverity.WARNING, "originOptions"));
  }

  /** Typical for v1 files: a Mister Handy with arms but no equipment pack. */
  @Test
  void keepsArmsWithoutAMisterHandyPack() throws Exception {
    ObjectNode root = minimal();
    root.put("origin", "Mister Handy");
    root.putObject("originOptions").put("noPincer", true).putArray("attachments").add("Pincer");
    CharacterFile result = importer.importCharacter(root);
    assertEquals(StartingEquipment.NONE, result.character().getStartingEquipment());
    assertEquals("{\"attachments\":[\"Pincer\"]}",
        result.character().getLegacyImport().payload().get("originOptions").toString());
    assertTrue(has(result, WarningSeverity.WARNING, "originOptions"));
    assertTrue(hasInfo(result, "originOptions"), "the no-pincer flag cannot be kept without arms");
  }

  @Test
  void ignoresPincerAndArmsOfOtherOrigins() throws Exception {
    ObjectNode survivor = minimal();
    survivor.putObject("originOptions").put("noPincer", true).putArray("attachments").add("Pincer");
    CharacterFile ignored = importer.importCharacter(survivor);
    assertEquals(OriginChoices.NONE, ignored.character().getOriginChoices());
    assertFalse(ignored.character().getLegacyImport().payload().has("originOptions"));
    assertTrue(hasInfo(ignored, "originOptions"));
  }

  private static ObjectNode misterHandy(String pack, String... attachments) {
    ObjectNode root = minimal();
    root.put("origin", "Mister Handy");
    root.put("equipmentPack", pack);
    ArrayNode names = root.putObject("originOptions").put("noPincer", false).putArray("attachments");
    for (String attachment : attachments) {
      names.add(attachment);
    }
    return root;
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
        result.character().getPerks().getFirst().attributeChoices());
    assertTrue(result.importReport().hasWarnings());
  }

  /** User decision: perks outside the catalog (e.g. the legacy invention "Thief") are kept and reported. */
  @Test
  void warnsAboutPerksNotInTheCatalog() throws Exception {
    ObjectNode root = minimal();
    root.putArray("perks").add("Thief").add("Gun Nut");
    CharacterFile result = importer.importCharacter(root);
    assertEquals(2, result.character().getPerks().size());
    assertTrue(result.importReport().warnings().stream().anyMatch(w -> w.severity() == WarningSeverity.WARNING
        && w.message().equals("Perks not in the supported books kept by their legacy name: Thief.")));
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
    assertTrue(result.character().getPerks().getFirst().attributeChoices().isEmpty());
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

  /** WOA p. 25: a Child of Atom stores up to 5 Radiation Points, other origins none. */
  @Test
  void clampsRadiationPointsToTheOriginMaximum() throws Exception {
    ObjectNode root = minimal();
    root.put("origin", "ChildOfAtom");
    root.putObject("vitals").put("rads", 3).put("radiationPoints", 9);
    CharacterFile result = importer.importCharacter(root);
    assertEquals(5, result.character().getVitals().radiationPoints());
    assertEquals(3, result.character().getVitals().rads());
    assertTrue(hasInfo(result, "vitals"));

    // Other origins store none (before D5 the import kept up to 5 for every origin).
    ObjectNode survivor = minimal();
    survivor.putObject("vitals").put("radiationPoints", 2);
    CharacterFile survivorResult = importer.importCharacter(survivor);
    assertEquals(0, survivorResult.character().getVitals().radiationPoints());
    assertTrue(survivorResult.importReport().warnings().stream()
        .anyMatch(w -> w.field().equals("vitals") && w.message().contains("reduced to 0")));
  }

  /** The legacy app ignored rads for the maximum HP; CRB p. 31 lowers it, and the current HP with it. */
  @Test
  void lowersCurrentHpAboveTheRadLoweredMaximum() throws Exception {
    ObjectNode root = minimal();
    root.putObject("vitals").put("currentHP", 9).put("rads", 3);
    CharacterFile result = importer.importCharacter(root);
    assertNull(result.character().getVitals().currentHp(), "9 above END 5 + LCK 5 − 3 rads: at maximum");
    assertTrue(result.importReport().warnings().stream()
        .anyMatch(w -> w.field().equals("vitals") && w.message().contains("lowered to 7")));
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
    return has(result, WarningSeverity.INFO, field);
  }

  private static boolean has(CharacterFile result, WarningSeverity severity, String field) {
    return result.importReport().warnings().stream()
        .anyMatch(w -> w.severity() == severity && w.field().equals(field));
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
