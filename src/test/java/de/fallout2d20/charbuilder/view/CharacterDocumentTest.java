package de.fallout2d20.charbuilder.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CharacterDocumentTest {

  @Test
  void titleNamesTheCharacterAndMarksUnsavedChanges() {
    PlayerCharacter character = PlayerCharacter.createNew();
    CharacterDocument document = CharacterDocument.created(character);
    assertEquals("Unnamed", document.title());

    character.setName("Nora");
    document.markDirty();
    assertEquals("Nora *", document.title());
    assertEquals("Nora", document.displayName());
  }

  @Test
  void aBetaCopyIsMarkedUntilItIsSavedAndHasNoFolder() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.setName("Nora");
    CharacterDocument document = CharacterDocument.betaCopy(character);
    assertEquals("Nora (beta sample)", document.title());
    assertEquals("Nora", document.displayName());
    assertNull(document.file());
    assertNull(document.directory());

    document.markDirty();
    document.markSaved(Path.of("chars", "nora.json"));
    assertEquals("Nora", document.title());
    assertFalse(document.isDirty());
    assertEquals(Path.of("chars").toAbsolutePath(), document.directory());
  }

  @Test
  void aLegacyImportStartsUnsavedWithoutFileInTheLegacyFolder() {
    CharacterDocument document = CharacterDocument.imported(PlayerCharacter.createNew(), Path.of("old", "jc.json"));
    assertTrue(document.isDirty());
    assertNull(document.file());
    assertEquals(Path.of("old").toAbsolutePath(), document.directory());
  }

  @Test
  void aLegacyImportFromTheBetaMenuHasNoFolder() {
    assertNull(CharacterDocument.imported(PlayerCharacter.createNew(), null).directory());
  }

  @Test
  void aLoadedCharacterStartsInItsFilesFolder() {
    CharacterDocument document = CharacterDocument.loaded(PlayerCharacter.createNew(), Path.of("chars", "nora.json"));
    assertFalse(document.isDirty());
    assertEquals(Path.of("chars").toAbsolutePath(), document.directory());
  }

  @Test
  void recognizesItsFileInEveryNotation() {
    CharacterDocument document = CharacterDocument.loaded(PlayerCharacter.createNew(), Path.of("chars", "nora.json"));
    assertTrue(document.isFile(Path.of("chars", ".", "other", "..", "nora.json")));
    assertTrue(document.isFile(Path.of("chars", "nora.json").toAbsolutePath()));
    assertFalse(document.isFile(Path.of("chars", "jc.json")));
  }

  @Test
  void aCharacterWithoutFileBelongsToNoFile() {
    assertFalse(CharacterDocument.created(PlayerCharacter.createNew()).isFile(Path.of("nora.json")));
  }
}
