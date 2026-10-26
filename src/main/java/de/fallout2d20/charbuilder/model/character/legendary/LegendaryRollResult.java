package de.fallout2d20.charbuilder.model.character.legendary;

import org.jspecify.annotations.NonNull;

/**
 * A rolled legendary property and how it was rolled, e.g. {@code d20 14: Table 2 · d20 19: no result · d20 7}.
 *
 * @param property the property rolled
 * @param description the d20 results in order, for the player
 */
public record LegendaryRollResult<T>(@NonNull T property, @NonNull String description) {}
