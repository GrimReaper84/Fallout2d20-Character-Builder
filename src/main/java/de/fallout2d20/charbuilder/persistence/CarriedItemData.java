package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * Save-file form of a carried item: a catalog reference, a custom item or a holotape, the magazine issue, quantity,
 * note and the part of the starting equipment it comes from (see {@link WeaponEntryData}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CarriedItemData(
    @Nullable CatalogRefData ref,
    @Nullable CustomItemData custom,
    @Nullable HolotapeData holotape,
    @Nullable String issue,
    int quantity,
    @Nullable String notes,
    @Nullable String startingSource) {}
