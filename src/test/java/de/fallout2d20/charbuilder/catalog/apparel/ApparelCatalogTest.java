package de.fallout2d20.charbuilder.catalog.apparel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Spot checks of the apparel catalog against CRB p. 124–132 and 146–147. */
class ApparelCatalogTest {

  @Test
  void containsAllCoreItemsPerKind() {
    Map<ApparelKind, Long> perKind = ApparelCatalog.all().stream()
        .collect(Collectors.groupingBy(ApparelData::kind, Collectors.counting()));
    assertEquals(Map.of(ApparelKind.CLOTHING, 7L, ApparelKind.OUTFIT, 12L, ApparelKind.HEADGEAR, 10L,
        ApparelKind.ARMOR, 56L, ApparelKind.ROBOT_PLATING, 2L, ApparelKind.ROBOT_ARMOR, 33L), perKind);
  }

  /** CRB p. 130. */
  @Test
  void raiderChestPiece() {
    assertEquals(new ApparelData("raider-chest-piece", "Raider Chest Piece", List.of(new Source(Book.CRB, 130)),
        ApparelKind.ARMOR, List.of(Coverage.TORSO), 1, 1, 0, false, "7", 18, 0, 0, null),
        ApparelCatalog.byId("raider-chest-piece").orElseThrow());
    assertTrue(ApparelCatalog.byId("raider-arm").orElseThrow().needsLocation());
  }

  /** CRB p. 124: radiation "Immune"; CRB p. 146–147: plating and robot armor values. */
  @Test
  void hazmatSuitAndRobotArmor() {
    ApparelData hazmat = ApparelCatalog.byId("hazmat-suit").orElseThrow();
    assertTrue(hazmat.radiationImmune());
    assertEquals(List.of(Coverage.HEAD, Coverage.ARMS, Coverage.LEGS, Coverage.TORSO), hazmat.coverage());
    ApparelData gutsy = ApparelCatalog.byId("mister-gutsy-plating").orElseThrow();
    assertEquals(-10, gutsy.carryWeight());
    assertNull(gutsy.cost());
    ApparelData storage = ApparelCatalog.byId("factory-storage-armor-main-body").orElseThrow();
    assertEquals(20, storage.carryWeight());
    assertEquals("Armorer 1", storage.perk());
    assertEquals(List.of(Coverage.MAIN_BODY), storage.coverage());
  }

  /** CRB p. 146: hats are the only headgear a Mister Handy may wear. */
  @Test
  void hats() {
    assertEquals("Brotherhood Scribe’s Hat", ApparelCatalog.byId("brotherhood-scribes-hat").orElseThrow().name());
    assertTrue(ApparelCatalog.byId("casual-hat").orElseThrow().isHat());
    assertFalse(ApparelCatalog.byId("army-helmet").orElseThrow().isHat());
  }
}
