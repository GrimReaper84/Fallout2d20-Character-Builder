package de.fallout2d20.charbuilder.catalog.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ArmAttachmentTest {

  /** CRB p. 55: only arms featuring a ranged weapon come with ammo (names per CRB p. 91). */
  @Test
  void onlyRangedWeaponArmsUseAmmo() {
    assertEquals(Optional.of("10mm"), ArmAttachment.AUTO_PISTOL_10MM.ammo());
    assertEquals(Optional.of("Flamer Fuel"), ArmAttachment.FLAMER.ammo());
    assertEquals(Optional.of("Fusion Cell"), ArmAttachment.LASER_EMITTER.ammo());
    assertEquals(Optional.empty(), ArmAttachment.BUZZ_SAW.ammo());
    assertEquals(Optional.empty(), ArmAttachment.PINCER.ammo());
  }

  /** The legacy app stored the book names (data/attachments.ts). */
  @Test
  void findsAttachmentsByTheirLegacyNames() {
    List<String> legacyNames = List.of("Pincer", "Buzz-Saw", "Flamer", "Laser Emitter", "10mm Auto Pistol");
    assertEquals(List.of(ArmAttachment.PINCER, ArmAttachment.BUZZ_SAW, ArmAttachment.FLAMER,
        ArmAttachment.LASER_EMITTER, ArmAttachment.AUTO_PISTOL_10MM),
        legacyNames.stream().map(name -> ArmAttachment.fromDisplayName(name).orElseThrow()).toList());
    assertEquals(Optional.of(ArmAttachment.BUZZ_SAW), ArmAttachment.fromDisplayName(" buzz-saw "));
    assertEquals(Optional.empty(), ArmAttachment.fromDisplayName("Hunting Rifle"));
    assertEquals(Optional.empty(), ArmAttachment.fromDisplayName(""));
  }

  @Test
  void displayNamesAreDistinct() {
    assertEquals(ArmAttachment.values().length,
        Arrays.stream(ArmAttachment.values()).map(ArmAttachment::displayName).distinct().count());
  }
}
