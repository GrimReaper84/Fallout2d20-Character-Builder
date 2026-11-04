package de.fallout2d20.charbuilder.model.scavenging;

import de.fallout2d20.charbuilder.catalog.loot.LootCategory;
import de.fallout2d20.charbuilder.catalog.loot.LootRef;
import de.fallout2d20.charbuilder.catalog.loot.LootRow;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One result of a roll on the loot tables.
 *
 * @param category the item category
 * @param origin why it was rolled
 * @param tableId the table a shift of the roll acts on; {@code null} where no table row can be chosen (junk count, NPC)
 * @param roll the roll on that table
 * @param name the result; a {@link LootRow#QUANTITY_MARK} stands for the quantity
 * @param quantity how many
 * @param note a remark of the table or row
 * @param ref the catalog entry
 * @param steps the rolls that led here, for traceability
 * @param luckUsed whether the roll was already changed by Luck (or the LCK adjustment of a junk table)
 * @param magazine whether this is a magazine find, which 1 Luck can turn into a Rare Book (WG p. 108)
 * @param ammunition the standard ammunition a ranged weapon comes with (CRB p. 204)
 */
public record Find(
    @NonNull LootCategory category,
    @NonNull FindOrigin origin,
    @Nullable String tableId,
    int roll,
    @NonNull String name,
    int quantity,
    @Nullable String note,
    @Nullable LootRef ref,
    @NonNull List<String> steps,
    boolean luckUsed,
    boolean magazine,
    @Nullable FoundAmmunition ammunition) {

  public Find {
    steps = List.copyOf(steps);
  }

  /** The result as one line, e.g. {@code 9 × 10mm} or {@code 57 Caps}. */
  public @NonNull String text() {
    if (name.contains(LootRow.QUANTITY_MARK)) {
      return name.replace(LootRow.QUANTITY_MARK, Integer.toString(quantity));
    }
    return quantity == 1 ? name : quantity + " × " + name;
  }

  /** The result with the ammunition that comes with it, e.g. {@code Hunting Rifle, with 9 × .308 Ammo}. */
  public @NonNull String fullText() {
    return ammunition == null ? text() : text() + ", " + ammunition.text();
  }

  /** The steps as one line. */
  public @NonNull String stepsText() {
    return String.join(" · ", steps);
  }
}
