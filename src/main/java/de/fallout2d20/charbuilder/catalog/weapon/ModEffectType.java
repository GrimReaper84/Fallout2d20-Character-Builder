package de.fallout2d20.charbuilder.catalog.weapon;

/** The kinds of change a weapon mod makes (CRB pp. 87–88, mod tables pp. 100–119). */
public enum ModEffectType {
  /** Damage rating raised or lowered by {@code value} CD. */
  DAMAGE,
  /** Damage rating replaced by {@code value} CD (e.g. the .38 Receiver). */
  SET_DAMAGE,
  /** Fire rate raised or lowered by {@code value}. */
  FIRE_RATE,
  /** Range moved by {@code value} steps (CRB p. 88). */
  RANGE,
  /** Quality {@code text} gained, e.g. Accurate. */
  GAIN_QUALITY,
  /** Quality {@code text} removed, e.g. Inaccurate. */
  REMOVE_QUALITY,
  /** Damage effect {@code text} gained, e.g. Vicious or Piercing 1. */
  GAIN_EFFECT,
  /** Damage effect {@code text} removed. */
  REMOVE_EFFECT,
  /** Damage type changed to the {@link DamageType} named by {@code text}. */
  SET_DAMAGE_TYPE,
  /** Ammunition changed to the ammunition id {@code text}. */
  SET_AMMUNITION,
  /** The weapon may take a mod in the {@link ModSlot} named by {@code text} (Bracketed barrels: Muzzle). */
  ALLOWS_SLOT,
  /** A rule that changes no value, as short text in own words. */
  SPECIAL
}
