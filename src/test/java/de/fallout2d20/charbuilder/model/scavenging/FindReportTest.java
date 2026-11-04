package de.fallout2d20.charbuilder.model.scavenging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.fallout2d20.charbuilder.catalog.loot.LootCategory;
import java.util.List;
import org.junit.jupiter.api.Test;

class FindReportTest {

  private static Find find(LootCategory category, String name, int quantity) {
    return new Find(category, FindOrigin.BASIC, null, 2, name, quantity, null, null, List.of(), false, false, null);
  }

  @Test
  void findsAreListedPerCategoryInBookOrder() {
    String text = FindReport.text("Ruined house", List.of(find(LootCategory.JUNK, "Junk items: {}", 16),
        find(LootCategory.AMMUNITION, "10mm", 9), find(LootCategory.AMMUNITION, "Fusion Core", 1)));
    assertEquals(String.join(System.lineSeparator(), "Ruined house", "Ammunition:", "- 9 × 10mm", "- Fusion Core",
        "Junk:", "- Junk items: 16"), text);
  }

  /** CRB p. 204: the ammunition of a ranged weapon is part of what is passed on. */
  @Test
  void theAmmunitionOfAWeaponIsListed() {
    Find rifle = new Find(LootCategory.WEAPONS_RANGED, FindOrigin.BASIC, null, 13, "Hunting Rifle", 1, null, null,
        List.of(), false, false, new FoundAmmunition(9, ".308 Ammo", null));
    assertEquals(String.join(System.lineSeparator(), "House", "Weapons (Ranged):",
        "- Hunting Rifle, with 9 × .308 Ammo"), FindReport.text("House", List.of(rifle)));
  }
}
