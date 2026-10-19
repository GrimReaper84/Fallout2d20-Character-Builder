package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Save-file form of a carried weapon: a catalog reference or a custom weapon, the quantity, a note and
 * whether it is built in ({@code null} = no), the installed mods per slot name ({@code null} = none; added
 * 2026-10-17 as an optional field, format version stays 1; slot names this version does not know are
 * skipped on load), and the part of the starting equipment it comes from ({@code PACK}, {@code TAG_SKILL};
 * {@code null} = added by hand; added 2026-10-19, unknown values are read as added by hand).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WeaponEntryData(
    @Nullable CatalogRefData ref,
    @Nullable CustomWeaponData custom,
    int quantity,
    @Nullable String notes,
    @Nullable Boolean builtIn,
    @Nullable Map<String, CatalogRefData> mods,
    @Nullable String startingSource) {}
