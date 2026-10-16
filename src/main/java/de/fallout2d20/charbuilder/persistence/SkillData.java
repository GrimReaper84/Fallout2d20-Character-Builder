package de.fallout2d20.charbuilder.persistence;

/** Save-file form of a skill state; {@code stagedRanks} is absent (0) in files written before it existed. */
public record SkillData(boolean tagged, int creationRanks, int advancementRanks, int stagedRanks) {}
