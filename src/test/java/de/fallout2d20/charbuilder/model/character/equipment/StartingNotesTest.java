package de.fallout2d20.charbuilder.model.character.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Roll lines of the starting equipment in the gear notes (user decision 2026-10-19). */
class StartingNotesTest {

  @Test
  void appendsToEmptyNotes() {
    assertEquals("[Pack] Roll: 2d20 caps",
        StartingNotes.rewrite("", StartingSource.PACK, List.of(), List.of("2d20 caps")));
  }

  @Test
  void replacesOnlyUntouchedLinesOfTheSource() {
    String notes = """
        Found a map
        [Pack] Roll: 6 + 3 CD rounds
        [Pack] Roll: 1 roll on the food table = Iguana
        [Pack] Roll: 2 rolls on the chem table
        [Tag skill] Roll: 4 + 2 CD bobby pins

        """;
    assertEquals("""
        Found a map
        [Pack] Roll: 1 roll on the food table = Iguana
        [Pack] Roll: 2 rolls on the chem table
        [Tag skill] Roll: 4 + 2 CD bobby pins
        [Pack] Roll: 8 + 4 CD rounds""", StartingNotes.rewrite(notes, StartingSource.PACK,
        List.of("6 + 3 CD rounds", "1 roll on the food table", "2 rolls on the chem table"),
        List.of("8 + 4 CD rounds", "2 rolls on the chem table")), "the line with a result stays, a kept one stays put");
  }

  @Test
  void unchangedRollsKeepTheirPlace() {
    String notes = "[Pack] Roll: 6 + 3 CD rounds\nBought a map";
    assertEquals(notes, StartingNotes.rewrite(notes, StartingSource.PACK, List.of("6 + 3 CD rounds"),
        List.of("6 + 3 CD rounds")));
  }
}
