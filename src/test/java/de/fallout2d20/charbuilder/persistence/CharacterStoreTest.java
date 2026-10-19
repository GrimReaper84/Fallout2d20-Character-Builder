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
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.equipment.ArmAttachment;
import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.apparel.CustomApparel;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.equipment.ArmSelection;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.CustomItem;
import de.fallout2d20.charbuilder.model.character.item.Holotape;
import de.fallout2d20.charbuilder.model.character.item.Materials;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.CustomWeapon;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CharacterStoreTest {

  private static final ObjectMapper JSON = new ObjectMapper();
  private final CharacterStore store =
      new CharacterStore(Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneOffset.UTC), "1.0.0");

  @TempDir Path dir;

  /** A GM skill correction (D2) survives a save and is written only when not 0; GM mode itself is not saved. */
  @Test
  void roundTripsGmSkillCorrections() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.completeCreation();
    character.setGmMode(true);
    character.abilities().adjustSkillRankAsGm(Skill.REPAIR, 2);
    Path file = dir.resolve("gm.json");
    store.save(character, file);

    PlayerCharacter loaded = store.load(file).character();
    assertEquals(2, loaded.abilities().skillState(Skill.REPAIR).gmRanks());
    assertEquals(2, loaded.abilities().skillRank(Skill.REPAIR));
    assertFalse(loaded.isGmMode());
    JsonNode skills = JSON.readTree(file.toFile()).get("skills");
    assertEquals(2, skills.get(Skill.REPAIR.id()).get("gmRanks").asInt());
    assertNull(skills.get(Skill.SCIENCE.id()).get("gmRanks"));
  }

  /** Session vitals (D5) survive a save; a full character writes no Luck points, injuries or notes. */
  @Test
  void roundTripsSessionVitals() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.completeCreation();
    Path file = dir.resolve("vitals.json");
    store.save(character, file);
    JsonNode written = JSON.readTree(file.toFile()).get("vitals");
    assertNull(written.get("luckPoints"));
    assertNull(written.get("injuries"));
    assertNull(written.get("notes"));

    character.session().setRads(2);
    character.session().setCurrentHp(5);
    character.session().setLuckPoints(1);
    character.session().setInjured(HitLocation.RIGHT_LEG, true);
    character.session().setInjured(HitLocation.HEAD, true);
    character.session().setNotes("Bleeding torso treated");
    store.save(character, file);
    assertEquals(character.session().getVitals(), store.load(file).character().session().getVitals());
    assertEquals("[\"HEAD\",\"RIGHT_LEG\"]", JSON.readTree(file.toFile()).get("vitals").get("injuries").toString());
  }

  /** Items (catalog, magazine issue, custom), caps and materials survive a save; empty parts are not written. */
  @Test
  void roundTripsTheInventory() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addItem(CarriedItem.of(CatalogRef.of("stimpak")).withQuantity(2).withNotes("for Nora"));
    character.gear()
        .addItem(CarriedItem.of(CatalogRef.of("tumblers-today")).withIssue("Open Any Lock in 5 Seconds Flat"));
    character.gear().addItem(CarriedItem.of(new CustomItem("Teddy bear", 1, "Cute")));
    character.gear().setCaps(123);
    character.gear().addApparel(WornApparel.of(CatalogRef.of("tough-clothing")));
    character.gear().addApparel(WornApparel.of(CatalogRef.of("drifter-outfit")));
    character.gear().setMaterials(new Materials(3, 2, 1, 0));
    Path file = dir.resolve("inventory.json");
    store.save(character, file);

    PlayerCharacter loaded = store.load(file).character();
    assertEquals(character.gear().getItems(), loaded.gear().getItems());
    assertEquals(123, loaded.gear().getCaps());
    assertEquals(List.of(false, true), loaded.gear().getApparel().stream().map(WornApparel::carried).toList());
    assertNull(JSON.readTree(file.toFile()).get("apparel").get(0).get("carried"), "worn is not written");
    assertEquals(new Materials(3, 2, 1, 0), loaded.gear().getMaterials());

    Path empty = dir.resolve("empty.json");
    store.save(PlayerCharacter.createNew(), empty);
    JsonNode written = JSON.readTree(empty.toFile());
    assertNull(written.get("items"));
    assertNull(written.get("caps"));
    assertNull(written.get("materials"));
  }

  /**
   * Holotapes and the starting-equipment marker survive a save (added 2026-10-19, format 1); a file without them
   * loads with entries added by hand, and an unknown marker counts as added by hand.
   */
  @Test
  void roundTripsHolotapesAndStartingSources() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.startingGear().choosePack(CatalogRef.of("settler"));
    character.startingGear().chooseOption(0, "Pipe wrench");
    character.startingGear().apply(character.startingGear().getSelection());
    character.gear().addItem(CarriedItem.of(new Holotape("Vault 81 code", "4-8-15")));
    Path file = dir.resolve("starting.json");
    store.save(character, file);

    PlayerCharacter loaded = store.load(file).character();
    assertEquals(character.gear().getWeapons(), loaded.gear().getWeapons());
    assertEquals(character.gear().getApparel(), loaded.gear().getApparel());
    assertEquals(character.gear().getItems(), loaded.gear().getItems());
    JsonNode written = JSON.readTree(file.toFile());
    assertEquals("PACK", written.get("weapons").get(0).get("startingSource").asText());
    assertEquals("4-8-15", written.get("items").get(1).get("holotape").get("label").asText());

    ObjectNode edited = (ObjectNode) written;
    ((ObjectNode) edited.get("weapons").get(0)).put("startingSource", "FROM_THE_FUTURE");
    ((ObjectNode) edited.get("weapons").get(1)).remove("startingSource");
    JSON.writeValue(file.toFile(), edited);
    List<CarriedWeapon> weapons = store.load(file).character().gear().getWeapons();
    assertNull(weapons.get(0).startingSource());
    assertNull(weapons.get(1).startingSource());
  }

  /** A holotape without a name is a broken file, reported like any other format error. */
  @Test
  void rejectsAHolotapeWithoutName() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addItem(CarriedItem.of(new Holotape("Code", "")));
    Path file = dir.resolve("holotape.json");
    store.save(character, file);
    ObjectNode written = (ObjectNode) JSON.readTree(file.toFile());
    ((ObjectNode) written.get("items").get(0).get("holotape")).put("name", " ");
    JSON.writeValue(file.toFile(), written);
    assertThrows(SaveFormatException.class, () -> store.load(file));
  }

  /** A character whose pack items an earlier version added by button is never synced, so nothing doubles. */
  @Test
  void earlierAddedPackItemsAreNotSynced() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.startingGear().choosePack(CatalogRef.of("settler"));
    Path file = dir.resolve("added.json");
    store.save(character, file);
    ObjectNode written = (ObjectNode) JSON.readTree(file.toFile());
    written.put("packItemsAdded", true);
    JSON.writeValue(file.toFile(), written);

    PlayerCharacter loaded = store.load(file).character();
    assertTrue(loaded.startingGear().isPackItemsAdded());
    assertFalse(loaded.startingGear().canApply());
    loaded.abilities().setFreeTag(Skill.LOCKPICK, true);
    assertEquals("", loaded.gear().getNotes(), "no Tag skill roll line");
    assertTrue(loaded.gear().getItems().isEmpty());
  }

  @Test
  void roundTripsANewCharacter() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.setName("Nora");
    Path file = dir.resolve("nora.json");
    store.save(character, file);

    CharacterFile loaded = store.load(file);
    assertNull(loaded.importReport());
    assertEquals("Nora", loaded.character().getName());
    assertEquals(5, loaded.character().abilities().attributeValue(Attribute.LUCK));
    JsonNode written = JSON.readTree(file.toFile());
    assertEquals("fallout2d20-character-builder", written.get("format").asText());
    assertEquals(1, written.get("formatVersion").asInt());
    assertEquals("1.0.0", written.get("savedWith").asText());
    assertFalse(written.has("schemaVersion"), "no longer continues the legacy numbering");
    assertFalse(written.has("createdWith"), "unknown for a character made without an app version");
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
    PlayerCharacter before = imported.character();
    PlayerCharacter after = reloaded.character();

    assertNull(reloaded.importReport());
    assertTrue(after.isLegacyImport());
    for (Attribute attribute : Attribute.values()) {
      assertEquals(before.abilities().attributeValue(attribute), after.abilities().attributeValue(attribute));
    }
    for (Skill skill : Skill.values()) {
      assertEquals(before.abilities().skillRank(skill), after.abilities().skillRank(skill));
      assertEquals(before.abilities().skillState(skill), after.abilities().skillState(skill));
    }
    assertEquals(before.getPerks(), after.getPerks());
    assertEquals(before.getTraits(), after.getTraits());
    assertEquals(before.session().getVitals(), after.session().getVitals());
    assertEquals(before.getOrigin(), after.getOrigin());
    assertEquals(before.getLegacyImport().payload(), after.getLegacyImport().payload());
    assertEquals("2026-10-15", after.getLegacyImport().importedOn());
  }

  @Test
  void rejectsMissingRequiredParts() throws Exception {
    Path file = dir.resolve("broken.json");
    store.save(PlayerCharacter.createNew(), file);
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

  private SaveFormatException assertUnreadable(Path file, ObjectNode content) throws IOException {
    Files.writeString(file, content.toString());
    return assertThrows(SaveFormatException.class, () -> store.load(file));
  }

  /** CRB is always enabled, so a file listing only WOA loads (and saves) as CRB + WOA. */
  @Test
  void roundTripsEnabledBooks() throws Exception {
    Path file = dir.resolve("books.json");
    store.save(PlayerCharacter.createNew(), file);
    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());
    assertEquals("[\"CRB\",\"WOA\",\"WG\"]", node.get("books").toString());

    node.putArray("books").add("WOA");
    Files.writeString(file, node.toString());
    PlayerCharacter loaded = store.load(file).character();
    assertEquals(EnumSet.of(Book.CRB, Book.WOA), loaded.getEnabledBooks());
    store.save(loaded, file);
    assertEquals("[\"CRB\",\"WOA\"]", JSON.readTree(file.toFile()).get("books").toString());
  }

  /** Files written before the books field existed enable all books. */
  @Test
  void enablesAllBooksWhenTheFieldIsMissing() throws Exception {
    Path file = dir.resolve("old.json");
    store.save(PlayerCharacter.createNew(), file);
    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());
    node.remove("books");
    Files.writeString(file, node.toString());
    assertEquals(EnumSet.allOf(Book.class), store.load(file).character().getEnabledBooks());
  }

  @Test
  void rejectsUnknownBooks() throws Exception {
    Path file = dir.resolve("unknown-book.json");
    store.save(PlayerCharacter.createNew(), file);
    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());
    node.putArray("books").add("SG");
    assertUnreadable(file, node);
  }

  @Test
  void roundTripsOriginChoices() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    Path file = dir.resolve("origin.json");
    store.save(character, file);
    assertFalse(JSON.readTree(file.toFile()).has("originChoices"), "omitted when nothing is chosen");

    character.chooseOrigin(CatalogRef.of("vault-dweller"));
    character.chooseOriginTagSkill(Skill.SNEAK);
    character.setPurpose("Scout");
    store.save(character, file);
    PlayerCharacter loaded = store.load(file).character();
    assertEquals(new OriginChoices(Skill.SNEAK, "Scout"), loaded.getOriginChoices());
    assertEquals("sneak", JSON.readTree(file.toFile()).get("originChoices").get("tagSkill").asText());

    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());
    ((ObjectNode) node.get("originChoices")).put("tagSkill", "hacking");
    assertUnreadable(file, node);
  }

  @Test
  void roundTripsStartingEquipmentAndInventoryNotes() throws Exception {
    PlayerCharacter settler = PlayerCharacter.createNew();
    Path file = dir.resolve("settler.json");
    store.save(settler, file);
    JsonNode empty = JSON.readTree(file.toFile());
    assertFalse(empty.has("startingEquipment"), "omitted when nothing is chosen");
    assertFalse(empty.has("inventoryNotes"), "omitted when blank");

    settler.chooseOrigin(CatalogRef.of("survivor"));
    settler.abilities().setFreeTag(Skill.THROWING, true);
    settler.startingGear().choosePack(CatalogRef.of("raider"));
    settler.startingGear().chooseOption(1, "1 dose of RadAway");
    settler.startingGear().chooseTagSkillItem(Skill.THROWING, "2 + 1 CD tomahawks");
    settler.gear().setNotes("Owes 20 caps to the Diamond City bar");
    store.save(settler, file);
    PlayerCharacter loaded = store.load(file).character();
    assertEquals(settler.startingGear().getSelection(), loaded.startingGear().getSelection());
    assertEquals("Owes 20 caps to the Diamond City bar", loaded.gear().getNotes());
    JsonNode written = JSON.readTree(file.toFile()).get("startingEquipment");
    assertEquals("[null,\"1 dose of RadAway\",null]", written.get("choices").toString());
    assertEquals("2 + 1 CD tomahawks", written.get("tagSkillItems").get("throwing").asText());
    assertFalse(written.has("arms"));
  }

  /** Free arms (house-rule option) with a weapon text survive saving. */
  @Test
  void roundTripsMisterHandyArms() throws Exception {
    PlayerCharacter handy = PlayerCharacter.createNew();
    handy.chooseOrigin(CatalogRef.of("mister-handy"));
    handy.startingGear().choosePack(CatalogRef.of("nurse-handy"));
    handy.startingGear().setFreeArms(true);
    handy.startingGear().chooseArm(0, ArmSelection.of(ArmAttachment.PINCER));
    handy.startingGear().chooseArm(2, ArmSelection.ofWeapon("Hunting rifle"));
    Path file = dir.resolve("handy.json");
    store.save(handy, file);
    assertEquals(handy.startingGear().getSelection(), store.load(file).character().startingGear().getSelection());
    assertEquals("[{\"attachment\":\"PINCER\"},{\"attachment\":\"BUZZ_SAW\"},{\"weapon\":\"Hunting rifle\"}]",
        JSON.readTree(file.toFile()).get("startingEquipment").get("arms").toString());
  }

  /** Picks that no longer fit the catalog (e.g. after a text correction) are opened, not rejected. */
  @Test
  void opensPicksThatNoLongerFitTheCatalog() throws Exception {
    PlayerCharacter handy = PlayerCharacter.createNew();
    handy.chooseOrigin(CatalogRef.of("mister-handy"));
    handy.abilities().setFreeTag(Skill.EXPLOSIVES, true);
    handy.startingGear().choosePack(CatalogRef.of("mister-gutsy"));
    Path file = dir.resolve("stale.json");
    store.save(handy, file);
    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());
    ObjectNode equipment = (ObjectNode) node.get("startingEquipment");
    equipment.putArray("choices").add("Spoon");
    equipment.putObject("tagSkillItems").put("explosives", "3 Molotov cocktails").put("repair", "Multi-Tool");
    ((ArrayNode) equipment.get("arms")).remove(2);
    Files.writeString(file, node.toString());

    StartingEquipment loaded = store.load(file).character().startingGear().getSelection();
    assertEquals(List.of(), loaded.choices(), "Mister Gutsy has no choices");
    assertEquals(Map.of(), loaded.tagSkillItems());
    assertEquals(List.of(ArmSelection.of(ArmAttachment.AUTO_PISTOL_10MM), ArmSelection.of(ArmAttachment.BUZZ_SAW),
        ArmSelection.of(ArmAttachment.LASER_EMITTER)), loaded.arms(), "the pack's arms");
  }

  /** Pending points, staged ranks, starting Tags and current HP survive saving (D1). */
  @Test
  void roundTripsAdvancement() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.abilities().setFreeTag(Skill.SNEAK, true);
    character.completeCreation();
    character.advancement().addXp(1_000);
    character.advancement().stageSkillRank(Skill.REPAIR, 1);
    character.addPerkRank(CatalogRef.of("tag"), null, Skill.SCIENCE);
    Path file = dir.resolve("advanced.json");
    store.save(character, file);

    PlayerCharacter loaded = store.load(file).character();
    assertEquals(5, loaded.advancement().getLevel());
    assertEquals(new Progression(4, 4), loaded.advancement().getProgression());
    assertEquals(1, loaded.abilities().skillState(Skill.REPAIR).stagedRanks());
    assertEquals(1, loaded.getPerks().getFirst().stagedRanks());
    assertEquals(List.of(Skill.SCIENCE), loaded.getPerks().getFirst().skillChoices());
    assertEquals(Set.of(Skill.SNEAK), loaded.getStartingTagSkills());
    assertEquals(3, loaded.advancement().skillRanksLeft());
    assertEquals(3, loaded.advancement().perksLeft());
    JsonNode written = JSON.readTree(file.toFile());
    assertEquals(List.of("sneak"), List.of(written.get("startingTagSkills").get(0).asText()));
  }

  /** Catalog and custom weapons with quantity and note, and the ammunition per type (C1). */
  @Test
  void roundTripsWeaponsAndAmmunition() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")).withNotes("Long Barrel"));
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("frag-grenade")).withQuantity(3));
    character.gear().addWeapon(CarriedWeapon.of(new CustomWeapon("Alien Blaster", Skill.ENERGY_WEAPONS, 5, "Piercing 1",
        "Energy", 1, WeaponRange.MEDIUM, "Close Quarters", "fusion-cell")));
    character.gear().setAmmunition("10mm", 34);
    Path file = dir.resolve("weapons.json");
    store.save(character, file);

    PlayerCharacter loaded = store.load(file).character();
    assertEquals(character.gear().getWeapons(), loaded.gear().getWeapons());
    assertEquals(Map.of("10mm", 34), loaded.gear().getAmmunition());
  }

  /** D4: installed mods, custom mod slots and an unknown mod id survive a round trip; older files have none. */
  @Test
  void roundTripsWeaponMods() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    character.gear().setWeaponMod(0, ModSlot.RECEIVER, WeaponCatalog.modById("small-guns-hardened").orElseThrow());
    character.gear().addWeapon(CarriedWeapon.of(new CustomWeapon("Alien Blaster", Skill.ENERGY_WEAPONS, 5, "",
        "Energy", 1,
        WeaponRange.MEDIUM, "", null, EnumSet.of(ModSlot.BARREL, ModSlot.SIGHTS))));
    character.gear().setWeaponMod(1, ModSlot.BARREL, WeaponCatalog.modById("energy-weapons-long-barrel").orElseThrow());
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")).withMod(ModSlot.MUZZLE,
        CatalogRef.of("gone")));
    Path file = dir.resolve("mods.json");
    store.save(character, file);

    assertEquals(character.gear().getWeapons(), store.load(file).character().gear().getWeapons());
    JsonNode written = JSON.readTree(file.toFile());
    assertEquals("small-guns-hardened", written.get("weapons").get(0).get("mods").get("RECEIVER").get("id").asText());
  }

  /** D4: the new fields are optional – a weapon without mods writes neither, so older files load unchanged. */
  @Test
  void weaponsWithoutModsWriteNoModFields() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    character.gear().addWeapon(CarriedWeapon.of(new CustomWeapon("Alien Blaster", Skill.ENERGY_WEAPONS, 5, "",
        "Energy", 1,
        WeaponRange.MEDIUM, "", null)));
    Path file = dir.resolve("plain.json");
    store.save(character, file);

    JsonNode weapons = JSON.readTree(file.toFile()).get("weapons");
    assertFalse(weapons.get(0).has("mods"));
    assertFalse(weapons.get(1).has("mods"));
    assertFalse(weapons.get(1).get("custom").has("modSlots"));
    assertEquals(character.gear().getWeapons(), store.load(file).character().gear().getWeapons());
  }

  /** Slot names this version does not know (a newer file) are skipped instead of failing the load. */
  @Test
  void unknownModSlotNamesAreSkipped() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    character.gear().addWeapon(CarriedWeapon.of(new CustomWeapon("Alien Blaster", Skill.ENERGY_WEAPONS, 5, "",
        "Energy", 1,
        WeaponRange.MEDIUM, "", null, EnumSet.of(ModSlot.BARREL))));
    Path file = dir.resolve("future.json");
    store.save(character, file);
    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());
    ((ObjectNode) node.get("weapons").get(0)).putObject("mods").putObject("MAGAZINE").put("id", "small-guns-drum");
    ((ObjectNode) node.get("weapons").get(1).get("custom")).putArray("modSlots").add("MAGAZINE").add("BARREL");
    JSON.writeValue(file.toFile(), node);

    List<CarriedWeapon> loaded = store.load(file).character().gear().getWeapons();
    assertTrue(loaded.getFirst().mods().isEmpty());
    assertEquals(Set.of(ModSlot.BARREL), loaded.get(1).custom().modSlots());
  }

  /** Catalog item with chosen limb and note, and a custom item (C2). */
  @Test
  void roundTripsApparel() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addApparel(WornApparel.of(CatalogRef.of("raider-arm")).withLocation(HitLocation.LEFT_ARM)
        .withNotes("Studded"));
    character.gear().addApparel(WornApparel.of(new CustomApparel("Legacy armor (Head)", ApparelKind.ARMOR,
        Set.of(HitLocation.HEAD), 3, 2, 0, false, 0)));
    Path file = dir.resolve("apparel.json");
    store.save(character, file);
    assertEquals(character.gear().getApparel(), store.load(file).character().gear().getApparel());
  }

  @Test
  void characterInCreationWritesNoStartingTagSkills() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.abilities().setFreeTag(Skill.SNEAK, true);
    Path file = dir.resolve("creation.json");
    store.save(character, file);
    assertFalse(JSON.readTree(file.toFile()).has("startingTagSkills"));
  }

  /** A complete character cannot pick again, so stale picks keep their text. */
  @Test
  void completeCharactersKeepStalePicks() throws Exception {
    StartingEquipment settler = new StartingEquipment(CatalogRef.of("settler"), List.of("Spoon"), Map.of(),
        null, false, List.of());
    PlayerCharacter complete = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor"))
        .creationComplete(true).startingEquipment(settler).build();
    Path file = dir.resolve("complete.json");
    store.save(complete, file);
    assertEquals(List.of("Spoon"), store.load(file).character().startingGear().getSelection().choices());
  }

  @Test
  void rejectsUnknownAttachmentsAndSkills() throws Exception {
    PlayerCharacter handy = PlayerCharacter.createNew();
    handy.chooseOrigin(CatalogRef.of("mister-handy"));
    handy.startingGear().choosePack(CatalogRef.of("mister-gutsy"));
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

  /** The starting level is stored with the completion; characters in creation omit it. */
  @Test
  void roundTripsCompletionAndStartingLevel() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.advancement().setStartingLevel(2);
    Path file = dir.resolve("level2.json");
    store.save(character, file);
    assertFalse(JSON.readTree(file.toFile()).has("startingLevel"), "omitted during creation");

    character.completeCreation();
    store.save(character, file);
    PlayerCharacter loaded = store.load(file).character();
    assertTrue(loaded.isCreationComplete());
    assertEquals(OptionalInt.of(2), loaded.advancement().getStartingLevel());
    assertEquals(2, JSON.readTree(file.toFile()).get("startingLevel").asInt());
  }

  @Test
  void rejectsInvalidStartingLevel() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.advancement().setStartingLevel(2);
    character.completeCreation();
    Path file = dir.resolve("invalid-level.json");
    store.save(character, file);
    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());

    node.put("startingLevel", 3);
    assertStartingLevelMessage(assertUnreadable(file, node.deepCopy()));
    node.put("startingLevel", 2).put("creationComplete", false);
    assertStartingLevelMessage(assertUnreadable(file, node));
  }

  private static void assertStartingLevelMessage(SaveFormatException error) {
    assertTrue(error.getMessage().toLowerCase(Locale.ROOT).contains("starting level"), error.getMessage());
  }

  @Test
  void ignoresUnknownKeys() throws Exception {
    Path file = dir.resolve("extra.json");
    store.save(PlayerCharacter.createNew(), file);
    ObjectNode node = (ObjectNode) JSON.readTree(file.toFile());
    node.put("somethingNew", true);
    Files.writeString(file, node.toString());
    assertNull(store.load(file).importReport());
  }

  /** User decision 2026-10-17: the creating version is kept for good, the saving version follows each save. */
  @Test
  void keepsCreatedWithAndUpdatesSavedWith() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew("1.0.0");
    Path file = dir.resolve("new.json");
    store.save(character, file);
    CharacterStore newer = new CharacterStore(Clock.systemUTC(), "1.1.0");
    PlayerCharacter loaded = newer.load(file).character();
    assertEquals(Optional.of("1.0.0"), loaded.getCreatedWith());
    newer.save(loaded, file);
    JsonNode written = JSON.readTree(file.toFile());
    assertEquals("1.0.0", written.get("createdWith").asText());
    assertEquals("1.1.0", written.get("savedWith").asText());
  }

  /** User decision 2026-10-17: files of the 1.0.0 test builds still load and move to the new format on save. */
  @Test
  void readsTestBuildFilesAndWritesThemInTheNewFormat() throws Exception {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.setName("Tester");
    Path file = dir.resolve("test-build.json");
    store.save(character, file);
    ObjectNode testBuild = (ObjectNode) JSON.readTree(file.toFile());
    testBuild.remove(List.of("format", "formatVersion", "savedWith"));
    testBuild.put("schemaVersion", 3);
    Files.writeString(file, JSON.writeValueAsString(testBuild));

    CharacterFile loaded = store.load(file);
    assertNull(loaded.importReport(), "not a legacy import");
    assertEquals("Tester", loaded.character().getName());
    store.save(loaded.character(), file);
    JsonNode written = JSON.readTree(file.toFile());
    assertEquals(1, written.get("formatVersion").asInt());
    assertFalse(written.has("schemaVersion"));
  }

  @Test
  void newerFormatAsksForAnUpdate() throws Exception {
    Path file = dir.resolve("newer.json");
    Files.writeString(file, "{\"format\": \"fallout2d20-character-builder\", \"formatVersion\": 2, "
        + "\"savedWith\": \"1.4.0\", \"attributeAllocation\": {}, \"skills\": {}}");
    SaveFormatException error = assertThrows(SaveFormatException.class, () -> store.load(file));
    assertTrue(error.getMessage().contains("version 1.4.0"), error.getMessage());
    assertTrue(error.getMessage().contains("update"), error.getMessage());
  }

  @Test
  void rejectsForeignFormatsAndInvalidFormatVersions() throws Exception {
    assertLoadFails("{\"format\": \"something-else\", \"formatVersion\": 1}", "Not a character file");
    assertLoadFails("{\"format\": 5, \"formatVersion\": 1}", "Not a character file");
    assertLoadFails("{\"format\": \"fallout2d20-character-builder\"}", "no valid format version");
    assertLoadFails("{\"format\": \"fallout2d20-character-builder\", \"formatVersion\": \"1\"}",
        "no valid format version");
    assertLoadFails("{\"format\": \"fallout2d20-character-builder\", \"formatVersion\": 0}",
        "no valid format version");
  }

  private void assertLoadFails(String json, String expectedMessage) throws IOException {
    Path file = dir.resolve("invalid.json");
    Files.writeString(file, json);
    SaveFormatException error = assertThrows(SaveFormatException.class, () -> store.load(file));
    assertTrue(error.getMessage().contains(expectedMessage), error.getMessage());
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
