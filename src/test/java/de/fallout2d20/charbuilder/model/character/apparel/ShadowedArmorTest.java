package de.fallout2d20.charbuilder.model.character.apparel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModSlot;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import org.junit.jupiter.api.Test;

/** Shadowed armor tiers (CRB p. 133 sidebar; WG p. 86 Hunter's Pelt Outfit). */
class ShadowedArmorTest {

  private static void wearShadowed(PlayerCharacter character, String id, HitLocation location) {
    character.gear().addApparel(WornApparel.of(CatalogRef.of(id)).withLocation(location));
    int index = character.gear().getApparel().size() - 1;
    character.gear().setApparelMod(index, ApparelModSlot.MATERIAL,
        ApparelCatalog.modById("leather-shadowed-leather").orElseThrow());
  }

  @Test
  void torsoCountsTwiceAndCarriedPiecesNotAtAll() {
    PlayerCharacter character = PlayerCharacter.createNew();
    assertEquals(0, ShadowedArmor.count(character));
    assertTrue(ShadowedArmor.effect(0).isEmpty());
    wearShadowed(character, "leather-arm", HitLocation.LEFT_ARM);
    assertEquals(1, ShadowedArmor.count(character));
    assertTrue(ShadowedArmor.effect(1).startsWith("Ignore the first complication"));
    wearShadowed(character, "leather-chest-piece", null);
    assertEquals(3, ShadowedArmor.count(character));
    assertTrue(ShadowedArmor.effect(3).contains("per scene"));
    wearShadowed(character, "leather-leg", HitLocation.LEFT_LEG);
    wearShadowed(character, "leather-leg", HitLocation.RIGHT_LEG);
    assertEquals(5, ShadowedArmor.count(character));
    assertTrue(ShadowedArmor.effect(5).contains("any Sneak test"));
    character.gear().setApparelCarried(1, true);
    assertEquals(3, ShadowedArmor.count(character));
  }

  /** WG p. 86: the Hunter's Pelt Outfit counts as four Shadowed pieces, the Hood as one more. */
  @Test
  void huntersPeltCountsAsFourPieces() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addApparel(WornApparel.of(CatalogRef.of("hunters-pelt-outfit")));
    assertEquals(4, ShadowedArmor.count(character));
    character.gear().addApparel(WornApparel.of(CatalogRef.of("hunters-hood")));
    assertEquals(5, ShadowedArmor.count(character));
  }
}
