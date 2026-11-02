package de.fallout2d20.charbuilder.catalog.perk;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.weapon.WeaponKind;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WeaponFilterTest {

  private static final FilteredWeapon RIFLE = new FilteredWeapon("hunting-rifle", Skill.SMALL_GUNS, 0,
      List.of("Two-Handed", "Recoil (7)"), List.of("Piercing 1"), "Physical", Set.of());
  private static final FilteredWeapon MACHETE = new FilteredWeapon("machete", Skill.MELEE_WEAPONS, null,
      List.of(), List.of("Piercing 1"), "Physical", Set.of(WeaponKind.BLADED));

  @Test
  void emptyFilterMatchesEverything() {
    assertTrue(WeaponFilter.ANY.matches(RIFLE));
    assertTrue(WeaponFilter.ANY.matches(MACHETE));
  }

  @Test
  void handednessByTheTwoHandedQuality() {
    assertTrue(filter(Set.of(), true, null, null).matches(RIFLE));
    assertFalse(filter(Set.of(), false, null, null).matches(RIFLE));
    assertTrue(filter(Set.of(), false, null, null).matches(MACHETE));
  }

  @Test
  void fireRateBoundNeedsAFireRate() {
    assertTrue(filter(Set.of(Skill.SMALL_GUNS), null, null, 2).matches(RIFLE));
    assertFalse(filter(Set.of(), null, 1, null).matches(RIFLE));
    assertFalse(filter(Set.of(), null, null, 2).matches(MACHETE));
  }

  @Test
  void kindsQualitiesIdsAndRadiation() {
    assertTrue(new WeaponFilter(Set.of(), null, null, null, Set.of(WeaponKind.BLADED), List.of(), List.of(), false)
        .matches(MACHETE));
    assertFalse(new WeaponFilter(Set.of(), null, null, null, Set.of(WeaponKind.BLADED), List.of(), List.of(), false)
        .matches(RIFLE));
    assertTrue(new WeaponFilter(Set.of(), null, null, null, Set.of(), List.of("Recoil"), List.of(), false)
        .matches(RIFLE));
    assertFalse(new WeaponFilter(Set.of(), null, null, null, Set.of(), List.of(), List.of("gun-bash"), false)
        .matches(RIFLE));
    assertFalse(new WeaponFilter(Set.of(), null, null, null, Set.of(), List.of(), List.of(), true).matches(RIFLE));
    assertTrue(new WeaponFilter(Set.of(), null, null, null, Set.of(), List.of(), List.of(), true)
        .matches(new FilteredWeapon(null, Skill.ENERGY_WEAPONS, 1, List.of(), List.of(), "Radiation", Set.of())));
  }

  private static WeaponFilter filter(Set<Skill> skills, Boolean twoHanded, Integer min, Integer max) {
    return new WeaponFilter(skills, twoHanded, min, max, Set.of(), List.of(), List.of(), false);
  }
}
