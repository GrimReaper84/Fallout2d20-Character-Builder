package de.fallout2d20.charbuilder.model.scavenging;

/** How a roll on the junk category is resolved. */
public enum JunkMode {
  /** CRB p. 208: 2d20 is the number of junk items. */
  COUNT,
  /** WG p. 120: a d20 selects a junk table, a d20 on it gives one item. */
  DETAILED
}
