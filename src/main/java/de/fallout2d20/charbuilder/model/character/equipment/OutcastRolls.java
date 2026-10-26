package de.fallout2d20.charbuilder.model.character.equipment;

import de.fallout2d20.charbuilder.catalog.equipment.D20TableRow;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentCatalog;
import de.fallout2d20.charbuilder.model.character.Gear;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

/**
 * The rolls on the Outcast Equipment Table (WG p. 19) that the Ex-Knight and Ex-Scribe packs name (user decision
 * 2026-10-26: a dice button). The pack writes them as a roll line into the gear notes ({@link StartingNotes}), e.g.
 * {@code [Pack] Roll: 3 rolls on the Outcast Equipment Table (WG p. 19)}; each roll appends its result to that line
 * ({@code → d20 7: Combat Chest Piece}) and adds the equipment to the inventory as if added by hand, so a later pack
 * change neither removes it nor the edited line. Only the line of the current pack counts: the open rolls of a pack
 * chosen before lapse (user decision 2026-10-26), its line stays as text.
 */
public final class OutcastRolls {

  /** The text of the roll line after the count, as the packs word it. */
  private static final @NonNull String TABLE = "on the Outcast Equipment Table";
  private static final @NonNull String RESULT = StartingNotes.RESULT + "d20 ";
  private static final @NonNull Pattern LINE = Pattern.compile(
      Pattern.quote(StartingSource.PACK.rollPrefix()) + "(\\d+) rolls? " + Pattern.quote(TABLE) + ".*");

  private OutcastRolls() {}

  /** How many rolls of the current pack's line are still open; 0 without such a pack or line. */
  public static int left(@NonNull PlayerCharacter character) {
    return line(character).map(OutcastRolls::open).orElse(0);
  }

  /**
   * Rolls once: adds the equipment of the rolled row to the inventory and its result to the notes line.
   *
   * @return the row rolled
   * @throws IllegalStateException if no roll is open
   */
  public static @NonNull D20TableRow roll(@NonNull PlayerCharacter character,
      @NonNull RandomGenerator random) {
    Gear gear = character.gear();
    String line = line(character).filter(found -> open(found) > 0)
        .orElseThrow(() -> new IllegalStateException("No open roll on the Outcast Equipment Table"));
    D20TableRow row = EquipmentCatalog.outcastEquipment(random.nextInt(D20TableRow.DIE_SIZE) + 1);
    PackItems entries = PackInventory.entries(row.name());
    entries.weapons().forEach(gear::addWeaponStacked);
    entries.apparel().forEach(gear::addApparel);
    entries.items().forEach(gear::addItemStacked);
    String rolled = line + RESULT + row.roll() + ": " + row.name();
    List<String> lines = gear.getNotes().lines().map(note -> note.equals(line) ? rolled : note).toList();
    gear.setNotes(String.join("\n", lines));
    return row;
  }

  /** The notes line of the current pack's Outcast rolls, with the results appended so far; empty if none. */
  private static @NonNull Optional<String> line(@NonNull PlayerCharacter character) {
    Optional<String> wanted = character.startingGear().getPack().stream().flatMap(pack -> pack.items().stream())
        .map(item -> StartingSource.PACK.rollPrefix() + item).filter(item -> LINE.matcher(item).matches())
        .findFirst();
    if (wanted.isEmpty()) {
      return Optional.empty();
    }
    String base = wanted.get();
    return character.gear().getNotes().lines()
        .filter(note -> note.equals(base) || note.startsWith(base + StartingNotes.RESULT)).findFirst();
  }

  /** The rolls the line names minus the results appended to it; 0 for any other line. */
  private static int open(@NonNull String line) {
    Matcher matcher = LINE.matcher(line);
    if (!matcher.matches()) {
      return 0;
    }
    int results = line.split(Pattern.quote(RESULT), -1).length - 1;
    return Math.max(0, Integer.parseInt(matcher.group(1)) - results);
  }
}
