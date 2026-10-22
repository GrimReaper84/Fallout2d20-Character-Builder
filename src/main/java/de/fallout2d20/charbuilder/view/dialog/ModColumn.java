package de.fallout2d20.charbuilder.view.dialog;

import java.util.function.Function;
import org.jspecify.annotations.NonNull;

/**
 * A text column of a mod dialog.
 *
 * @param title the column header
 * @param width preferred width in pixels
 * @param text the cell text of a mod
 */
record ModColumn<T>(@NonNull String title, double width, @NonNull Function<T, String> text) {}
