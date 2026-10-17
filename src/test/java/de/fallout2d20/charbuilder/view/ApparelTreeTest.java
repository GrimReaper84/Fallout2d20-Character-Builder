package de.fallout2d20.charbuilder.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.apparel.CustomApparel;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Grouping of the apparel tree by where a piece sits (CRB p. 28, 124, 130, 146; WOA p. 20 house rule). */
class ApparelTreeTest {

  private static final List<HitLocation> HUMANOID = HitLocation.humanoid();
  private static final List<HitLocation> MISTER_HANDY = HitLocation.misterHandy();

  @Test
  void humanoidPiecesBySingleGroup() {
    assertEquals(ApparelGroup.HEAD, group("army-helmet", HUMANOID));
    assertEquals(ApparelGroup.HEAD, group("metal-helmet", HUMANOID));
    assertEquals(ApparelGroup.TORSO, group("raider-chest-piece", HUMANOID));
    assertEquals(ApparelGroup.ARMS, group("raider-arm", HUMANOID), "limb not chosen yet");
    assertEquals(ApparelGroup.ARMS, ApparelTree.group(WornApparel.of(CatalogRef.of("raider-arm"))
        .withLocation(HitLocation.LEFT_ARM), HUMANOID), "limb chosen");
    assertEquals(ApparelGroup.LEGS, group("raider-leg", HUMANOID));
  }

  /**
   * User decisions 2026-10-17: clothing (worn under armor) in its own group; outfits, full-body armor and
   * other pieces over several groups under Outfit.
   */
  @Test
  void clothingAndMultiLocationPieces() {
    assertEquals(ApparelGroup.CLOTHING, group("brotherhood-of-steel-uniform", HUMANOID));
    assertEquals(ApparelGroup.CLOTHING, ApparelTree.group(WornApparel.of(new CustomApparel("Shirt",
        ApparelKind.CLOTHING, Set.of(HitLocation.TORSO), 1, 0, 0, false, 0)), HUMANOID));
    assertEquals(ApparelGroup.OUTFIT, group("brotherhood-of-steel-fatigues", HUMANOID));
    assertEquals(ApparelGroup.OUTFIT, group("cage-armor", HUMANOID));
    assertEquals(ApparelGroup.OUTFIT, group("vault-tec-security-armor", HUMANOID));
    assertEquals(ApparelGroup.OUTFIT, ApparelTree.group(custom(Set.of(HitLocation.HEAD, HitLocation.TORSO)), HUMANOID));
    assertEquals(ApparelGroup.TORSO, ApparelTree.group(custom(Set.of(HitLocation.TORSO)), HUMANOID));
    assertEquals(ApparelGroup.OUTFIT, group("gone", HUMANOID), "unknown id: no locations known");
  }

  @Test
  void misterHandyGroups() {
    assertEquals(ApparelGroup.PLATING, group("standard-plating", MISTER_HANDY));
    assertEquals(ApparelGroup.OPTICS, group("factory-armor-optics", MISTER_HANDY));
    assertEquals(ApparelGroup.MAIN_BODY, group("factory-armor-main-body", MISTER_HANDY));
    assertEquals(ApparelGroup.ARMS, group("factory-armor-arms", MISTER_HANDY));
    assertEquals(ApparelGroup.ARMS, ApparelTree.group(WornApparel.of(CatalogRef.of("factory-armor-arms"))
        .withLocation(HitLocation.ARM_2), MISTER_HANDY), "arm chosen");
    assertEquals(ApparelGroup.THRUSTER, group("factory-armor-thruster", MISTER_HANDY));
  }

  /** WOA p. 20 house rule: Protectron robot armor sits on the mapped humanoid locations. */
  @Test
  void protectronGroups() {
    assertEquals(ApparelGroup.PLATING, group("standard-plating", HUMANOID));
    assertEquals(ApparelGroup.HEAD, group("factory-armor-optics", HUMANOID));
    assertEquals(ApparelGroup.TORSO, group("factory-armor-main-body", HUMANOID));
    assertEquals(ApparelGroup.ARMS, group("factory-armor-arms", HUMANOID));
    assertEquals(ApparelGroup.LEGS, group("factory-armor-thruster", HUMANOID));
  }

  @Test
  void groupsInBodyOrderAlphabeticalWithoutEmptyGroups() {
    List<WornApparel> apparel = List.of(WornApparel.of(CatalogRef.of("raider-chest-piece")),
        WornApparel.of(CatalogRef.of("metal-helmet")), WornApparel.of(CatalogRef.of("army-helmet")),
        WornApparel.of(CatalogRef.of("brotherhood-of-steel-uniform")));

    Map<ApparelGroup, List<ApparelEntry>> groups = ApparelTree.grouped(ApparelTree.entries(apparel, HUMANOID));

    assertEquals(List.of(ApparelGroup.HEAD, ApparelGroup.TORSO, ApparelGroup.CLOTHING), List.copyOf(groups.keySet()));
    assertEquals(List.of(2, 1), groups.get(ApparelGroup.HEAD).stream().map(ApparelEntry::index).toList(),
        "Army Helmet before Metal Helmet");
  }

  @Test
  void labelShowsTheChosenLimb() {
    List<ApparelEntry> entries = ApparelTree.entries(List.of(WornApparel.of(CatalogRef.of("raider-arm")),
        WornApparel.of(CatalogRef.of("raider-arm")).withLocation(HitLocation.RIGHT_ARM)), HUMANOID);
    assertTrue(entries.getFirst().needsLocation());
    assertEquals(entries.getFirst().item().displayName(), entries.getFirst().label());
    assertEquals(entries.get(1).item().displayName() + " (Right Arm)", entries.get(1).label());
  }

  private static ApparelGroup group(String id, List<HitLocation> body) {
    return ApparelTree.group(WornApparel.of(CatalogRef.of(id)), body);
  }

  private static WornApparel custom(Set<HitLocation> locations) {
    return WornApparel.of(new CustomApparel("Homebrew", ApparelKind.ARMOR, locations, 1, 1, 0, false, 0));
  }
}
