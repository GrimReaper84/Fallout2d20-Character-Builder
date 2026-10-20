package de.fallout2d20.charbuilder.catalog.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class EquipmentPackDataTest {

  private static final List<Source> CRB = List.of(new Source(Book.CRB, 78));
  private static final ArmSlot PINCER = new ArmSlot(List.of(ArmAttachment.PINCER));

  @Test
  void rejectsIncompletePacks() {
    List<String> vaultDweller = List.of("vault-dweller");
    List<String> blankItem = List.of(" ");
    assertThrows(IllegalArgumentException.class, () -> pack(List.of(), vaultDweller, null, 0, null));
    assertThrows(IllegalArgumentException.class, () -> pack(CRB, List.of(), null, 0, null));
    assertThrows(IllegalArgumentException.class, () -> new EquipmentPackData(" ", "X", CRB,
        vaultDweller, null, List.of(), null, false, 0, null));
    assertThrows(IllegalArgumentException.class, () -> new EquipmentPackData("x", "X", CRB,
        vaultDweller, null, blankItem, null, false, 0, null));
  }

  @Test
  void rejectsInvalidContents() {
    List<String> vaultDweller = List.of("vault-dweller");
    List<String> misterHandy = List.of("mister-handy");
    List<ArmSlot> twoArms = List.of(PINCER, PINCER);
    assertThrows(IllegalArgumentException.class, () -> pack(CRB, vaultDweller, null, -1, null));
    assertThrows(IllegalArgumentException.class, () -> pack(CRB, misterHandy, null, 10, twoArms));
    assertThrows(IllegalArgumentException.class, () -> pack(CRB, vaultDweller, vaultDweller, 0, null));
  }

  @Test
  void rejectsInvalidChoicesAndArmSlots() {
    List<String> oneOption = List.of("Baton");
    List<String> sameOptions = List.of("Jet", "Jet");
    List<String> options = List.of("Jet", "RadAway");
    List<ArmAttachment> samePincers = List.of(ArmAttachment.PINCER, ArmAttachment.PINCER);
    assertThrows(IllegalArgumentException.class, () -> new PackChoice("Melee weapon", oneOption));
    assertThrows(IllegalArgumentException.class, () -> new PackChoice("Aid", sameOptions));
    assertThrows(IllegalArgumentException.class, () -> new PackChoice(" ", options));
    assertThrows(IllegalArgumentException.class, () -> new ArmSlot(List.of()));
    assertThrows(IllegalArgumentException.class, () -> new ArmSlot(samePincers));
    assertTrue(PINCER.fixed());
    assertTrue(PINCER.allows(ArmAttachment.PINCER));
    assertFalse(PINCER.allows(ArmAttachment.FLAMER));
    assertFalse(PINCER.allows(null));
    assertFalse(new ArmSlot(List.of(ArmAttachment.PINCER, ArmAttachment.BUZZ_SAW)).fixed());
  }

  /** Absent optional JSON fields mean: no GM-permission origins, no choices, no arms. */
  @Test
  void absentOptionalFieldsAreEmpty() {
    EquipmentPackData pack = pack(CRB, List.of("vault-dweller"), null, 10, null);
    assertEquals(List.of(), pack.originsWithGmPermission());
    assertEquals(List.of(), pack.choices());
    assertEquals(List.of(), pack.arms());
  }

  /** CRB p. 57: a ghoul vault dweller may take a Vault Dweller pack with the GM's permission. */
  @Test
  void offersPacksToOriginsWithGmPermission() {
    EquipmentPackData pack = pack(CRB, List.of("vault-dweller"), List.of("ghoul"), 10, null);
    assertTrue(pack.isOfferedTo("vault-dweller"));
    assertFalse(pack.needsGmPermission("vault-dweller"));
    assertTrue(pack.isOfferedTo("ghoul"));
    assertTrue(pack.needsGmPermission("ghoul"));
    assertFalse(pack.isOfferedTo("survivor"));
    assertTrue(pack.isAvailableWith(EnumSet.of(Book.CRB)));
    assertFalse(pack.isAvailableWith(Set.of(Book.WOA)));
  }

  private static EquipmentPackData pack(List<Source> sources, List<String> origins,
      @Nullable List<String> gmOrigins, int caps, @Nullable List<ArmSlot> arms) {
    return new EquipmentPackData("x", "X", sources, origins, gmOrigins, List.of("Vault jumpsuit"), null,
        false, caps, arms);
  }
}
