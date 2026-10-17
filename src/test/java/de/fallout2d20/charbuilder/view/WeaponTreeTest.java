package de.fallout2d20.charbuilder.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponAttack;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Grouping of the weapon tree by effective range (CRB p. 89–90). */
class WeaponTreeTest {

  private final Character character = Character.createNew();

  @Test
  void groupsFromNearToFarWithoutEmptyGroups() {
    List<WeaponEntry> entries = List.of(carried(0, "gauss-rifle"), carried(1, "frag-grenade"),
        carried(2, "combat-knife"), carried(3, "bottlecap-mine"), carried(4, "10mm-pistol"), strike());

    Map<RangeGroup, List<WeaponEntry>> groups = WeaponTree.grouped(entries);

    assertEquals(List.of(RangeGroup.MELEE, RangeGroup.CLOSE, RangeGroup.MEDIUM, RangeGroup.LONG, RangeGroup.NO_RANGE),
        List.copyOf(groups.keySet()));
    assertEquals(List.of("Combat Knife", "Unarmed Strike"), names(groups.get(RangeGroup.MELEE)));
    assertEquals(List.of("Frag Grenade"), names(groups.get(RangeGroup.MEDIUM)));
    assertEquals(List.of("Bottlecap Mine"), names(groups.get(RangeGroup.NO_RANGE)));
  }

  @Test
  void weaponsInsideAGroupAreAlphabetical() {
    List<WeaponEntry> entries = List.of(carried(0, "throwing-knives"), carried(1, "10mm-pistol"));

    assertEquals(List.of("10mm Pistol", "Throwing Knives"), names(WeaponTree.grouped(entries).get(RangeGroup.CLOSE)));
  }

  private WeaponEntry carried(int index, String id) {
    CarriedWeapon weapon = CarriedWeapon.of(CatalogRef.of(id));
    return new WeaponEntry(index, WeaponAttack.of(character, weapon), weapon);
  }

  private WeaponEntry strike() {
    return new WeaponEntry(WeaponsTab.UNARMED_STRIKE_INDEX,
        WeaponAttack.of(character, WeaponCatalog.byId("unarmed-strike").orElseThrow()), null);
  }

  private static List<String> names(List<WeaponEntry> entries) {
    return entries.stream().map(entry -> entry.attack().name()).toList();
  }
}
