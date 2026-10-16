package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Root of the save format {@code schemaVersion: 3}. Maps are keyed by attribute and skill ids;
 * all 7 attributes and 17 skills are always written. {@code books} lists the enabled book codes
 * ({@code CRB}, {@code WOA}, {@code WG}); files written before it existed enable all books.
 * {@code startingLevel} is written only for a complete character whose starting level is known;
 * {@code startingTagSkills} (skill ids) only for a complete character.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CharacterData(
    @JsonProperty(required = true) int schemaVersion,
    @Nullable String name,
    @Nullable CatalogRefData origin,
    @Nullable OriginChoicesData originChoices,
    int level,
    long xp,
    boolean creationComplete,
    @Nullable Integer startingLevel,
    @Nullable List<String> startingTagSkills,
    @Nullable List<String> books,
    @JsonProperty(required = true) Map<String, Integer> attributeAllocation,
    @JsonProperty(required = true) Map<String, SkillData> skills,
    @Nullable List<TraitData> traits,
    @Nullable List<PerkData> perks,
    @Nullable ProgressionData progression,
    @Nullable VitalsData vitals,
    @Nullable StartingEquipmentData startingEquipment,
    @Nullable String inventoryNotes,
    @Nullable LegacyBaselineData legacyBaseline,
    @Nullable LegacyImportData legacyImport) {}
