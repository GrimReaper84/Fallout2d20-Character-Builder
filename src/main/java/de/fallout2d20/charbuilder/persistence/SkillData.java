package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Save-file form of a skill state; {@code stagedRanks} is absent (0) in files written before it existed,
 * {@code gmRanks} (GM correction, D2) is written only when not 0.
 */
public record SkillData(boolean tagged, int creationRanks, int advancementRanks, int stagedRanks,
    @JsonInclude(JsonInclude.Include.NON_DEFAULT) int gmRanks) {}
