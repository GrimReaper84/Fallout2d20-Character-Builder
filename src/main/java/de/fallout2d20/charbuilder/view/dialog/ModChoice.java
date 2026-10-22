package de.fallout2d20.charbuilder.view.dialog;

import org.jspecify.annotations.Nullable;

/**
 * A row of a mod dialog and its result: a mod, or {@code null} for "No mod".
 *
 * @param <T> the mod type (weapon or apparel mod)
 */
public record ModChoice<T>(@Nullable T mod) {}
