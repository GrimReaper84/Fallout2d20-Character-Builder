package de.fallout2d20.charbuilder.model.character.derived;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.weapon.AmmunitionData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * The weight a character carries against their carry weight, computed on every call (never stored).
 *
 * <p>Counted: weapons × quantity except those built into the character ({@link CarriedWeapon#isIntegrated()},
 * user decision 2026-10-16), worn apparel (its table weight; robot items have none), items ×
 * quantity, ammunition with a whole-pound weight per unit × count, junk and materials (CRB p. 208).
 * {@code <1} items count nothing (CRB p. 87). Custom weapons and apparel have no weight.
 *
 * <p>CRB p. 87: above the carry weight, STR and AGI tests get +1 difficulty, Sprint is not possible and
 * Initiative drops by 1; 50 lbs. or more over adds 1 to both, and so on per further 50 lbs. At twice the carry weight the character
 * cannot move, fails STR and AGI tests and has Initiative 0.
 *
 * @param carried total weight carried in lbs.
 * @param carryWeight the carry weight from {@link DerivedStats}
 * @param penalty extra difficulty on STR/AGI tests and Initiative reduction; 0 when not over-encumbered
 * @param immobile whether the character carries twice the carry weight or more
 */
public record CarryLoad(int carried, int carryWeight, int penalty, boolean immobile) {

  /** CRB p. 87: every further 50 lbs. over the carry weight raises the penalty by 1. */
  static final int STEP = 50;

  public static @NonNull CarryLoad of(@NonNull Character character) {
    int carried = 0;
    for (CarriedWeapon weapon : character.getWeapons()) {
      if (weapon.isIntegrated()) {
        continue;
      }
      carried += weapon.data().map(WeaponData::weight).map(Weight::pounds).orElse(0) * weapon.quantity();
    }
    for (WornApparel item : character.getApparel()) {
      carried += item.data().map(ApparelData::weight).map(Weight::pounds).orElse(0);
    }
    for (CarriedItem item : character.getItems()) {
      carried += Weight.pounds(item.printedWeight()) * item.quantity();
    }
    for (Map.Entry<String, Integer> ammunition : character.getAmmunition().entrySet()) {
      carried += WeaponCatalog.ammunitionById(ammunition.getKey()).map(AmmunitionData::weight).map(Weight::pounds)
          .orElse(0) * ammunition.getValue();
    }
    carried += character.getMaterials().weight();
    return of(carried, DerivedStats.of(character).carryWeight());
  }

  /** The load of a total weight against a carry weight (CRB p. 87). */
  static @NonNull CarryLoad of(int carried, int carryWeight) {
    int over = carried - carryWeight;
    int penalty = over > 0 ? 1 + over / STEP : 0;
    return new CarryLoad(carried, carryWeight, penalty, carried >= 2 * carryWeight);
  }

  public boolean isOverEncumbered() {
    return penalty > 0;
  }
}
