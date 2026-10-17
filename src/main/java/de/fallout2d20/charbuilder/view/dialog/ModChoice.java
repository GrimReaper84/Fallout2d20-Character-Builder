package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import org.jspecify.annotations.Nullable;

/** A row of the mod dialog and its result: a mod, or {@code null} for "No mod". */
public record ModChoice(@Nullable WeaponModData mod) {}
