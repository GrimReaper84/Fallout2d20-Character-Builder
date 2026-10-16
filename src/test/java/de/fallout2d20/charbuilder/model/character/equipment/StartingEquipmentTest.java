package de.fallout2d20.charbuilder.model.character.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.equipment.ArmAttachment;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StartingEquipmentTest {

  private static final ArmSelection PINCER = ArmSelection.of(ArmAttachment.PINCER);

  @Test
  void armCarriesAnAttachmentOrAWeapon() {
    assertThrows(IllegalArgumentException.class, () -> new ArmSelection(ArmAttachment.FLAMER, "Hunting rifle"));
    assertEquals(ArmSelection.OPEN, ArmSelection.ofWeapon("  "));
    assertTrue(ArmSelection.ofWeapon(" ").isOpen());
    assertEquals("Hunting rifle", ArmSelection.ofWeapon(" Hunting rifle ").weapon());
    assertFalse(ArmSelection.ofWeapon("Hunting rifle").isOpen());
    assertTrue(PINCER.isPincer());
    assertFalse(ArmSelection.of(ArmAttachment.BUZZ_SAW).isPincer());
  }

  /** CRB p. 55: a Mister Handy has three arms; "no pincer" follows from them. */
  @Test
  void hasThreeArmsOrNone() {
    assertThrows(IllegalArgumentException.class, () -> withArms(List.of(PINCER)));
    assertFalse(StartingEquipment.NONE.hasPincer());
    assertFalse(StartingEquipment.NONE.lacksPincer(), "without arms nothing is known");
    StartingEquipment gutsy = withArms(List.of(ArmSelection.of(ArmAttachment.AUTO_PISTOL_10MM),
        ArmSelection.of(ArmAttachment.BUZZ_SAW), ArmSelection.of(ArmAttachment.LASER_EMITTER)));
    assertFalse(gutsy.hasPincer());
    assertTrue(gutsy.lacksPincer());
    assertTrue(gutsy.withArm(1, PINCER).hasPincer());
    assertFalse(gutsy.withArm(1, PINCER).lacksPincer());
    assertFalse(gutsy.hasPincer(), "withers return a new selection");
  }

  @Test
  void isImmutableAndAllowsOpenChoices() {
    List<String> choices = new ArrayList<>(Arrays.asList(null, "Jet"));
    StartingEquipment raider = new StartingEquipment(CatalogRef.of("raider"), choices, Map.of(), " ", false,
        List.of());
    choices.set(1, "RadAway");
    assertEquals(Arrays.asList(null, "Jet"), raider.choices());
    assertNull(raider.trinket());
    assertThrows(UnsupportedOperationException.class, () -> raider.choices().set(0, "Lead pipe"));
    StartingEquipment picked = raider.withChoice(0, "Lead pipe").withTagSkillItem(Skill.THROWING, "2 + 1 CD tomahawks");
    assertEquals(Arrays.asList("Lead pipe", "Jet"), picked.choices());
    assertEquals(Map.of(Skill.THROWING, "2 + 1 CD tomahawks"), picked.tagSkillItems());
    assertEquals(Map.of(), picked.withTagSkillItem(Skill.THROWING, null).tagSkillItems());
    assertEquals(Arrays.asList(null, "Jet"), raider.choices());
  }

  /** Data from a file is fitted to the catalog: stale picks open, choices and arms sized to the pack. */
  @Test
  void fitsSelectionsToTheCatalog() {
    StartingEquipment stale = new StartingEquipment(CatalogRef.of("raider"), Arrays.asList("Pool cue", "Beer"),
        Map.of(Skill.MELEE_WEAPONS, "Machete", Skill.THROWING, "Spear", Skill.REPAIR, "Multi-Tool"), "Medal",
        true, List.of(PINCER, PINCER, PINCER));
    StartingEquipment fitted = stale.fittedToCatalog(false);
    assertEquals(Arrays.asList("Pool cue", null, null), fitted.choices());
    assertEquals(Map.of(Skill.MELEE_WEAPONS, "Machete"), fitted.tagSkillItems());
    assertEquals(List.of(), fitted.arms(), "the Raider pack has no arms");
    assertFalse(fitted.freeArms());
    assertEquals("Medal", fitted.trinket());

    StartingEquipment kept = stale.fittedToCatalog(true);
    assertEquals(Arrays.asList("Pool cue", "Beer", null), kept.choices(), "complete characters keep stale picks");
    assertEquals(stale.tagSkillItems(), kept.tagSkillItems());
    assertEquals(List.of(), kept.arms(), "the structure is fitted anyway");

    StartingEquipment gutsy = new StartingEquipment(CatalogRef.of("mister-gutsy"), List.of(), Map.of(), null,
        false, List.of());
    assertEquals(List.of(ArmSelection.of(ArmAttachment.AUTO_PISTOL_10MM), ArmSelection.of(ArmAttachment.BUZZ_SAW),
        ArmSelection.of(ArmAttachment.LASER_EMITTER)), gutsy.fittedToCatalog(false).arms(),
        "missing arms become the pack's arms");

    StartingEquipment unknownPack = new StartingEquipment(CatalogRef.of("lone-wanderer"), List.of("Knife"),
        Map.of(), null, false, List.of());
    assertEquals(unknownPack, unknownPack.fittedToCatalog(false), "a pack outside the catalog is kept as it is");
    assertEquals(StartingEquipment.NONE, StartingEquipment.NONE.fittedToCatalog(false));
  }

  private static StartingEquipment withArms(List<ArmSelection> arms) {
    return new StartingEquipment(CatalogRef.of("mister-gutsy"), List.of(), Map.of(), null, false, arms);
  }
}
