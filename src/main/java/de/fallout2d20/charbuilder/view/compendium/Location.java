package de.fallout2d20.charbuilder.view.compendium;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A place in the GM compendium (H2) to go back to after following a link.
 *
 * @param id the selected entry of a catalog section; null for none
 */
record Location(@NonNull Section section, @Nullable String id) {}
