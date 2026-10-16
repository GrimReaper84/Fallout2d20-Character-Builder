package de.fallout2d20.charbuilder.export.sheet;

import org.jspecify.annotations.NonNull;

/** One line of the weapons table, every cell as printed text; {@code tagged} fills the Tag box. */
public record WeaponRow(
    @NonNull String name,
    @NonNull String skill,
    @NonNull String targetNumber,
    boolean tagged,
    @NonNull String damage,
    @NonNull String effects,
    @NonNull String type,
    @NonNull String rate,
    @NonNull String range,
    @NonNull String qualities,
    @NonNull String ammunition,
    @NonNull String weight) {}
