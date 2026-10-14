package de.fallout2d20.charbuilder.persistence;

import java.util.Map;

/** Save-file form of the legacy baseline, keyed by attribute and skill ids. */
public record LegacyBaselineData(Map<String, Integer> attributes, Map<String, Integer> skills) {}
