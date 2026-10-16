package de.fallout2d20.charbuilder.model.character.creation;

/**
 * The part of the character a hint belongs to, so a view can show it next to the values it is about:
 * name and origin choice, the attributes/skills/traits, the perks, the apparel or the gear.
 */
public enum HintArea {
  /** Name, origin choice and the book of the origin. */
  HEADER,
  /** Origin options (extra Tag skill, purpose), traits, S.P.E.C.I.A.L. and skills. */
  STATS,
  /** Perks. */
  PERKS,
  /** Worn and carried apparel. */
  ARMOR,
  /** Equipment pack and other gear. */
  GEAR
}
