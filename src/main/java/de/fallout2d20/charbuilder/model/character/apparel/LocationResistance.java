package de.fallout2d20.charbuilder.model.character.apparel;

import de.fallout2d20.charbuilder.model.character.derived.Resistance;
import org.jspecify.annotations.NonNull;

/** The damage resistances of one hit location: worn apparel plus innate resistances. */
public record LocationResistance(
    @NonNull HitLocation location,
    @NonNull Resistance physical,
    @NonNull Resistance energy,
    @NonNull Resistance radiation,
    @NonNull Resistance poison) {}
