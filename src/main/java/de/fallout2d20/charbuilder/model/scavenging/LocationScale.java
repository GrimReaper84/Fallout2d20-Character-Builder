package de.fallout2d20.charbuilder.model.scavenging;

import java.time.Duration;
import org.jspecify.annotations.NonNull;

/** The size of a location; sets the time a search takes (CRB p. 196). */
public enum LocationScale {
  TINY("Tiny", Duration.ofMinutes(1)),
  SMALL("Small", Duration.ofMinutes(10)),
  AVERAGE("Average", Duration.ofMinutes(30)),
  LARGE("Large", Duration.ofHours(2));

  private final @NonNull String displayName;
  private final @NonNull Duration time;

  LocationScale(@NonNull String displayName, @NonNull Duration time) {
    this.displayName = displayName;
    this.time = time;
  }

  public @NonNull String displayName() {
    return displayName;
  }

  public @NonNull Duration time() {
    return time;
  }
}
