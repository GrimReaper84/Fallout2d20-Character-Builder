package de.fallout2d20.charbuilder.view;

import org.jspecify.annotations.NonNull;

/**
 * What the character view calls back after an edit.
 *
 * @param onEdited after an edit that needs no rebuild (typed text such as the name)
 * @param onChanged after an edit that changes other values; the caller rebuilds the view
 * @param onComplete by the "Create Character" button; the caller confirms open hints and ends creation
 */
record ViewCallbacks(@NonNull Runnable onEdited, @NonNull Runnable onChanged, @NonNull Runnable onComplete) {}
