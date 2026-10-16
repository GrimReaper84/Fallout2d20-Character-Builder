package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * Save-file form of a carried weapon: a catalog reference or a custom weapon, the quantity, a note and
 * whether it is built in ({@code null} = no).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WeaponEntryData(
    @Nullable CatalogRefData ref,
    @Nullable CustomWeaponData custom,
    int quantity,
    @Nullable String notes,
    @Nullable Boolean builtIn) {}
