package de.fallout2d20.charbuilder.export.sheet;

import org.jspecify.annotations.NonNull;

/** One line of the gear table: item (with quantity) and its weight in lbs. for the whole stack. */
public record GearRow(@NonNull String item, @NonNull String pounds) {}
