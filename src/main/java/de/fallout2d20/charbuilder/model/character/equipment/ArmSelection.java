package de.fallout2d20.charbuilder.model.character.equipment;

import de.fallout2d20.charbuilder.catalog.equipment.ArmAttachment;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * What one arm of a Mister Handy carries: an attachment from the Arm Attachments table (CRB p. 54),
 * or – with freely configurable arms, a house-rule option beyond the book – a weapon named by the
 * player. Neither means the arm is still open.
 *
 * @param attachment the attachment, or {@code null}
 * @param weapon the free weapon's name (stripped; blank counts as none), or {@code null}
 */
public record ArmSelection(@Nullable ArmAttachment attachment, @Nullable String weapon) {

  public static final @NonNull ArmSelection OPEN = new ArmSelection(null, null);

  public ArmSelection {
    weapon = weapon == null || weapon.isBlank() ? null : weapon.strip();
    if (attachment != null && weapon != null) {
      throw new IllegalArgumentException("An arm carries an attachment or a weapon, not both");
    }
  }

  public static @NonNull ArmSelection of(@NonNull ArmAttachment attachment) {
    return new ArmSelection(attachment, null);
  }

  /** An arm carrying a freely chosen weapon; a blank name gives an open arm. */
  public static @NonNull ArmSelection ofWeapon(@NonNull String weapon) {
    return new ArmSelection(null, weapon);
  }

  public boolean isOpen() {
    return attachment == null && weapon == null;
  }

  public boolean isPincer() {
    return attachment == ArmAttachment.PINCER;
  }
}
