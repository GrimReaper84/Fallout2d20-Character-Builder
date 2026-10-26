package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Save-file form of an apparel entry: a catalog reference or a custom item, the chosen limb, a note and
 * whether it is only carried ({@code null} = worn), the part of the starting equipment it comes from (see
 * {@link WeaponEntryData}), and the installed mods per slot name ({@code null} = none; added 2026-10-22 as an
 * optional field, format version stays 1; slot names this version does not know are skipped on load), and the id of
 * its legendary property ({@code null} = none; added 2026-10-23, see {@link WeaponEntryData}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WornApparelData(
    @Nullable CatalogRefData ref,
    @Nullable CustomApparelData custom,
    @Nullable HitLocation location,
    @Nullable String notes,
    @Nullable Boolean carried,
    @Nullable String startingSource,
    @Nullable Map<String, CatalogRefData> mods,
    @Nullable String legendary) {}
