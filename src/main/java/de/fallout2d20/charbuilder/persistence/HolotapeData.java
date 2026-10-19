package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Save-file form of a holotape (added 2026-10-19 as an optional field, format version stays 1). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HolotapeData(@NonNull String name, @Nullable String label) {}
