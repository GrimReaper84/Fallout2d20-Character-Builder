package de.fallout2d20.charbuilder.persistence;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import java.util.List;
import org.jspecify.annotations.NonNull;

/** Save-file form of a custom apparel item. */
public record CustomApparelData(
    @NonNull String name,
    @NonNull ApparelKind kind,
    @NonNull List<HitLocation> locations,
    int physical,
    int energy,
    int radiation,
    boolean radiationImmune,
    int carryWeight) {}
