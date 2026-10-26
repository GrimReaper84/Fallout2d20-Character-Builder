package de.fallout2d20.charbuilder.model.character.legendary;

import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorLocation;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryCatalog;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Rolls a legendary property on the book tables. Weapons (WG p. 96): a d20 picks Table 1 (1–10) or Table 2 (11–20),
 * then a d20 in the weapon type's column of that table; a result without a property ("No Result – Roll Again") is
 * rolled again on the same table. Armor (WG p. 104): a d20 in the chosen location's column; the head column has no
 * property for 19–20, which is rolled again as well (assumption, plan F6d). Spending Luck to choose the table or
 * shift the result is done by hand in the choice list.
 */
public final class LegendaryRoll {

  private static final int DIE = 20;
  private static final int TABLE_1_UP_TO = 10;

  private final @NonNull RandomGenerator random;

  public LegendaryRoll(@NonNull RandomGenerator random) {
    this.random = random;
  }

  /** A property for a weapon of this type. */
  public @NonNull LegendaryRollResult<LegendaryWeaponData> weapon(@NonNull LegendaryWeaponType type) {
    int tableRoll = d20();
    int table = tableRoll <= TABLE_1_UP_TO ? 1 : 2;
    List<String> steps = new ArrayList<>(List.of("d20 " + tableRoll + ": Table " + table));
    List<LegendaryWeaponData> column = LegendaryCatalog.weaponProperties().stream()
        .filter(property -> property.table() == table && property.fits(type)).toList();
    LegendaryWeaponData property = roll(steps, result -> column.stream()
        .filter(entry -> Objects.equals(entry.roll(type), result)).findFirst().orElse(null));
    return new LegendaryRollResult<>(property, String.join(" · ", steps));
  }

  /** A property for an armor piece, on the column of the chosen location. */
  public @NonNull LegendaryRollResult<LegendaryArmorData> armor(@NonNull LegendaryArmorLocation location) {
    List<String> steps = new ArrayList<>(List.of(location.displayName()));
    LegendaryArmorData property = roll(steps, result -> LegendaryCatalog.armorProperties().stream()
        .filter(entry -> Objects.equals(entry.roll(location), result)).findFirst().orElse(null));
    return new LegendaryRollResult<>(property, String.join(" · ", steps));
  }

  /** Rolls a d20 until a result names a property; every column has at least one. */
  private <T> @NonNull T roll(@NonNull List<String> steps, @NonNull IntFunction<@Nullable T> lookup) {
    while (true) {
      int result = d20();
      Optional<T> property = Optional.ofNullable(lookup.apply(result));
      if (property.isPresent()) {
        steps.add("d20 " + result);
        return property.get();
      }
      steps.add("d20 " + result + ": no result");
    }
  }

  private int d20() {
    return random.nextInt(DIE) + 1;
  }
}
