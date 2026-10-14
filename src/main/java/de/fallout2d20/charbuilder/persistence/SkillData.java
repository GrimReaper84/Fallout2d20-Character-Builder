package de.fallout2d20.charbuilder.persistence;

/** Save-file form of a skill state. */
public record SkillData(boolean tagged, int creationRanks, int advancementRanks) {}
