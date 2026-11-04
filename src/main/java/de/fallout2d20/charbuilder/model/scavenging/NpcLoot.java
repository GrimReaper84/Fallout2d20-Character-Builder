package de.fallout2d20.charbuilder.model.scavenging;

import de.fallout2d20.charbuilder.catalog.loot.LootCatalog;
import de.fallout2d20.charbuilder.catalog.loot.LootCategory;
import de.fallout2d20.charbuilder.catalog.loot.LootRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * What defeated NPCs carry beyond their inventory (CRB p. 335): caps from their wealth rating in d20s, ammunition for
 * a ranged weapon in the quantity of the loot table, and for each NPC 1 CD (prolific scavengers 2 CD), one junk item
 * of the GM's choice per Effect. Inventory, unused consumables, butchery and salvage come from the NPC's description.
 */
public final class NpcLoot {

  private static final int PROLIFIC_DICE = 2;

  private final @NonNull Dice dice;

  public NpcLoot(@NonNull RandomGenerator random) {
    dice = new Dice(random);
  }

  /** The ammunition rows of the loot table (CRB p. 200), each type once, in table order. */
  public static @NonNull List<LootRow> ammunitionRows() {
    List<LootRow> rows = new ArrayList<>();
    for (LootRow row : LootCatalog.table(LootCategory.AMMUNITION.tableId()).rows()) {
      if (rows.stream().noneMatch(known -> known.name().equals(row.name()))) {
        rows.add(row);
      }
    }
    return rows;
  }

  /**
   * Loots a group of equal NPCs; the results of all of them are added up.
   *
   * @param npcs how many, at least 1
   * @param wealth the wealth rating, normally 1 to 5; 0 for no caps
   * @param ammunition the row of the ammunition their weapon uses, {@code null} without a ranged weapon
   * @param prolific whether they are particularly prolific scavengers
   */
  public @NonNull List<Find> loot(int npcs, int wealth, @Nullable LootRow ammunition, boolean prolific) {
    if (npcs < 1 || wealth < 0) {
      throw new IllegalArgumentException("Invalid NPC loot: " + npcs + " NPCs, wealth " + wealth);
    }
    List<Find> finds = new ArrayList<>();
    if (wealth > 0) {
      int caps = dice.d20s(npcs * wealth);
      finds.add(new Find(LootCategory.ODDITIES, FindOrigin.BASIC, null, caps, LootRow.QUANTITY_MARK + " Caps", caps,
          null, null, List.of(npcs + " × " + wealth + "d20 = " + caps), false, false, null));
    }
    if (ammunition != null) {
      finds.add(ammunition(npcs, ammunition));
    }
    finds.add(junk(npcs, prolific));
    return finds;
  }

  private @NonNull Find ammunition(int npcs, @NonNull LootRow row) {
    String formula = Objects.requireNonNull(row.quantity(), "Ammunition rows have a quantity");
    List<String> steps = new ArrayList<>();
    int total = 0;
    for (int i = 0; i < npcs; i++) {
      RolledQuantity rolled = dice.quantity(formula);
      total += rolled.value();
      steps.add(rolled.text());
    }
    return new Find(LootCategory.AMMUNITION, FindOrigin.BASIC, null, total, row.name(), total, row.note(), row.ref(),
        steps, false, false, null);
  }

  private @NonNull Find junk(int npcs, boolean prolific) {
    int perNpc = prolific ? PROLIFIC_DICE : 1;
    int effects = dice.combatDice(npcs * perNpc).effects();
    return new Find(LootCategory.JUNK, FindOrigin.BASIC, null, effects, "Junk items, GM chooses: " + LootRow.QUANTITY_MARK,
        effects, "Rarity depends on the type of NPC.", null, List.of(npcs * perNpc + " CD: " + effects + " Effects"),
        false, false, null);
  }
}
