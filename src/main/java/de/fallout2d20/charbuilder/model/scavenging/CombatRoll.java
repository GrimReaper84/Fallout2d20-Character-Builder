package de.fallout2d20.charbuilder.model.scavenging;

/**
 * The result of rolling Combat Dice.
 *
 * @param total the sum of the dice
 * @param effects how many dice showed an Effect
 */
public record CombatRoll(int total, int effects) {}
