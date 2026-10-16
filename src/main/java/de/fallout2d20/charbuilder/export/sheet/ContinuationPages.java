package de.fallout2d20.charbuilder.export.sheet;

import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Pages after the two sheet pages: the table rows that did not fit (weapons, ammo, gear, perks &amp;
 * traits) and the notes, flowing from top to bottom and starting a new page whenever the next row does
 * not fit. No page is created when there is nothing to continue.
 */
final class ContinuationPages implements AutoCloseable {

  private static final float NOTE_SIZE = SheetLayout.TEXT_SIZE + 1;
  /** One line at {@link #NOTE_SIZE} with line spacing and cell padding, so notes are not shrunk. */
  private static final float NOTE_LINE_HEIGHT = 15f;

  private final @NonNull PDDocument document;
  private final @NonNull String characterName;
  private @Nullable PdfCanvas canvas;
  private float y;

  ContinuationPages(@NonNull PDDocument document, @NonNull String characterName) {
    this.document = document;
    this.characterName = characterName;
  }

  /** A titled table with the remaining rows; the header row is repeated on every new page. */
  void table(@NonNull String title, @NonNull List<Column> columns, @NonNull List<List<String>> rows,
      float rowHeight) throws IOException {
    if (rows.isEmpty()) {
      return;
    }
    int next = 0;
    boolean first = true;
    while (next < rows.size()) {
      PdfCanvas page = room(SheetLayout.BAR_HEIGHT + SheetTable.height(1, rowHeight));
      int fitting = Math.max(1, (int) ((SheetLayout.BOTTOM - y - SheetLayout.BAR_HEIGHT
          - SheetLayout.HEADER_ROW_HEIGHT) / rowHeight));
      int count = Math.min(fitting, rows.size() - next);
      page.section(SheetLayout.MARGIN, y, SheetLayout.CONTENT_WIDTH, first ? title : title + " (cont.)");
      y = SheetTable.draw(page, SheetLayout.MARGIN, y + SheetLayout.BAR_HEIGHT, SheetLayout.CONTENT_WIDTH, columns,
          rows.subList(next, next + count), count, rowHeight) + SheetLayout.GAP * 2;
      next += count;
      first = false;
    }
  }

  /** The notes as wrapped lines across the page width. */
  void notes(@NonNull List<String> notes) throws IOException {
    if (notes.isEmpty()) {
      return;
    }
    PdfCanvas page = room(SheetLayout.BAR_HEIGHT + NOTE_LINE_HEIGHT);
    page.section(SheetLayout.MARGIN, y, SheetLayout.CONTENT_WIDTH, "NOTES");
    y += SheetLayout.BAR_HEIGHT + 4;
    for (String note : notes) {
      for (String line : PdfCanvas.wrapped(note, NOTE_SIZE, SheetLayout.CONTENT_WIDTH)) {
        page = room(NOTE_LINE_HEIGHT);
        page.text(SheetLayout.MARGIN, y, SheetLayout.CONTENT_WIDTH, NOTE_LINE_HEIGHT, line, PdfCanvas.REGULAR,
            NOTE_SIZE, false);
        y += NOTE_LINE_HEIGHT;
      }
      y += 3;
    }
  }

  /** The current page if {@code needed} points are left on it, otherwise a new one. */
  private @NonNull PdfCanvas room(float needed) throws IOException {
    if (canvas != null && y + needed <= SheetLayout.BOTTOM) {
      return canvas;
    }
    if (canvas != null) {
      canvas.close();
    }
    PdfCanvas page = CharacterSheetPdf.newPage(document, characterName);
    canvas = page;
    y = SheetLayout.MARGIN;
    return page;
  }

  @Override
  public void close() throws IOException {
    if (canvas != null) {
      canvas.close();
    }
  }
}
