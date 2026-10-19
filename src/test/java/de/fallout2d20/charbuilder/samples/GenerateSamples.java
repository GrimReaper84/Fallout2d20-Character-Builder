package de.fallout2d20.charbuilder.samples;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.persistence.CharacterStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Writes the generated {@link SampleCharacters} into the samples folder (E9); started by the Gradle task
 * {@code generateSamples}, never by the build. Files it does not generate (the hand-made Nora Mayfield,
 * own saves dropped into the folder) are left alone.
 */
public final class GenerateSamples {

  private GenerateSamples() {}

  /** @param args the samples folder */
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("Usage: GenerateSamples <samples folder>");
    }
    Path folder = Path.of(args[0]);
    Files.createDirectories(folder);
    for (Path file : write(folder)) {
      System.out.println("Wrote " + file);
    }
  }

  /** Writes every generated sample into the folder; returns the written files. */
  static List<Path> write(Path folder) throws IOException {
    CharacterStore store = new CharacterStore(Clock.systemUTC(), SampleCharacters.VERSION);
    List<Path> written = new ArrayList<>();
    for (Map.Entry<String, Supplier<PlayerCharacter>> sample : SampleCharacters.all().entrySet()) {
      Path file = folder.resolve(sample.getKey());
      store.save(sample.getValue().get(), file);
      written.add(file);
    }
    return written;
  }
}
