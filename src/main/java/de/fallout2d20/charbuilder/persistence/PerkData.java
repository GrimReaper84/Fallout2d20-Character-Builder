package de.fallout2d20.charbuilder.persistence;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** Save-file form of a perk selection; choices as attribute and skill ids. */
public record PerkData(
    CatalogRefData ref,
    int rank,
    int importedRanks,
    @Nullable List<String> attributeChoices,
    @Nullable List<String> skillChoices) {}
