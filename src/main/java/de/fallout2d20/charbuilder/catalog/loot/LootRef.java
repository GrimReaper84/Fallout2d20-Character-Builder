package de.fallout2d20.charbuilder.catalog.loot;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;

/**
 * The catalog entry behind a loot table row. Table names do not always equal catalog names (a weapon with mods, a
 * name in another wording), so the link is explicit.
 *
 * @param kind the catalog
 * @param id the entry's id in that catalog
 */
public record LootRef(
    @JsonProperty(required = true) @NonNull LootRefKind kind,
    @JsonProperty(required = true) @NonNull String id) {}
