package de.fallout2d20.charbuilder.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BetaMenuTest {

  @Test
  void listsOnlyJsonFilesSortedByName(@TempDir Path dir) throws Exception {
    Files.writeString(dir.resolve("b-sample.json"), "{}");
    Files.writeString(dir.resolve("a-sample.JSON"), "{}");
    Files.writeString(dir.resolve("notes.txt"), "");
    Files.createDirectory(dir.resolve("folder.json"));

    assertEquals(List.of(dir.resolve("a-sample.JSON"), dir.resolve("b-sample.json")), BetaMenu.files(dir));
  }

  @Test
  void missingFolderListsNothing(@TempDir Path dir) throws Exception {
    assertTrue(BetaMenu.files(dir.resolve("missing")).isEmpty());
  }

  @Test
  void labelsAreReadableNames() {
    assertEquals("Nora Mayfield", BetaMenu.label(Path.of("nora-mayfield.json")));
    assertEquals("Gen 3 Synth", BetaMenu.label(Path.of("gen-3-synth.json")));
    assertEquals("Level Up Pending", BetaMenu.label(Path.of("level_up-pending.json")));
  }
}
