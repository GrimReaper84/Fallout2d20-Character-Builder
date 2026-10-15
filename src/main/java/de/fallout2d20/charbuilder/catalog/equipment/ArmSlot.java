package de.fallout2d20.charbuilder.catalog.equipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One of the three arms an equipment pack gives a Mister Handy (CRB p. 77): either a fixed
 * attachment or a choice between several.
 *
 * @param options attachments allowed on this arm; exactly one means the arm is fixed
 */
public record ArmSlot(@JsonProperty(required = true) @NonNull List<ArmAttachment> options) {

  public ArmSlot {
    if (options == null || options.isEmpty() || new HashSet<>(options).size() != options.size()) {
      throw new IllegalArgumentException("Arm slot needs distinct attachment options: " + options);
    }
    options = List.copyOf(options);
  }

  /** Whether the pack fixes this arm, i.e. there is nothing to choose. */
  public boolean fixed() {
    return options.size() == 1;
  }

  /** Whether the pack allows this attachment on the arm; {@code null} (open arm or free weapon) never. */
  public boolean allows(@Nullable ArmAttachment attachment) {
    return attachment != null && options.contains(attachment);
  }
}
