package de.fallout2d20.charbuilder.catalog.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The weapon qualities of CRB p. 90 as read from and written to a weapon's quality text. */
class WeaponQualityTest {

  @Test
  void readsAndWritesQualitiesInBookOrder() {
    QualitySelection selection = WeaponQuality.parse("two-handed, Thrown (M), close-quarters");
    assertEquals(Set.of(WeaponQuality.TWO_HANDED, WeaponQuality.THROWN, WeaponQuality.CLOSE_QUARTERS),
        selection.qualities());
    assertEquals(WeaponRange.MEDIUM, selection.thrownRange());
    assertEquals("", selection.other());
    assertEquals("Close Quarters, Thrown (M), Two-Handed", selection.text());
    assertEquals(selection, WeaponQuality.parse(selection.text()));
  }

  @Test
  void keepsWhatIsNotABookQuality() {
    QualitySelection selection = WeaponQuality.parse("Blast, Big Boom , Thrown, ");
    assertEquals(Set.of(WeaponQuality.BLAST), selection.qualities());
    assertNull(selection.thrownRange());
    assertEquals("Big Boom, Thrown", selection.other());
    assertEquals("Blast, Big Boom, Thrown", selection.text());
    assertEquals("", WeaponQuality.parse("  ").text());
  }

  @Test
  void thrownNeedsItsRange() {
    Set<WeaponQuality> thrown = Set.of(WeaponQuality.THROWN);
    Set<WeaponQuality> none = Set.of();
    assertThrows(IllegalArgumentException.class, () -> new QualitySelection(thrown, null, ""));
    assertThrows(IllegalArgumentException.class, () -> new QualitySelection(none, WeaponRange.CLOSE, ""));
    assertEquals("Thrown (C)", new QualitySelection(thrown, WeaponRange.CLOSE, "").text());
  }

  /** CRB p. 90: Accurate/Inaccurate and Reliable/Unreliable can not be on one weapon. */
  @Test
  void oppositesExcludeEachOther() {
    assertEquals(Optional.of(WeaponQuality.INACCURATE), WeaponQuality.ACCURATE.opposite());
    assertEquals(Optional.of(WeaponQuality.RELIABLE), WeaponQuality.UNRELIABLE.opposite());
    assertTrue(WeaponQuality.BLAST.opposite().isEmpty());
  }

  @Test
  void summariesForTooltips() {
    assertEquals(Optional.of(WeaponQuality.THROWN.summary()), WeaponQuality.summaryOf("Thrown (C)"));
    assertEquals(Optional.of(WeaponQuality.PARRY.summary()), WeaponQuality.summaryOf("parry"));
    assertTrue(WeaponQuality.summaryOf("Big Boom").isEmpty());
    for (WeaponQuality quality : WeaponQuality.values()) {
      assertFalse(quality.summary().isBlank(), quality.name());
    }
  }

  /** Every quality written in the weapon catalog is one of the book's (no typos in the catalog). */
  @Test
  void catalogQualitiesAreBookQualities() {
    for (WeaponData weapon : WeaponCatalog.all()) {
      QualitySelection selection = WeaponQuality.parse(String.join(", ", weapon.qualities()));
      assertEquals("", selection.other(), weapon.id());
    }
  }
}
