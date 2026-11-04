package de.fallout2d20.charbuilder.model.scavenging;

import de.fallout2d20.charbuilder.catalog.loot.LootRow;
import org.jspecify.annotations.NonNull;

/**
 * A row a find's roll can be shifted to.
 *
 * @param roll the nearest roll on the row
 * @param row the row
 */
public record LootOption(int roll, @NonNull LootRow row) {}
