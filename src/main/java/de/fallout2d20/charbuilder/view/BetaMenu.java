package de.fallout2d20.charbuilder.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.jspecify.annotations.NonNull;

/**
 * Beta menu for development (E9): opens the test characters under {@code samples/} and the legacy
 * fixtures under {@code legacy/} of the folder named by the system property {@value #PROPERTY}. Only the
 * Gradle {@code run} task sets the property, so release builds never show the menu. The folders are read
 * when the menu is built and again on "Reload list", so files dropped in later show up.
 */
final class BetaMenu {

  /** System property with the folder that holds {@code samples/} and {@code legacy/}. */
  static final @NonNull String PROPERTY = "charbuilder.beta";

  private static final @NonNull String SAMPLES = "samples";
  private static final @NonNull String LEGACY = "legacy";

  private BetaMenu() {}

  /** The folder named by {@value #PROPERTY}, if the property is set and the folder exists. */
  static @NonNull Optional<Path> folder() {
    String value = System.getProperty(PROPERTY);
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    Path folder = Path.of(value);
    if (!Files.isDirectory(folder)) {
      System.err.println("Beta mode off: " + PROPERTY + " folder not found: " + folder.toAbsolutePath());
      return Optional.empty();
    }
    return Optional.of(folder);
  }

  /**
   * The Beta menu with its two submenus.
   *
   * @param openSample opens a sample as an unsaved copy
   * @param openLegacy opens a legacy file like File → Open (with its import report)
   */
  static @NonNull Menu create(@NonNull Path folder, @NonNull Consumer<Path> openSample,
      @NonNull Consumer<Path> openLegacy) {
    Menu samples = new Menu("Samples");
    Menu legacy = new Menu("Legacy imports");
    MenuItem reload = new MenuItem("Reload list");
    reload.setOnAction(_ -> fill(folder, samples, legacy, openSample, openLegacy));
    fill(folder, samples, legacy, openSample, openLegacy);
    return new Menu("Beta", null, samples, legacy, new SeparatorMenuItem(), reload);
  }

  private static void fill(@NonNull Path folder, @NonNull Menu samples, @NonNull Menu legacy,
      @NonNull Consumer<Path> openSample, @NonNull Consumer<Path> openLegacy) {
    samples.getItems().setAll(items(folder.resolve(SAMPLES), BetaMenu::label, openSample));
    legacy.getItems().setAll(items(folder.resolve(LEGACY), file -> file.getFileName().toString(), openLegacy));
  }

  private static @NonNull List<MenuItem> items(@NonNull Path dir,
      @NonNull Function<Path, String> label, @NonNull Consumer<Path> open) {
    List<Path> files;
    try {
      files = files(dir);
    } catch (IOException e) {
      MenuItem error = new MenuItem("Folder not readable: " + e.getMessage());
      error.setDisable(true);
      return List.of(error);
    }
    if (files.isEmpty()) {
      MenuItem none = new MenuItem("No files in " + dir.getFileName());
      none.setDisable(true);
      return List.of(none);
    }
    return files.stream().map(file -> {
      MenuItem item = new MenuItem(label.apply(file));
      item.setOnAction(_ -> open.accept(file));
      return item;
    }).toList();
  }

  /** The {@code *.json} files of a folder, sorted by name; empty if the folder does not exist. */
  static @NonNull List<Path> files(@NonNull Path dir) throws IOException {
    if (!Files.isDirectory(dir)) {
      return List.of();
    }
    try (Stream<Path> entries = Files.list(dir)) {
      return entries.filter(Files::isRegularFile)
          .filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
          .sorted().toList();
    }
  }

  /** Menu label of a sample, e.g. {@code nora-mayfield.json} → {@code Nora Mayfield}. */
  static @NonNull String label(@NonNull Path file) {
    String name = file.getFileName().toString();
    String base = name.toLowerCase(Locale.ROOT).endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    return Arrays.stream(base.split("[-_ ]+")).filter(word -> !word.isEmpty())
        .map(BetaMenu::capitalize).collect(Collectors.joining(" "));
  }

  private static @NonNull String capitalize(@NonNull String word) {
    return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
  }
}
