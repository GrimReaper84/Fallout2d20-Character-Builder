package de.fallout2d20.charbuilder.persistence;

import org.jspecify.annotations.Nullable;

/** Save-file form of the session vitals; {@code currentHp} null means "at maximum". */
public record VitalsData(@Nullable Integer currentHp, int rads, int radiationPoints) {}
