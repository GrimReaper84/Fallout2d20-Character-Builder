package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Save-file form of a custom apparel item, with the name of its chosen mod table ({@code null} = none; added
 * 2026-10-22 as an optional field, format version stays 1; a table name this version does not know, or one that
 * does not fit the kind, is read as none).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomApparelData(
    @NonNull String name,
    @NonNull ApparelKind kind,
    @NonNull List<HitLocation> locations,
    int physical,
    int energy,
    int radiation,
    boolean radiationImmune,
    int carryWeight,
    @Nullable String modTable) {}
