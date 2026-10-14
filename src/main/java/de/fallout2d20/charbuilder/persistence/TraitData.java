package de.fallout2d20.charbuilder.persistence;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** Save-file form of a trait selection; attribute choices as attribute ids. */
public record TraitData(CatalogRefData ref, boolean imported, @Nullable List<String> attributeChoices) {}
