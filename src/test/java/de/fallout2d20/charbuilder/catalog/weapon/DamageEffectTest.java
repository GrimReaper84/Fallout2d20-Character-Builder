package de.fallout2d20.charbuilder.catalog.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Damage effects as printed in the weapons (CRB p. 30, WG p. 56). */
class DamageEffectTest {

  @Test
  void readsRatingsAndTypes() {
    assertEquals(Optional.of(DamageEffect.PIERCING), DamageEffect.of("Piercing 2"));
    assertEquals(Optional.of(DamageEffect.PERSISTENT), DamageEffect.of("Persistent (Poison)"));
    assertEquals(Optional.of(DamageEffect.ARC), DamageEffect.of("arc"));
    assertTrue(DamageEffect.of("Glowing").isEmpty());
  }

  /** Every damage effect of the weapons and mods is one of the books'. */
  @Test
  void everyCatalogEffectIsKnown() {
    for (WeaponData weapon : WeaponCatalog.all()) {
      for (String effect : weapon.damageEffects()) {
        assertTrue(DamageEffect.of(effect).isPresent(), weapon.id() + ": " + effect);
      }
    }
    for (WeaponModData mod : WeaponCatalog.mods()) {
      mod.effects().stream().filter(effect -> effect.type() == ModEffectType.GAIN_EFFECT)
          .forEach(effect -> assertTrue(DamageEffect.of(effect.text()).isPresent(), mod.id()));
    }
  }
}
