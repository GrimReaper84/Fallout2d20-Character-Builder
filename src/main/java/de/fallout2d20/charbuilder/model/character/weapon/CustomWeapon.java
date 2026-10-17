package de.fallout2d20.charbuilder.model.character.weapon;

import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A weapon outside the catalog: homebrew, a weapon of a book not catalogued yet, or a legacy weapon whose
 * name is unknown. Effects, damage type and qualities are free text.
 *
 * @param name the weapon's name
 * @param skill the skill used to attack; {@code null} if unknown (imported), then no attack value is shown
 * @param damage damage rating in Combat Dice
 * @param effects damage effects as text, e.g. {@code Piercing 1}; empty for none
 * @param damageType damage type as text, e.g. {@code Energy}
 * @param fireRate fire rate 0–6 (CRB p. 89); {@code null} for none
 * @param range ideal range; {@code null} for melee weapons
 * @param qualities qualities as text; empty for none
 * @param ammunition id of the catalog ammunition it uses; {@code null} for none or unknown
 * @param modSlots the mod slots the player gave the weapon (user decision 2026-10-17); any catalog mod of
 *     these slots can be installed; empty for none
 */
public record CustomWeapon(
    @NonNull String name,
    @Nullable Skill skill,
    int damage,
    @NonNull String effects,
    @NonNull String damageType,
    @Nullable Integer fireRate,
    @Nullable WeaponRange range,
    @NonNull String qualities,
    @Nullable String ammunition,
    @NonNull Set<ModSlot> modSlots) {

  /** CRB p. 89: fire rate is a number from 0 to 6. */
  public static final int MAX_FIRE_RATE = 6;

  public CustomWeapon {
    if (name.isBlank() || damage < 0 || (fireRate != null && (fireRate < 0 || fireRate > MAX_FIRE_RATE))) {
      throw new IllegalArgumentException("A custom weapon needs a name and non-negative values");
    }
    name = name.strip();
    effects = effects.strip();
    damageType = damageType.strip();
    qualities = qualities.strip();
    ammunition = ammunition == null || ammunition.isBlank() ? null : ammunition;
    modSlots = modSlots.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(modSlots));
  }

  /** A custom weapon without mod slots. */
  public CustomWeapon(@NonNull String name, @Nullable Skill skill, int damage, @NonNull String effects,
      @NonNull String damageType, @Nullable Integer fireRate, @Nullable WeaponRange range, @NonNull String qualities,
      @Nullable String ammunition) {
    this(name, skill, damage, effects, damageType, fireRate, range, qualities, ammunition, Set.of());
  }

  /** The same weapon attacked with another skill, e.g. after importing it without one. */
  public @NonNull CustomWeapon withSkill(@Nullable Skill newSkill) {
    return new CustomWeapon(name, newSkill, damage, effects, damageType, fireRate, range, qualities, ammunition,
        modSlots);
  }

  /** The same weapon with other mod slots. */
  public @NonNull CustomWeapon withModSlots(@NonNull Set<ModSlot> newSlots) {
    return new CustomWeapon(name, skill, damage, effects, damageType, fireRate, range, qualities, ammunition, newSlots);
  }
}
