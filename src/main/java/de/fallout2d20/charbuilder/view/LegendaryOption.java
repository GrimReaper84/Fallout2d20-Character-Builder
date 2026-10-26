package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.Source;
import org.jspecify.annotations.NonNull;

/**
 * What {@link LegendarySection} shows of a weapon or armor property.
 *
 * @param name name as printed
 * @param rule the rule in own words
 * @param source book and page of the description
 */
record LegendaryOption(@NonNull String name, @NonNull String rule, @NonNull Source source) {}
