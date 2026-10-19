package de.fallout2d20.charbuilder.export.sheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.CustomItem;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/** The PDF sheet is written, can be read back and contains the character's values. */
class CharacterSheetPdfTest {

  private static PlayerCharacter survivor() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.setName("Nora");
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.abilities().setFreeTag(Skill.SMALL_GUNS, true);
    character.chooseTrait(CatalogRef.of("educated"));
    character.addPerkRank(CatalogRef.of("awareness"), null, null);
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    return character;
  }

  private static PDDocument written(PlayerCharacter character) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CharacterSheetPdf.write(SheetContent.of(character), out);
    return Loader.loadPDF(out.toByteArray());
  }

  private static String text(PDDocument document) throws IOException {
    return new PDFTextStripper().getText(document);
  }

  @Test
  void twoPagesWithTheCharacterValues() throws IOException {
    PlayerCharacter character = survivor();
    character.gear().setCaps(98765);
    try (PDDocument document = written(character)) {
      assertEquals(2, document.getNumberOfPages());
      String text = text(document);
      assertTrue(text.contains("Nora"));
      assertTrue(text.contains("STRENGTH"));
      assertTrue(text.contains("10mm Pistol"));
      assertTrue(text.contains("Awareness"));
      assertTrue(text.contains("Educated"));
      assertTrue(text.contains("LEFT LEG (15"));
      assertFalse(text.contains("98765"), "caps are a pencil field");
    }
  }

  /** User decision 2026-10-16: what does not fit goes onto an extra page, with the notes. */
  @Test
  void overflowAndNotesGoOntoAnExtraPage() throws IOException {
    PlayerCharacter character = survivor();
    for (int i = 1; i <= SheetLayout.WEAPON_ROWS + 2; i++) {
      character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("baseball-bat")).withNotes("Bat number " + i));
    }
    try (PDDocument document = written(character)) {
      assertEquals(3, document.getNumberOfPages());
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setStartPage(3);
      String third = stripper.getText(document);
      assertTrue(third.contains("WEAPONS"));
      assertTrue(third.contains("Baseball Bat"));
      assertTrue(third.contains("NOTES"));
      assertTrue(third.contains("Bat number 9"));
    }
  }

  @Test
  void manyNotesFlowOntoFurtherPages() throws IOException {
    PlayerCharacter character = survivor();
    character.gear().setNotes("A line of notes.\n".repeat(150));
    try (PDDocument document = written(character)) {
      assertTrue(document.getNumberOfPages() >= 4);
    }
  }

  /** Helvetica only knows WinAnsi: other characters are replaced instead of breaking the export. */
  @Test
  void charactersOutsideWinAnsiAreReplaced() throws IOException {
    assertEquals("-1 difficulty, >= 2 × 3 ?", PdfCanvas.printable("−1 difficulty, ≥ 2 × 3 \uD83D\uDE00"));
    PlayerCharacter character = survivor();
    character.setName("Zoë −1 中");
    character.gear().addItem(CarriedItem.of(new CustomItem("Lucky charm ●", 0, "")));
    try (PDDocument document = written(character)) {
      String text = text(document);
      assertTrue(text.contains("Zoë -1 ?"));
      assertTrue(text.contains("Lucky charm •"));
    }
  }
}
