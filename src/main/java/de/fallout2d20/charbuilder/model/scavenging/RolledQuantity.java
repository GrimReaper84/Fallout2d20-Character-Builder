package de.fallout2d20.charbuilder.model.scavenging;

import org.jspecify.annotations.NonNull;

/**
 * A rolled quantity formula.
 *
 * @param value the result
 * @param text how it came about, e.g. {@code 6+3 CD = 8}
 */
public record RolledQuantity(int value, @NonNull String text) {}
