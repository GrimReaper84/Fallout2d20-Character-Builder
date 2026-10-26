package de.fallout2d20.charbuilder.model.character.apparel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import org.junit.jupiter.api.Test;

/** Legendary armor on the piece's values and the resistances (WG pp. 96, 104, 107). */
class LegendaryApparelTest {

  /** WG p. 104: +1 Physical and Energy DR; Poisoner's +4 Poison DR; WG p. 96: cost ×5. */
  @Test
  void poisonersArm() {
    ModdedApparel arm = ModdedApparel.of(legendary("combat-arm", "poisoners")).orElseThrow();
    assertEquals(3, arm.values().physical());
    assertEquals(3, arm.values().energy());
    assertEquals(4, arm.values().poison());
    assertEquals(125, arm.cost());
    assertEquals("legendary-armor-poisoners", arm.legendary().id());
  }

  /** A property without values still gives the +1/+1 DR. */
  @Test
  void ruleOnlyProperty() {
    ApparelValues values = legendary("combat-chest-piece", "unyielding").values().orElseThrow();
    assertEquals(3, values.physical());
    assertEquals(3, values.energy());
    assertEquals(0, values.poison());
  }

  /** The DR and Poison DR count on the covered location only. */
  @Test
  void resistances() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addApparel(legendary("combat-arm", "poisoners").withLocation(HitLocation.LEFT_ARM));
    LocationResistance left = location(character, HitLocation.LEFT_ARM);
    LocationResistance right = location(character, HitLocation.RIGHT_ARM);
    assertEquals(3, left.physical().value());
    assertEquals(right.poison().value() + 4, left.poison().value());
  }

  private static LocationResistance location(PlayerCharacter character, HitLocation location) {
    return ArmorResistances.of(character).stream().filter(entry -> entry.location() == location).findFirst()
        .orElseThrow();
  }

  private static WornApparel legendary(String piece, String property) {
    return WornApparel.of(CatalogRef.of(piece)).withLegendary(CatalogRef.of("legendary-armor-" + property));
  }
}
