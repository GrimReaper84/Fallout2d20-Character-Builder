package de.fallout2d20.charbuilder.catalog.equipment;

import java.util.Arrays;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An arm attachment of a Mister Handy robot.
 *
 * <p>CRB p. 54 (Arm Attachments table): five attachments exist, and a Mister Handy has three of them,
 * set by its equipment pack (CRB p. 55). An arm that features a weapon comes with 20 shots of ammo
 * for it (CRB p. 55); the ammunition names follow the ammunition table (CRB p. 91).
 */
public enum ArmAttachment {
  AUTO_PISTOL_10MM("10mm Auto Pistol", "10mm"),
  BUZZ_SAW("Buzz-Saw", null),
  FLAMER("Flamer", "Flamer Fuel"),
  LASER_EMITTER("Laser Emitter", "Fusion Cell"),
  PINCER("Pincer", null);

  private final @NonNull String displayName;
  private final @Nullable String ammo;

  ArmAttachment(@NonNull String displayName, @Nullable String ammo) {
    this.displayName = displayName;
    this.ammo = ammo;
  }

  /** Name as spelled in the book (and in legacy saves). */
  public @NonNull String displayName() {
    return displayName;
  }

  /** Ammunition the weapon of this arm uses; empty for arms without a ranged weapon. */
  public @NonNull Optional<String> ammo() {
    return Optional.ofNullable(ammo);
  }

  /** The attachment with this book name, ignoring case and surrounding blanks (legacy import). */
  public static @NonNull Optional<ArmAttachment> fromDisplayName(@NonNull String name) {
    String wanted = name.strip();
    return Arrays.stream(values())
        .filter(attachment -> attachment.displayName.equalsIgnoreCase(wanted))
        .findFirst();
  }
}
