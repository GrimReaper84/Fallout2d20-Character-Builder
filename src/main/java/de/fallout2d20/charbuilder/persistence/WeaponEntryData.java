package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Save-file form of a carried weapon: a catalog reference or a custom weapon, the quantity, a note and
 * whether it is built in ({@code null} = no), the installed mods per slot name ({@code null} = none; added
 * 2026-10-17 as an optional field, format version stays 1; slot names this version does not know are
 * skipped on load).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WeaponEntryData(
    @Nullable CatalogRefData ref,
    @Nullable CustomWeaponData custom,
    int quantity,
    @Nullable String notes,
    @Nullable Boolean builtIn,
    @Nullable Map<String, CatalogRefData> mods) {}
