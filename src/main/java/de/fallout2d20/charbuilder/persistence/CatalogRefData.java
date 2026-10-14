package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/** Save-file form of a catalog reference. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CatalogRefData(String id, @Nullable String legacyName) {}
