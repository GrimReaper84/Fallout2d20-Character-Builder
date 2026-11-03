package de.fallout2d20.charbuilder.view.compendium;

import org.jspecify.annotations.NonNull;

/**
 * A link from one compendium entry to another (H2), e.g. from a weapon to its ammunition.
 *
 * @param section the section of the target
 * @param id the target's {@link CompendiumEntry#id()}
 * @param name the link text
 */
record Reference(@NonNull Section section, @NonNull String id, @NonNull String name) {}
