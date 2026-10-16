package de.fallout2d20.charbuilder.export.sheet;

import org.jspecify.annotations.NonNull;

/**
 * One hit location box: name, d20 range and the total damage resistances (apparel plus innate). The HP
 * field of the printed sheet (for Power Armor pieces) is left for the pencil.
 */
public record HitLocationRow(
    @NonNull String name,
    @NonNull String dieRange,
    @NonNull String physical,
    @NonNull String energy,
    @NonNull String radiation) {}
