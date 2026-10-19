package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import org.jspecify.annotations.Nullable;

/**
 * Save-file form of an apparel entry: a catalog reference or a custom item, the chosen limb, a note and
 * whether it is only carried ({@code null} = worn), and the part of the starting equipment it comes from (see
 * {@link WeaponEntryData}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WornApparelData(
    @Nullable CatalogRefData ref,
    @Nullable CustomApparelData custom,
    @Nullable HitLocation location,
    @Nullable String notes,
    @Nullable Boolean carried,
    @Nullable String startingSource) {}
