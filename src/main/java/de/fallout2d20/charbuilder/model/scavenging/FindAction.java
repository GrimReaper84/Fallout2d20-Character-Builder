package de.fallout2d20.charbuilder.model.scavenging;

/** What can change a find's result; which one applies depends on the find ({@link ScavengeSearch#findAction}). */
public enum FindAction {
  /** 1 Luck: the roll moves by up to the location's level (CRB p. 200). */
  SHIFT_WITH_LUCK,
  /** 1 Luck: the LCK score is added to a junk count (CRB p. 208). */
  ADD_LUCK_TO_COUNT,
  /** No Luck point: a junk table roll moves by up to the LCK score (WG p. 120). */
  ADJUST_JUNK
}
