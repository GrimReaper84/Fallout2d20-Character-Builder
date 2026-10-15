package de.fallout2d20.charbuilder.persistence;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.LegacyBaseline;
import de.fallout2d20.charbuilder.model.character.LegacyImport;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.SkillState;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import de.fallout2d20.charbuilder.model.character.Vitals;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
      skills.put(skill.id(), new SkillData(state.tagged(), state.creationRanks(), state.advancementRanks()));
    }
    List<TraitData> traits = character.getTraits().stream()
        .map(t -> new TraitData(toData(t.trait()), t.imported(), attributeIds(t.attributeChoices())))
        .toList();
    List<PerkData> perks = character.getPerks().stream()
        .map(p -> new PerkData(toData(p.perk()), p.rank(), p.importedRanks(),
            attributeIds(p.attributeChoices()), p.skillChoices().stream().map(Skill::id).toList()))
        .toList();
    CatalogRef origin = character.getOrigin();
    Vitals vitals = character.getVitals();
    LegacyImport legacyImport = character.getLegacyImport();
    return new CharacterData(
        SCHEMA_VERSION,
        character.getName(),
        origin == null ? null : toData(origin),
        character.getLevel(),
        character.getXp(),
        character.isCreationComplete(),
        character.getEnabledBooks().stream().map(Book::name).toList(),
        allocation,
        skills,
        traits,
        perks,
        new ProgressionData(character.getProgression().pendingSkillRanks(),
            character.getProgression().pendingPerks()),
        new VitalsData(vitals.currentHp(), vitals.rads(), vitals.radiationPoints()),
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
      skills.put(skill, new SkillState(skillData.tagged(), skillData.creationRanks(), skillData.advancementRanks()));
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
          attributes(perk.attributeChoices()), skillChoices));
    }
    ProgressionData progression = data.progression();
    VitalsData vitals = data.vitals();
    CatalogRefData origin = data.origin();
    LegacyImportData legacyImport = data.legacyImport();
    String name = data.name();
    try {
      return new Character(
          name == null ? "" : name,
          origin == null ? null : toModel(origin),
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
              legacyImport.importedOn(), legacyImport.payload()));
    } catch (IllegalArgumentException e) {
      throw new SaveFormatException("Invalid character data: " + e.getMessage(), e);
    }
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
