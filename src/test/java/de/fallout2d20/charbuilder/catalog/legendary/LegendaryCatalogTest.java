package de.fallout2d20.charbuilder.catalog.legendary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.weapon.ModEffect;
import de.fallout2d20.charbuilder.catalog.weapon.ModEffectType;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** The legendary property catalogs against WG pp. 97–108. */
class LegendaryCatalogTest {

  /** Tables p. 97 (25 rows) and p. 98 (19 rows); armor table p. 104 (28 rows). All from the Wanderer's Guide. */
  @Test
  void countsAndBook() {
    assertEquals(44, LegendaryCatalog.weaponProperties().size());
    assertEquals(25, LegendaryCatalog.weaponProperties().stream().filter(property -> property.table() == 1).count());
    assertEquals(28, LegendaryCatalog.armorProperties().size());
    assertTrue(LegendaryCatalog.weaponProperties().stream()
        .allMatch(property -> property.isAvailableWith(Set.of(Book.WG)) && !property.isAvailableWith(Set.of(Book.CRB))));
    assertTrue(LegendaryCatalog.armorProperties().stream()
        .allMatch(property -> property.isAvailableWith(Set.of(Book.WG))));
  }

  /** The "No Result – Roll Again" rows of pp. 97–98: the results without a property per column. */
  @Test
  void weaponColumnsLeaveTheNoResultRanges() {
    assertEquals(List.of(18, 19, 20), missing(1, LegendaryWeaponType.SMALL_GUNS));
    assertEquals(List.of(16, 17, 18, 19, 20), missing(1, LegendaryWeaponType.ENERGY_WEAPONS));
    assertEquals(List.of(16, 17, 18, 19, 20), missing(1, LegendaryWeaponType.BIG_GUNS));
    assertEquals(List.of(), missing(1, LegendaryWeaponType.MELEE));
    assertEquals(List.of(18, 19, 20), missing(1, LegendaryWeaponType.UNARMED));
    assertEquals(List.of(18, 19, 20), missing(2, LegendaryWeaponType.SMALL_GUNS));
    assertEquals(List.of(16, 17, 18, 19, 20), missing(2, LegendaryWeaponType.ENERGY_WEAPONS));
    assertEquals(IntStream.rangeClosed(12, 20).boxed().toList(), missing(2, LegendaryWeaponType.MELEE));
    assertEquals(IntStream.rangeClosed(12, 20).boxed().toList(), missing(2, LegendaryWeaponType.UNARMED));
  }

  /** P. 104: every column is full except the head (19–20 have no property). */
  @Test
  void armorColumns() {
    for (LegendaryArmorLocation location : LegendaryArmorLocation.values()) {
      List<Integer> rolls = LegendaryCatalog.armorProperties().stream().map(property -> property.roll(location))
          .filter(Objects::nonNull).sorted().toList();
      int last = location == LegendaryArmorLocation.HEAD ? 18 : 20;
      assertEquals(IntStream.rangeClosed(1, last).boxed().toList(), rolls, location.name());
    }
  }

  /** P. 102: Powerful only in the gun columns, Mighty only for melee and unarmed weapons; both +2 CD. */
  @Test
  void powerfulAndMighty() {
    LegendaryWeaponData powerful = weapon("legendary-weapon-powerful");
    LegendaryWeaponData mighty = weapon("legendary-weapon-mighty");
    assertEquals(EnumSet.of(LegendaryWeaponType.SMALL_GUNS, LegendaryWeaponType.ENERGY_WEAPONS,
        LegendaryWeaponType.BIG_GUNS), EnumSet.copyOf(powerful.rolls().keySet()));
    assertEquals(EnumSet.of(LegendaryWeaponType.MELEE, LegendaryWeaponType.UNARMED),
        EnumSet.copyOf(mighty.rolls().keySet()));
    assertEquals(2, powerful.effects().getFirst().number());
    assertEquals(2, mighty.effects().getFirst().number());
  }

  /** A kind of change {@code ModdedWeapon} does not apply for a legendary property fails the catalog load. */
  @Test
  void unsupportedEffectsAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> new LegendaryWeaponData("x", "X", List.of(new Source(Book.WG,
        98)), 1, Map.of(LegendaryWeaponType.MELEE, 1), List.of(new ModEffect(ModEffectType.RANGE, 1, null)), "Rule."));
    assertEquals(12, LegendaryCatalog.weaponProperties().stream().filter(property -> !property.effects().isEmpty())
        .count());
  }

  /** P. 107: only Poisoner's gives Poison DR, +4. */
  @Test
  void poisoners() {
    assertEquals(4, LegendaryCatalog.armorPropertyById("legendary-armor-poisoners").orElseThrow().poison());
    assertEquals(1, LegendaryCatalog.armorProperties().stream().filter(property -> property.poison() > 0).count());
  }

  /** P. 96: machine guns, the Gatling Gun and the Minigun roll as Small Guns; a Blast Small Gun as a Big Gun. */
  @Test
  void unusualWeapons() {
    for (String id : List.of("50-cal-machine-gun", "gatling-gun", "minigun", "gauss-minigun")) {
      assertTrue(WeaponCatalog.byId(id).orElseThrow().legendaryAsSmallGun(), id);
    }
    assertFalse(WeaponCatalog.byId("gatling-laser").orElseThrow().legendaryAsSmallGun());
    assertEquals(LegendaryWeaponType.BIG_GUNS, type("25mm-grenade-apw"));
    assertEquals(LegendaryWeaponType.SMALL_GUNS, type("minigun"));
    assertEquals(LegendaryWeaponType.BIG_GUNS, type("gatling-laser"));
    assertEquals(LegendaryWeaponType.MELEE, type("machete"));
    assertTrue(LegendaryWeaponType.of(WeaponCatalog.byId("bow").orElseThrow().skill(), List.of(), false).isEmpty());
    assertTrue(LegendaryWeaponType.of(WeaponCatalog.byId("frag-grenade").orElseThrow().skill(), List.of(), false)
        .isEmpty());
  }

  private static LegendaryWeaponType type(String id) {
    var weapon = WeaponCatalog.byId(id).orElseThrow();
    return LegendaryWeaponType.of(weapon.skill(), weapon.qualities(), weapon.legendaryAsSmallGun()).orElseThrow();
  }

  private static LegendaryWeaponData weapon(String id) {
    return LegendaryCatalog.weaponPropertyById(id).orElseThrow();
  }

  private static List<Integer> missing(int table, LegendaryWeaponType type) {
    List<Integer> rolls = LegendaryCatalog.weaponProperties().stream().filter(property -> property.table() == table)
        .map(property -> property.roll(type)).filter(Objects::nonNull).toList();
    return IntStream.rangeClosed(1, 20).boxed().filter(roll -> !rolls.contains(roll)).toList();
  }
}
