package de.fallout2d20.charbuilder.model.character.equipment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;

/**
 * The lines of the gear notes that name amounts of the starting equipment still to roll (user decision
 * 2026-10-19: rolled amounts are no items but own note lines). Each line starts with the prefix of its
 * {@link StartingSource}. When that part of the starting equipment changes, only the lines it wrote before and
 * the player left untouched are replaced; a line the player edited (e.g. the result written behind it) stays,
 * as does everything else.
 */
public final class StartingNotes {

  private StartingNotes() {}

  /**
   * The notes without the untouched lines of {@code oldRolls} that {@code newRolls} no longer names, and with the
   * new lines not yet there appended.
   *
   * @param oldRolls the amounts the source named before, without prefix
   * @param newRolls the amounts it names now, without prefix, e.g. {@code 10 + 5 CD rounds of 10mm ammunition}
   */
  public static @NonNull String rewrite(@NonNull String notes, @NonNull StartingSource source,
      @NonNull List<String> oldRolls, @NonNull List<String> newRolls) {
    Set<String> wanted = withPrefix(source, newRolls);
    Set<String> obsolete = withPrefix(source, oldRolls);
    List<String> lines = new ArrayList<>(notes.isEmpty() ? List.of() : notes.lines()
        .filter(line -> !obsolete.contains(line) || wanted.contains(line)).toList());
    while (!lines.isEmpty() && lines.getLast().isBlank()) {
      lines.removeLast();
    }
    for (String line : wanted) {
      if (!lines.contains(line)) {
        lines.add(line);
      }
    }
    return String.join("\n", lines);
  }

  private static @NonNull Set<String> withPrefix(@NonNull StartingSource source, @NonNull List<String> rolls) {
    return rolls.stream().map(roll -> source.rollPrefix() + roll)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
