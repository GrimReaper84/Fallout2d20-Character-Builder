package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Save-file form of a custom item. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomItemData(@NonNull String name, int weight, @Nullable String effects) {}
