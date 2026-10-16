package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/** Save-file form of a carried item: a catalog reference or a custom item, the magazine issue, quantity and note. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CarriedItemData(
    @Nullable CatalogRefData ref,
    @Nullable CustomItemData custom,
    @Nullable String issue,
    int quantity,
    @Nullable String notes) {}
