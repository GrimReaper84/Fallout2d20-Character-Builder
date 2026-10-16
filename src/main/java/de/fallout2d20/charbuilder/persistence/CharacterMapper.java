package de.fallout2d20.charbuilder.persistence;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.equipment.ArmAttachment;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentPackData;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.LegacyBaseline;
import de.fallout2d20.charbuilder.model.character.LegacyImport;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.SkillState;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import de.fallout2d20.charbuilder.model.character.Vitals;
import de.fallout2d20.charbuilder.model.character.apparel.CustomApparel;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.equipment.ArmSelection;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.CustomWeapon;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Converts between the character model and the v3 save-file records. */
final class CharacterMapper {

  static final int SCHEMA_VERSION = 3;

  private CharacterMapper() {}

  static CharacterData toData(Character character) {
    Map<String, Integer> allocation = new LinkedHashMap<>();
    for (Attribute attribute : Attribute.values()) {
      allocation.put(attribute.id(), character.attributeAllocation(attribute));
    }
    Map<String, SkillData> skills = new LinkedHashMap<>();
    for (Skill skill : Skill.values()) {
      SkillState state = character.skillState(skill);
      skills.put(skill.id(), new SkillData(state.tagged(), state.creationRanks(), state.advancementRanks(),
          state.stagedRanks()));
    }
    List<TraitData> traits = character.getTraits().stream()
        .map(t -> new TraitData(toData(t.trait()), t.imported(), attributeIds(t.attributeChoices())))
        .toList();
    List<PerkData> perks = character.getPerks().stream()
        .map(p -> new PerkData(toData(p.perk()), p.rank(), p.importedRanks(),
            attributeIds(p.attributeChoices()), p.skillChoices().stream().map(Skill::id).toList(),
            p.stagedRanks()))
        .toList();
    CatalogRef origin = character.getOrigin();
    Vitals vitals = character.getVitals();
    LegacyImport legacyImport = character.getLegacyImport();
    return new CharacterData(
        SCHEMA_VERSION,
        character.getName(),
        origin == null ? null : toData(origin),
        toData(character.getOriginChoices()),
        character.getLevel(),
        character.getXp(),
        character.isCreationComplete(),
        startingLevel(character),
        character.isCreationComplete()
            ? character.getStartingTagSkills().stream().sorted().map(Skill::id).toList() : null,
        character.getEnabledBooks().stream().map(Book::name).toList(),
        allocation,
        skills,
        traits,
        perks,
        new ProgressionData(character.getProgression().pendingSkillRanks(),
            character.getProgression().pendingPerks()),
        new VitalsData(vitals.currentHp(), vitals.rads(), vitals.radiationPoints()),
        toData(character.getStartingEquipment()),
        character.getInventoryNotes().isBlank() ? null : character.getInventoryNotes(),
        character.getWeapons().isEmpty() ? null : character.getWeapons().stream().map(CharacterMapper::toData).toList(),
        character.getAmmunition().isEmpty() ? null : new LinkedHashMap<>(character.getAmmunition()),
        character.getApparel().isEmpty() ? null : character.getApparel().stream().map(CharacterMapper::toData).toList(),
        toData(character.getLegacyBaseline()),
        legacyImport == null ? null : new LegacyImportData(legacyImport.sourceSchemaVersion(),
            legacyImport.importedOn(), legacyImport.payload()));
  }

  static Character toModel(CharacterData data) throws SaveFormatException {
    Map<Attribute, Integer> allocation = new EnumMap<>(Attribute.class);
    for (Attribute attribute : Attribute.values()) {
      Integer value = data.attributeAllocation().get(attribute.id());
      if (value == null) {
        throw new SaveFormatException("Missing attribute allocation: " + attribute.id());
      }
      allocation.put(attribute, value);
    }
    Map<Skill, SkillState> skills = new EnumMap<>(Skill.class);
    for (Map.Entry<String, SkillData> entry : data.skills().entrySet()) {
      Skill skill = skill(entry.getKey());
      SkillData skillData = entry.getValue();
      skills.put(skill, new SkillState(skillData.tagged(), skillData.creationRanks(), skillData.advancementRanks(),
          skillData.stagedRanks()));
    }
    List<TraitSelection> traits = new ArrayList<>();
    for (TraitData trait : orEmpty(data.traits())) {
      traits.add(new TraitSelection(toModel(trait.ref()), trait.imported(), attributes(trait.attributeChoices())));
    }
    List<PerkSelection> perks = new ArrayList<>();
    for (PerkData perk : orEmpty(data.perks())) {
      List<Skill> skillChoices = new ArrayList<>();
      for (String id : orEmpty(perk.skillChoices())) {
        skillChoices.add(skill(id));
      }
      perks.add(new PerkSelection(toModel(perk.ref()), perk.rank(), perk.importedRanks(),
          attributes(perk.attributeChoices()), skillChoices, perk.stagedRanks()));
    }
    ProgressionData progression = data.progression();
    VitalsData vitals = data.vitals();
    CatalogRefData origin = data.origin();
    LegacyImportData legacyImport = data.legacyImport();
    String name = data.name();
    StartingEquipment startingEquipment = toModel(data.startingEquipment());
    String inventoryNotes = data.inventoryNotes();
    try {
      return new Character(
          name == null ? "" : name,
          origin == null ? null : toModel(origin),
          toModel(data.originChoices()),
          data.level(),
          data.xp(),
          data.creationComplete(),
          books(data.books()),
          allocation,
          skills,
          traits,
          perks,
          progression == null ? Progression.NONE
              : new Progression(progression.pendingSkillRanks(), progression.pendingPerks()),
          vitals == null ? Vitals.FULL
              : new Vitals(vitals.currentHp(), vitals.rads(), vitals.radiationPoints()),
          toModel(data.legacyBaseline()),
          legacyImport == null ? null : new LegacyImport(legacyImport.sourceSchemaVersion(),
              legacyImport.importedOn(), legacyImport.payload()),
          startingEquipment,
          inventoryNotes == null ? "" : inventoryNotes,
          data.startingLevel(),
          startingTagSkills(data.startingTagSkills()),
          weapons(data.weapons()),
          data.ammunition() == null ? Map.of() : data.ammunition(),
          apparel(data.apparel()));
    } catch (IllegalArgumentException e) {
      throw new SaveFormatException("Invalid character data: " + e.getMessage(), e);
    }
  }

  /** Stored only for a complete character whose starting level is known (CRB p. 81 starting caps). */
  private static @Nullable Integer startingLevel(@NonNull Character character) {
    OptionalInt level = character.getStartingLevel();
    return character.isCreationComplete() && level.isPresent() ? level.getAsInt() : null;
  }

  private static @NonNull WeaponEntryData toData(@NonNull CarriedWeapon weapon) {
    CatalogRef ref = weapon.weapon();
    CustomWeapon custom = weapon.custom();
    CustomWeaponData customData = null;
    if (custom != null) {
      Skill skill = custom.skill();
      customData = new CustomWeaponData(custom.name(), skill == null ? null : skill.id(), custom.damage(),
          blankToNull(custom.effects()), blankToNull(custom.damageType()), custom.fireRate(), custom.range(),
          blankToNull(custom.qualities()), custom.ammunition());
    }
    return new WeaponEntryData(ref == null ? null : toData(ref), customData, weapon.quantity(),
        blankToNull(weapon.notes()));
  }

  private static @NonNull List<CarriedWeapon> weapons(@Nullable List<WeaponEntryData> entries)
      throws SaveFormatException {
    List<CarriedWeapon> weapons = new ArrayList<>();
    for (WeaponEntryData entry : orEmpty(entries)) {
      CatalogRefData ref = entry.ref();
      CustomWeaponData custom = entry.custom();
      String notes = entry.notes();
      weapons.add(new CarriedWeapon(ref == null ? null : toModel(ref), custom == null ? null : toModel(custom),
          entry.quantity(), notes == null ? "" : notes));
    }
    return weapons;
  }

  private static @NonNull CustomWeapon toModel(@NonNull CustomWeaponData data) throws SaveFormatException {
    String skill = data.skill();
    return new CustomWeapon(data.name(), skill == null ? null : skill(skill), data.damage(),
        nullToEmpty(data.effects()), nullToEmpty(data.damageType()), data.fireRate(), data.range(),
        nullToEmpty(data.qualities()), data.ammunition());
  }

  private static @NonNull WornApparelData toData(@NonNull WornApparel item) {
    CatalogRef ref = item.apparel();
    CustomApparel custom = item.custom();
    CustomApparelData customData = custom == null ? null : new CustomApparelData(custom.name(), custom.kind(),
        custom.locations().stream().sorted().toList(), custom.physical(), custom.energy(), custom.radiation(),
        custom.radiationImmune(), custom.carryWeight());
    return new WornApparelData(ref == null ? null : toData(ref), customData, item.location(), blankToNull(item.notes()));
  }

  private static @NonNull List<WornApparel> apparel(@Nullable List<WornApparelData> entries) throws SaveFormatException {
    List<WornApparel> apparel = new ArrayList<>();
    for (WornApparelData entry : orEmpty(entries)) {
      CatalogRefData ref = entry.ref();
      CustomApparelData custom = entry.custom();
      String notes = entry.notes();
      apparel.add(new WornApparel(ref == null ? null : toModel(ref), custom == null ? null : toModel(custom),
          entry.location(), notes == null ? "" : notes));
    }
    return apparel;
  }

  private static @NonNull CustomApparel toModel(@NonNull CustomApparelData data) {
    return new CustomApparel(data.name(), data.kind(), Set.copyOf(data.locations()), data.physical(), data.energy(),
        data.radiation(), data.radiationImmune(), data.carryWeight());
  }

  private static @Nullable String blankToNull(@NonNull String text) {
    return text.isBlank() ? null : text;
  }

  private static @NonNull String nullToEmpty(@Nullable String text) {
    return text == null ? "" : text;
  }

  /** Stored Tag skills of a complete character; {@code null} if the file has none. */
  private static @Nullable Set<Skill> startingTagSkills(@Nullable List<String> ids) throws SaveFormatException {
    if (ids == null) {
      return null;
    }
    Set<Skill> skills = EnumSet.noneOf(Skill.class);
    for (String id : ids) {
      skills.add(skill(id));
    }
    return skills;
  }

  /** Enabled books; a file without the field (written before it existed) enables all books. */
  private static @NonNull Set<Book> books(@Nullable List<String> codes) throws SaveFormatException {
    if (codes == null) {
      return EnumSet.allOf(Book.class);
    }
    Set<Book> books = EnumSet.noneOf(Book.class);
    for (String code : codes) {
      try {
        books.add(Book.valueOf(code));
      } catch (IllegalArgumentException e) {
        throw new SaveFormatException("Unknown book: " + code, e);
      }
    }
    return books;
  }

  /** Omitted from the file when nothing is chosen. */
  private static @Nullable OriginChoicesData toData(@NonNull OriginChoices choices) {
    if (choices.equals(OriginChoices.NONE)) {
      return null;
    }
    Skill tagSkill = choices.tagSkill();
    return new OriginChoicesData(tagSkill == null ? null : tagSkill.id(), choices.purpose());
  }

  private static @NonNull OriginChoices toModel(@Nullable OriginChoicesData data)
      throws SaveFormatException {
    if (data == null) {
      return OriginChoices.NONE;
    }
    String tagSkill = data.tagSkill();
    return new OriginChoices(tagSkill == null ? null : skill(tagSkill), data.purpose());
  }

  /** Omitted from the file when nothing is chosen. */
  private static @Nullable StartingEquipmentData toData(@NonNull StartingEquipment equipment) {
    if (equipment.equals(StartingEquipment.NONE)) {
      return null;
    }
    CatalogRef pack = equipment.pack();
    Map<String, String> picks = new LinkedHashMap<>();
    equipment.tagSkillItems().forEach((skill, option) -> picks.put(skill.id(), option));
    List<ArmData> arms = equipment.arms().stream()
        .map(arm -> {
          ArmAttachment attachment = arm.attachment();
          return new ArmData(attachment == null ? null : attachment.name(), arm.weapon());
        })
        .toList();
    return new StartingEquipmentData(pack == null ? null : toData(pack), equipment.choices(),
        picks.isEmpty() ? null : picks, equipment.trinket(), equipment.freeArms(), arms.isEmpty() ? null : arms);
  }

  /**
   * The starting equipment of a file; unknown skill ids and attachment names make the file unreadable.
   * Picks and arms that no longer fit the catalog are opened again by the character (see
   * {@link StartingEquipment#fittedToCatalog(boolean)}).
   */
  private static @NonNull StartingEquipment toModel(@Nullable StartingEquipmentData data)
      throws SaveFormatException {
    if (data == null) {
      return StartingEquipment.NONE;
    }
    Map<Skill, String> picks = new EnumMap<>(Skill.class);
    Map<String, @Nullable String> tagSkillItems = data.tagSkillItems();
    if (tagSkillItems != null) {
      for (Map.Entry<String, @Nullable String> entry : tagSkillItems.entrySet()) {
        Skill skill = skill(entry.getKey());
        String option = entry.getValue();
        if (option != null) {
          picks.put(skill, option);
        }
      }
    }
    List<ArmSelection> arms = new ArrayList<>();
    for (@Nullable ArmData arm : orEmpty(data.arms())) {
      if (arm == null) {
        throw new SaveFormatException("Arm without data");
      }
      arms.add(toModel(arm));
    }
    CatalogRefData pack = data.pack();
    List<@Nullable String> choices = data.choices();
    // Any other number of arms than three is dropped; the character fits the arms to the pack.
    return new StartingEquipment(pack == null ? null : toModel(pack), choices == null ? List.of() : choices,
        picks, data.trinket(), data.freeArms(), arms.size() == EquipmentPackData.ARM_COUNT ? arms : List.of());
  }

  private static @NonNull ArmSelection toModel(@NonNull ArmData data) throws SaveFormatException {
    String attachment = data.attachment();
    try {
      return new ArmSelection(attachment == null ? null : ArmAttachment.valueOf(attachment), data.weapon());
    } catch (IllegalArgumentException e) {
      throw new SaveFormatException("Invalid arm: " + attachment + " / " + data.weapon(), e);
    }
  }

  private static CatalogRefData toData(CatalogRef ref) {
    return new CatalogRefData(ref.id(), ref.legacyName());
  }

  private static CatalogRef toModel(CatalogRefData data) throws SaveFormatException {
    if (data.id() == null || data.id().isBlank()) {
      throw new SaveFormatException("Catalog reference without id");
    }
    return new CatalogRef(data.id(), data.legacyName());
  }

  private static @Nullable LegacyBaselineData toData(@Nullable LegacyBaseline baseline) {
    if (baseline == null) {
      return null;
    }
    Map<String, Integer> attributes = new LinkedHashMap<>();
    for (Attribute attribute : Attribute.values()) {
      attributes.put(attribute.id(), baseline.attribute(attribute));
    }
    Map<String, Integer> skills = new LinkedHashMap<>();
    for (Skill skill : Skill.values()) {
      skills.put(skill.id(), baseline.skillRank(skill));
    }
    return new LegacyBaselineData(attributes, skills);
  }

  private static @Nullable LegacyBaseline toModel(@Nullable LegacyBaselineData data) throws SaveFormatException {
    if (data == null) {
      return null;
    }
    Map<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
    for (Attribute attribute : Attribute.values()) {
      Integer value = data.attributes().get(attribute.id());
      if (value == null) {
        throw new SaveFormatException("Legacy baseline misses attribute " + attribute.id());
      }
      attributes.put(attribute, value);
    }
    Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
    for (Skill skill : Skill.values()) {
      Integer rank = data.skills().get(skill.id());
      if (rank == null) {
        throw new SaveFormatException("Legacy baseline misses skill " + skill.id());
      }
      skills.put(skill, rank);
    }
    return new LegacyBaseline(attributes, skills);
  }

  private static List<String> attributeIds(List<Attribute> attributes) {
    return attributes.stream().map(Attribute::id).toList();
  }

  private static List<Attribute> attributes(@Nullable List<String> ids) throws SaveFormatException {
    List<Attribute> attributes = new ArrayList<>();
    for (String id : orEmpty(ids)) {
      attributes.add(Attribute.fromId(id)
          .orElseThrow(() -> new SaveFormatException("Unknown attribute id: " + id)));
    }
    return attributes;
  }

  private static Skill skill(String id) throws SaveFormatException {
    return Skill.fromId(id).orElseThrow(() -> new SaveFormatException("Unknown skill id: " + id));
  }

  private static <T> List<T> orEmpty(@Nullable List<T> list) {
    return list == null ? List.of() : list;
  }
}
