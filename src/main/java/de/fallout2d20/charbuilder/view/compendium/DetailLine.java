package de.fallout2d20.charbuilder.view.compendium;

import org.jspecify.annotations.NonNull;

/** One line of a compendium entry's details (H2): a {@link DetailValue} or {@link DetailLinks} to other entries. */
sealed interface DetailLine permits DetailValue, DetailLinks {

  @NonNull String label();
}
