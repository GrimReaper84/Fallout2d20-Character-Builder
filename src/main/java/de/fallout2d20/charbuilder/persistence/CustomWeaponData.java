package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Save-file form of a custom weapon; the skill as skill id. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomWeaponData(
    @NonNull String name,
    @Nullable String skill,
    int damage,
    @Nullable String effects,
    @Nullable String damageType,
    @Nullable Integer fireRate,
    @Nullable WeaponRange range,
    @Nullable String qualities,
    @Nullable String ammunition) {}
