package de.fallout2d20.charbuilder.model.character.creation;

import org.jspecify.annotations.NonNull;

/**
 * A rule a character in creation does not (yet) follow. Problems are hints, not errors: editing and
 * saving stay possible (project rule: soft warnings, the GM may override).
 *
 * @param topic what the problem is about, e.g. {@code Origin}, {@code Strength}, {@code Sneak}
 * @param message what is wrong, in a short sentence
 */
public record CreationProblem(@NonNull String topic, @NonNull String message) {}
