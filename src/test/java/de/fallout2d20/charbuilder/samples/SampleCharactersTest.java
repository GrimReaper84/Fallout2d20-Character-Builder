package de.fallout2d20.charbuilder.samples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.persistence.CharacterFile;
import de.fallout2d20.charbuilder.persistence.CharacterStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The Beta menu samples (E9) stay loadable, up to date and in the state their names promise. */
class SampleCharactersTest {

  private static final Path SAMPLES = Path.of("src/test/resources/samples");
  private static final String IN_CREATION = "in-creation.json";

  private final CharacterStore store = new CharacterStore();

  @Test
  void everySampleFileLoadsInTheCurrentFormat() throws Exception {
    assertTrue(Files.isRegularFile(SAMPLES.resolve("nora-mayfield.json")));
    List<Path> files;
    try (Stream<Path> entries = Files.list(SAMPLES)) {
      files = entries.filter(file -> file.toString().endsWith(".json")).toList();
    }
    for (Path file : files) {
      CharacterFile loaded = store.load(file);
      assertNull(loaded.importReport(), file + " must not need a legacy import");
    }
  }

  /**
   * The generator builds every sample without open creation hints (it throws otherwise) and the committed
   * files match its output, so a catalog or rule change cannot leave them stale unnoticed.
   */
  @Test
  void committedSamplesMatchTheGenerator(@TempDir Path dir) throws Exception {
    for (Path generated : GenerateSamples.write(dir)) {
      Path committed = SAMPLES.resolve(generated.getFileName());
      assertTrue(Files.isRegularFile(committed), committed + " missing, run ./gradlew generateSamples");
      assertEquals(Files.readString(generated), Files.readString(committed),
          committed + " is out of date, run ./gradlew generateSamples");
    }
  }

  @Test
  void samplesHaveTheirPromisedState() throws Exception {
    for (String name : SampleCharacters.all().keySet()) {
      assertEquals(!name.equals(IN_CREATION), load(name).isCreationComplete(), name);
    }

    PlayerCharacter pending = load("level-up-pending.json");
    assertEquals(5, pending.advancement().getLevel());
    assertEquals(2, pending.advancement().skillRanksLeft());
    assertEquals(2, pending.advancement().perksLeft());

    PlayerCharacter veteran = load("veteran-overflow.json");
    assertEquals(12, veteran.advancement().getLevel());
    assertTrue(veteran.gear().getItems().size() >= 40);
    assertFalse(veteran.gear().getWeapons().isEmpty());
  }

  private PlayerCharacter load(String name) throws Exception {
    return store.load(SAMPLES.resolve(name)).character();
  }
}
