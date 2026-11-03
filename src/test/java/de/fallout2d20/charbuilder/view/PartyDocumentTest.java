package de.fallout2d20.charbuilder.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import de.fallout2d20.charbuilder.model.party.Party;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PartyDocumentTest {

  @Test
  void titleNamesThePartyAndMarksUnsavedChanges() {
    Party party = Party.createNew("1.5.0");
    PartyDocument document = PartyDocument.created(party);
    assertEquals("GM", document.title());
    assertEquals("Party", document.displayName());

    party.setName("Commonwealth");
    document.markDirty();
    assertEquals("GM – Commonwealth *", document.title());

    document.markSaved(Path.of("party.json"));
    assertFalse(document.isDirty());
    assertEquals("GM – Commonwealth", document.title());
    assertEquals(Path.of("party.json").toAbsolutePath(), document.file());
  }
}
