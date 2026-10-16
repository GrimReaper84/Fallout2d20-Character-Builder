package de.fallout2d20.charbuilder.export.sheet;

import org.jspecify.annotations.NonNull;

/** One line of the perks &amp; traits table: name (with choices), rank (empty for traits) and short effect. */
public record PerkRow(@NonNull String name, @NonNull String rank, @NonNull String effect) {}
