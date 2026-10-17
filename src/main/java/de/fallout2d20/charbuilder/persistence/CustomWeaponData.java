package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Save-file form of a custom weapon; the skill as skill id, the names of the mod slots the player chose
 * ({@code null} = none; optional field added 2026-10-17; unknown names are skipped on load).
 */
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
    @Nullable String ammunition,
    @Nullable List<String> modSlots) {}
