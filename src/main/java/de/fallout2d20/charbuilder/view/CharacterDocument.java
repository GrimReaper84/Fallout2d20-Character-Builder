package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One open character of the main window (H1): the character, its file, the folder its file dialogs start in and
 * whether it has unsaved changes. A character imported from a legacy file has no file path, so its first save always
 * asks for a new file; the legacy file is never overwritten. Characters from the Beta menu (E9) have neither file nor
 * folder, so the test fixtures are never overwritten or proposed.
 */
final class CharacterDocument {

  private final @NonNull PlayerCharacter character;
  private @Nullable Path file;
  private @Nullable Path directory;
  private boolean dirty;
  /** Whether the character is an unsaved copy of a Beta menu sample (E9). */
  private boolean betaSample;

  private CharacterDocument(@NonNull PlayerCharacter character, @Nullable Path file, @Nullable Path directory,
      boolean dirty, boolean betaSample) {
    this.character = character;
    this.file = file == null ? null : normalized(file);
    this.directory = directory == null ? null : normalized(directory);
    this.dirty = dirty;
    this.betaSample = betaSample;
  }

  /** A new character: no file yet. */
  static @NonNull CharacterDocument created(@NonNull PlayerCharacter character) {
    return new CharacterDocument(character, null, null, false, false);
  }

  /** A character loaded from its file. */
  static @NonNull CharacterDocument loaded(@NonNull PlayerCharacter character, @NonNull Path file) {
    return new CharacterDocument(character, file, normalized(file).getParent(), false, false);
  }

  /**
   * A character imported from a legacy file: unsaved, without file; its dialogs start in the legacy file's folder.
   *
   * @param legacyFile the legacy file; null for one from the Beta menu (its folder is never proposed)
   */
  static @NonNull CharacterDocument imported(@NonNull PlayerCharacter character, @Nullable Path legacyFile) {
    return new CharacterDocument(character, null, legacyFile == null ? null : normalized(legacyFile).getParent(),
        true, false);
  }

  /** An unsaved copy of a Beta menu file (E9): without file and folder. */
  static @NonNull CharacterDocument betaCopy(@NonNull PlayerCharacter character) {
    return new CharacterDocument(character, null, null, false, true);
  }

  @NonNull PlayerCharacter character() {
    return character;
  }

  @Nullable Path file() {
    return file;
  }

  /** The folder the character's file dialogs start in: its file's folder, or the imported legacy file's folder. */
  @Nullable Path directory() {
    return directory;
  }

  boolean isDirty() {
    return dirty;
  }

  void markDirty() {
    dirty = true;
  }

  /** The character was written to the file: it is the character's file from now on. */
  void markSaved(@NonNull Path target) {
    file = normalized(target);
    directory = file.getParent();
    dirty = false;
    betaSample = false;
  }

  /** Whether the character belongs to this file (the same path, however it was written). */
  boolean isFile(@NonNull Path path) {
    return normalized(path).equals(file);
  }

  /** The character's name, or "Unnamed". */
  @NonNull String displayName() {
    return character.getName().isBlank() ? "Unnamed" : character.getName();
  }

  /** The tab and window title: the name, a beta sample marked, "*" for unsaved changes. */
  @NonNull String title() {
    return displayName() + (betaSample ? " (beta sample)" : "") + (dirty ? " *" : "");
  }

  private static @NonNull Path normalized(@NonNull Path path) {
    return path.toAbsolutePath().normalize();
  }
}
