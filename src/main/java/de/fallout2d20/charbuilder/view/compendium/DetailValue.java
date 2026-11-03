package de.fallout2d20.charbuilder.view.compendium;

import org.jspecify.annotations.NonNull;

/** A labelled value of a compendium entry (H2), e.g. "Damage: 4 CD"; long values wrap. */
record DetailValue(@NonNull String label, @NonNull String text) implements DetailLine {}
