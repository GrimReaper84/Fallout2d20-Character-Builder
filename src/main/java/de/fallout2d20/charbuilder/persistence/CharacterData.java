package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Root of the save format: {@code format} is always {@link CharacterMapper#FORMAT}, {@code formatVersion}
 * the structure version (raised only on incompatible changes), {@code createdWith} the app version that
 * created the character (absent for legacy imports and test-build files) and {@code savedWith} the app
 * version of the last save. Test-build files carry {@code schemaVersion: 3} instead (read as format 1,
 * see {@link CharacterStore}). Maps are keyed by attribute and skill ids;
 * all 7 attributes and 17 skills are always written. {@code books} lists the enabled book codes
 * ({@code CRB}, {@code WOA}, {@code WG}); files written before it existed enable all books.
 * {@code startingLevel} is written only for a complete character whose starting level is known;
 * {@code startingTagSkills} (skill ids) only for a complete character. {@code ammunition} maps ammunition
 * ids to shots. {@code items}, {@code caps} and {@code materials} hold the inventory (written only when not
 * empty or 0).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CharacterData(
    @Nullable String format,
    @Nullable Integer formatVersion,
    @Nullable String createdWith,
    @Nullable String savedWith,
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
    @Nullable List<WeaponEntryData> weapons,
    @Nullable Map<String, Integer> ammunition,
    @Nullable List<WornApparelData> apparel,
    @Nullable List<CarriedItemData> items,
    @Nullable Long caps,
    @Nullable MaterialsData materials,
    @Nullable Boolean packItemsAdded,
    @Nullable LegacyBaselineData legacyBaseline,
    @Nullable LegacyImportData legacyImport) {}
