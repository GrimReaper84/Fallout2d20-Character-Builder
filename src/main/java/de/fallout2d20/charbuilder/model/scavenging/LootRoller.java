package de.fallout2d20.charbuilder.model.scavenging;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.loot.LootCatalog;
import de.fallout2d20.charbuilder.catalog.loot.LootCategory;
import de.fallout2d20.charbuilder.catalog.loot.LootRef;
import de.fallout2d20.charbuilder.catalog.loot.LootRefKind;
import de.fallout2d20.charbuilder.catalog.loot.LootRow;
import de.fallout2d20.charbuilder.catalog.loot.LootTable;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Rolls finds on the loot tables, with the rolls that hang on a row:
 * <ul>
 *   <li>a row may send to another table (magazine CRB p. 172, junk tables WG p. 120);
 *   <li>an armor piece rolls its hit location, rolling again where the armor has no such piece (CRB p. 201);
 *   <li>a ranged weapon comes with a standard quantity of its ammunition (CRB p. 204);
 *   <li>a junk roll is a number of items (CRB p. 208) or one item from the detailed tables (WG p. 120).
 * </ul>
 * Rolls the book leaves out (Random Ranged Weapons has no 10 and 19) are rolled again, the same way the legendary
 * tables treat a roll without a result.
 */
public final class LootRoller {

  private static final @NonNull String MAGAZINE_TABLE = "magazine";
  private static final @NonNull String RARE_BOOK_TABLE = "rare-book";
  private static final int JUNK_DICE = 2;
  private static final int LAST_HEAD = 2;
  private static final int LAST_TORSO = 8;
  private static final int LAST_LEFT_ARM = 11;
  private static final int LAST_RIGHT_ARM = 14;
  private static final int LAST_LEFT_LEG = 17;
  private static final @NonNull String JUNK_COUNT_NOTE = "Weight 2, worth 2 caps each.";

  private final @NonNull Dice dice;

  public LootRoller(@NonNull RandomGenerator random) {
    dice = new Dice(random);
  }

  /** One roll on the category's table. */
  public @NonNull Find roll(@NonNull LootCategory category, @NonNull FindOrigin origin, @NonNull JunkMode junkMode) {
    if (category == LootCategory.JUNK && junkMode == JunkMode.COUNT) {
      int count = dice.d20s(JUNK_DICE);
      return new Find(category, origin, null, count, "Junk items: " + LootRow.QUANTITY_MARK, count, JUNK_COUNT_NOTE,
          null, List.of(JUNK_DICE + "d20 = " + count), false, false, null);
    }
    List<String> steps = new ArrayList<>();
    LootTable table = LootCatalog.table(category.tableId());
    int roll = rollOn(table, steps);
    if (category == LootCategory.JUNK) {
      // The selection only leads to the junk table, whose rolls can be adjusted (WG p. 120).
      LootRow selected = table.row(roll).orElseThrow();
      steps.add(selected.name());
      table = LootCatalog.table(Objects.requireNonNull(selected.next(), "The junk selection sends to a table"));
      roll = rollOn(table, steps);
    }
    return resolve(category, origin, table, roll, steps, false);
  }

  /** The rows the find's roll can be shifted to: up to {@code range} up or down, within the table. */
  public @NonNull List<LootOption> options(@NonNull Find find, int range) {
    String tableId = find.tableId();
    if (tableId == null) {
      return List.of();
    }
    LootTable table = LootCatalog.table(tableId);
    Optional<LootRow> current = table.row(find.roll());
    Map<LootRow, Integer> nearest = new LinkedHashMap<>();
    for (int roll = Math.max(table.minRoll(), find.roll() - range);
        roll <= Math.min(table.maxRoll(), find.roll() + range); roll++) {
      int candidate = roll;
      table.row(roll).filter(row -> !current.equals(Optional.of(row))).ifPresent(row -> nearest.merge(row, candidate,
          (known, next) -> Math.abs(next - find.roll()) < Math.abs(known - find.roll()) ? next : known));
    }
    return nearest.entrySet().stream().map(entry -> new LootOption(entry.getValue(), entry.getKey())).toList();
  }

  /** The find with its roll moved to another row; everything hanging on the row is rolled anew. */
  public @NonNull Find shift(@NonNull Find find, int roll) {
    String tableId = Objects.requireNonNull(find.tableId(), "This find has no table to shift on");
    LootTable table = LootCatalog.table(tableId);
    List<String> steps = new ArrayList<>(
        List.of(table.diceText() + " " + find.roll() + ", shifted to " + roll));
    return resolve(find.category(), find.origin(), table, roll, steps, true);
  }

  /** The Rare Book a magazine find becomes for 1 Luck (WG p. 108), instead of the publication roll. */
  public @NonNull Find rareBook(@NonNull Find find) {
    LootTable table = LootCatalog.table(RARE_BOOK_TABLE);
    List<String> steps = new ArrayList<>(find.steps());
    steps.add("1 Luck: Rare Book");
    int roll = rollOn(table, steps);
    LootRow row = table.row(roll).orElseThrow();
    return new Find(find.category(), find.origin(), find.tableId(), find.roll(), row.name(), 1, null, row.ref(), steps,
        true, false, null);
  }

  /** A junk count raised by the LCK score for 1 Luck (CRB p. 208). */
  public @NonNull Find addLuck(@NonNull Find find, int luck) {
    List<String> steps = new ArrayList<>(find.steps());
    steps.add("1 Luck: +" + luck);
    return new Find(find.category(), find.origin(), find.tableId(), find.roll(), find.name(), find.quantity() + luck,
        find.note(), find.ref(), steps, true, false, null);
  }

  /** Rolls on the table until a roll has a row. */
  private int rollOn(@NonNull LootTable table, @NonNull List<String> steps) {
    while (true) {
      int roll = dice.d20s(table.dice());
      if (table.row(roll).isPresent()) {
        steps.add(table.diceText() + " " + roll);
        return roll;
      }
      steps.add(table.diceText() + " " + roll + ": no row in the book, rolled again");
    }
  }

  /** Follows the row of the roll to its result; a shift of the find acts on the start table and rolls the rest anew. */
  private @NonNull Find resolve(@NonNull LootCategory category, @NonNull FindOrigin origin, @NonNull LootTable start,
      int startRoll, @NonNull List<String> steps, boolean luckUsed) {
    LootRow row = start.row(startRoll).orElseThrow();
    boolean magazine = MAGAZINE_TABLE.equals(row.next());
    String note = row.note();
    while (row.next() != null) {
      steps.add(row.name());
      LootTable table = LootCatalog.table(row.next());
      row = table.row(rollOn(table, steps)).orElseThrow();
      note = row.note() == null ? note : row.note();
    }
    int quantity = 1;
    if (row.quantity() != null) {
      RolledQuantity rolled = dice.quantity(row.quantity());
      quantity = rolled.value();
      steps.add(rolled.text());
    }
    String name = row.name();
    LootRef ref = row.ref();
    if (row.rollsLocation()) {
      HitLocation location = rollLocation(row.pieces(), steps);
      Optional<ApparelData> piece = Optional.ofNullable(row.pieces()).flatMap(pieces -> piece(pieces, location));
      name = piece.map(ApparelData::name).orElse(row.name()) + " (" + location.displayName() + ")";
      ref = piece.map(found -> new LootRef(LootRefKind.APPAREL, found.id())).orElse(ref);
    }
    FoundAmmunition ammunition = category == LootCategory.WEAPONS_RANGED ? ammunition(ref, steps) : null;
    return new Find(category, origin, start.id(), startRoll, name, quantity, joined(note, start.note()), ref, steps,
        luckUsed, magazine, ammunition);
  }

  private static @Nullable String joined(@Nullable String first, @Nullable String second) {
    if (first == null || second == null) {
      return first == null ? second : first;
    }
    return first + " " + second;
  }

  /**
   * CRB p. 204: a ranged weapon comes with a standard quantity of its ammunition, in the amount of the loot table so
   * that the same ammunition always rolls the same; a weapon whose ammunition is not on the table (Junk Jet) gets none.
   */
  private @Nullable FoundAmmunition ammunition(@Nullable LootRef weapon, @NonNull List<String> steps) {
    if (weapon == null) {
      return null;
    }
    Optional<String> ammunitionId = WeaponCatalog.byId(weapon.id()).map(WeaponData::ammunition).stream()
        .flatMap(List::stream).findFirst();
    Optional<LootRow> row = LootCatalog.table(LootCategory.AMMUNITION.tableId()).rows().stream()
        .filter(candidate -> candidate.ref() != null && Optional.of(candidate.ref().id()).equals(ammunitionId))
        .findFirst();
    if (row.isEmpty()) {
      return null;
    }
    RolledQuantity rolled = dice.quantity(Objects.requireNonNull(row.get().quantity()));
    steps.add(row.get().name() + ": " + rolled.text());
    return new FoundAmmunition(rolled.value(), row.get().name(), row.get().ref());
  }

  private @NonNull HitLocation rollLocation(@Nullable String pieces, @NonNull List<String> steps) {
    while (true) {
      int roll = dice.d20();
      HitLocation location = location(roll);
      if (pieces == null || piece(pieces, location).isPresent()) {
        steps.add("location d20 " + roll + ": " + location.displayName());
        return location;
      }
      steps.add("location d20 " + roll + ": no " + location.displayName() + " piece, rolled again");
    }
  }

  /** The Hit Locations table, CRB p. 28. */
  private static @NonNull HitLocation location(int roll) {
    if (roll <= LAST_HEAD) {
      return HitLocation.HEAD;
    }
    if (roll <= LAST_TORSO) {
      return HitLocation.TORSO;
    }
    if (roll <= LAST_LEFT_ARM) {
      return HitLocation.LEFT_ARM;
    }
    if (roll <= LAST_RIGHT_ARM) {
      return HitLocation.RIGHT_ARM;
    }
    return roll <= LAST_LEFT_LEG ? HitLocation.LEFT_LEG : HitLocation.RIGHT_LEG;
  }

  private static @NonNull Optional<ApparelData> piece(@NonNull String pieces, @NonNull HitLocation location) {
    String suffix = switch (location) {
      case HEAD -> "-helmet";
      case TORSO -> "-chest-piece";
      case LEFT_ARM, RIGHT_ARM -> "-arm";
      case LEFT_LEG, RIGHT_LEG -> "-leg";
      default -> throw new IllegalArgumentException("Not a humanoid location: " + location);
    };
    return ApparelCatalog.byId(pieces + suffix);
  }
}
