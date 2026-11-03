package de.fallout2d20.charbuilder.model.character.derived;

import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.catalog.weapon.AmmunitionData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.apparel.ModdedApparel;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.Materials;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.ModdedWeapon;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * The weight a character carries against their carry weight, computed on every call (never stored).
 *
 * <p>Counted: weapons × quantity except those built into the character ({@link CarriedWeapon#isIntegrated()},
 * user decision 2026-10-16), worn and carried apparel (its table weight plus its mods', {@link ModdedApparel}; robot
 * items have none), items × quantity, ammunition with a whole-pound weight per unit × count, junk and materials
 * (CRB p. 208). {@code <1} items count nothing (CRB p. 87). Weapons count with their mods' weight
 * ({@link ModdedWeapon}); custom weapons and custom apparel have no weight of their own, only their mods'.
 *
 * <p>Wanderer's Guide perks: Sturdy Frame lets a quarter of the weight of worn armor ({@link WornApparel#isArmor()})
 * stop counting per rank, rounded down once (WG p. 45); Pack Rat halves the weight of junk, the junk counter and the
 * junk items of the WG tables alike (WG p. 45; user decision 2026-10-22), the items' half rounded down once. The book's
 * Power Armor exception does not arise yet (D3).
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
  private static final int QUARTERS = 4;

  public static @NonNull CarryLoad of(@NonNull PlayerCharacter character) {
    int carried = 0;
    for (CarriedWeapon weapon : character.gear().getWeapons()) {
      if (weapon.isIntegrated()) {
        continue;
      }
      carried += ModdedWeapon.of(weapon).map(ModdedWeapon::weight).map(Weight::pounds).orElse(0) * weapon.quantity();
    }
    int wornArmor = 0;
    for (WornApparel item : character.gear().getApparel()) {
      int pounds = ModdedApparel.of(item).map(ModdedApparel::weight).map(Weight::pounds).orElse(0);
      if (item.isArmor() && !item.carried()) {
        wornArmor += pounds;
      } else {
        carried += pounds;
      }
    }
    // At most three quarters stop counting (Sturdy Frame rank 3, WG p. 45).
    int quartersOff = Math.min(QUARTERS - 1, character.perkEffect(PerkDefinition::armorWeightQuartersPerRank));
    carried += wornArmor * (QUARTERS - quartersOff) / QUARTERS;
    int junkItems = 0;
    for (CarriedItem item : character.gear().getItems()) {
      int pounds = Weight.pounds(item.printedWeight()) * item.quantity();
      if (item.isJunk()) {
        junkItems += pounds;
      } else {
        carried += pounds;
      }
    }
    for (Map.Entry<String, Integer> ammunition : character.gear().getAmmunition().entrySet()) {
      carried += WeaponCatalog.ammunitionById(ammunition.getKey()).map(AmmunitionData::weight).map(Weight::pounds)
          .orElse(0) * ammunition.getValue();
    }
    Materials materials = character.gear().getMaterials();
    carried += materials.weight();
    if (character.perkEffect(perk -> perk.junkWeightHalved() ? 1 : 0) > 0) {
      // WG p. 45 Pack Rat: junk counts half (2 lbs. each, CRB p. 208, so exactly 1 lb.); junk items half their
      // total, rounded down.
      carried -= materials.junk() * Materials.JUNK_WEIGHT / 2;
      carried += junkItems / 2;
    } else {
      carried += junkItems;
    }
    return of(carried, DerivedStats.of(character).carryWeight());
  }

  /** The load of a total weight against a carry weight (CRB p. 87). */
  static @NonNull CarryLoad of(int carried, int carryWeight) {
    int over = carried - carryWeight;
    int penalty = over > 0 ? 1 + over / STEP : 0;
    return new CarryLoad(carried, carryWeight, penalty, carried >= 2 * carryWeight);
  }

  /**
   * The Initiative with this load (CRB p. 87): lowered by the penalty, 0 when the character cannot move.
   *
   * @param base the unencumbered Initiative ({@link DerivedStats#initiative()})
   */
  public int initiative(int base) {
    return immobile ? 0 : Math.max(0, base - penalty);
  }

  public boolean isOverEncumbered() {
    return penalty > 0;
  }
}
