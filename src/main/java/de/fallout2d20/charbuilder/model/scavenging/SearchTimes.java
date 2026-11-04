package de.fallout2d20.charbuilder.model.scavenging;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;

/** How the time of a search is written. */
public final class SearchTimes {

  private SearchTimes() {}

  /** E.g. {@code 45 minutes}, {@code 1 hour 30 minutes}, {@code 30 seconds}. */
  public static @NonNull String text(@NonNull Duration time) {
    List<String> parts = new ArrayList<>();
    addPart(parts, time.toHoursPart(), "hour");
    addPart(parts, time.toMinutesPart(), "minute");
    addPart(parts, time.toSecondsPart(), "second");
    return String.join(" ", parts);
  }

  private static void addPart(@NonNull List<String> parts, int amount, @NonNull String unit) {
    if (amount > 0) {
      parts.add(amount + " " + unit + (amount == 1 ? "" : "s"));
    }
  }
}
