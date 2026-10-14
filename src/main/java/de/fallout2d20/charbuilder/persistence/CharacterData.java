package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Root of the save format {@code schemaVersion: 3}. Maps are keyed by attribute and skill ids;
 * all 7 attributes and 17 skills are always written.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CharacterData(
    @JsonProperty(required = true) int schemaVersion,
    @Nullable String name,
    @Nullable CatalogRefData origin,
    int level,
    long xp,
    boolean creationComplete,
    @JsonProperty(required = true) Map<String, Integer> attributeAllocation,
    @JsonProperty(required = true) Map<String, SkillData> skills,
    @Nullable List<TraitData> traits,
    @Nullable List<PerkData> perks,
    @Nullable ProgressionData progression,
    @Nullable VitalsData vitals,
    @Nullable LegacyBaselineData legacyBaseline,
    @Nullable LegacyImportData legacyImport) {}
