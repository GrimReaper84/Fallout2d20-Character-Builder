package de.fallout2d20.charbuilder.persistence;

/** Save-file form of the junk and material counters. */
public record MaterialsData(int junk, int common, int uncommon, int rare) {}
