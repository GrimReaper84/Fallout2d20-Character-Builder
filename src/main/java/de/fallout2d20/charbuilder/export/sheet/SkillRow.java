package de.fallout2d20.charbuilder.export.sheet;

import org.jspecify.annotations.NonNull;

/** One line of the skills table: name, abbreviated governing attribute, Tag box and rank. */
public record SkillRow(@NonNull String name, @NonNull String attribute, boolean tagged, int rank) {}
