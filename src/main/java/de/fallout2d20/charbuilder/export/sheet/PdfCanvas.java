package de.fallout2d20.charbuilder.export.sheet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.jspecify.annotations.NonNull;

/**
 * Drawing on one sheet page with the origin at the top left (y grows downwards, unlike PDF): boxes,
 * black header bars and text that is fitted into a cell. Text uses the built-in Helvetica, so no font
 * file is shipped; characters outside its WinAnsi encoding are replaced (see {@link #printable}).
 */
final class PdfCanvas implements AutoCloseable {

  static final @NonNull PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  static final @NonNull PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

  private static final float LINE_WIDTH = 0.6f;
  private static final float GRID_GRAY = 0.55f;
  private static final float SECTION_GRAY = 0.4f;
  private static final float LEADING = 1.15f;
  /** Distance from the top of a line to its baseline, as a share of the font size. */
  private static final float ASCENT = 0.8f;
  private static final float PADDING = 2f;
  private static final float SIZE_STEP = 0.5f;
  private static final @NonNull String ELLIPSIS = "…";
  private static final @NonNull Map<Integer, String> REPLACEMENTS = Map.of(
      0x2212, "-", 0x2011, "-", 0x2009, " ", 0x202F, " ", 0x2264, "<=", 0x2265, ">=", 0x2192, "->", 0x25CF, "•");

  private final @NonNull PDPageContentStream stream;
  private final float height;

  private PdfCanvas(@NonNull PDPageContentStream stream, float height) {
    this.stream = stream;
    this.height = height;
  }

  /** Adds an A4 page to the document and opens it for drawing. */
  static @NonNull PdfCanvas newPage(@NonNull PDDocument document) throws IOException {
    PDPage page = new PDPage(PDRectangle.A4);
    document.addPage(page);
    PdfCanvas canvas = new PdfCanvas(new PDPageContentStream(document, page), PDRectangle.A4.getHeight());
    canvas.stream.setLineWidth(LINE_WIDTH);
    return canvas;
  }

  /** Outline of a box. */
  void box(float x, float y, float width, float boxHeight) throws IOException {
    stream.setStrokingColor(0f);
    stream.addRect(x, height - y - boxHeight, width, boxHeight);
    stream.stroke();
  }

  /** A thin grey line, used for table grids. */
  void gridLine(float x1, float y1, float x2, float y2) throws IOException {
    stream.setStrokingColor(GRID_GRAY);
    stream.moveTo(x1, height - y1);
    stream.lineTo(x2, height - y2);
    stream.stroke();
    stream.setStrokingColor(0f);
  }

  /** A black bar with a white bold label, as field labels and table headers. */
  void bar(float x, float y, float width, float barHeight, @NonNull String label, boolean centered)
      throws IOException {
    filledBar(x, y, width, barHeight, label, centered, 0f);
  }

  /** A dark grey bar with a white bold label as section title, set apart from the black table header. */
  void section(float x, float y, float width, @NonNull String label) throws IOException {
    filledBar(x, y, width, SheetLayout.BAR_HEIGHT, label, false, SECTION_GRAY);
  }

  private void filledBar(float x, float y, float width, float barHeight, @NonNull String label, boolean centered,
      float gray) throws IOException {
    stream.setNonStrokingColor(gray);
    stream.addRect(x, height - y - barHeight, width, barHeight);
    stream.fill();
    stream.setNonStrokingColor(1f);
    text(x, y, width, barHeight, label, BOLD, Math.min(7f, (barHeight - 2 * PADDING) / LEADING), centered);
    stream.setNonStrokingColor(0f);
  }

  /** A square Tag box centered in a cell, filled when {@code checked}. */
  void checkbox(float x, float y, float width, float cellHeight, boolean checked) throws IOException {
    float size = Math.min(8f, cellHeight - 4);
    float left = x + (width - size) / 2;
    float top = y + (cellHeight - size) / 2;
    box(left, top, size, size);
    if (checked) {
      stream.addRect(left + 1.5f, height - top - size + 1.5f, size - 3, size - 3);
      stream.fill();
    }
  }

  /**
   * Text fitted into a cell: shrinks from {@code maxSize} down to a readable minimum, wraps at word
   * boundaries, and as a last resort cuts the last line with "…". The block is centered vertically.
   */
  void text(float x, float y, float width, float cellHeight, @NonNull String text, @NonNull PDFont font,
      float maxSize, boolean centered) throws IOException {
    String clean = printable(text).strip();
    if (clean.isEmpty()) {
      return;
    }
    float innerWidth = width - 2 * PADDING;
    float innerHeight = cellHeight - 2 * PADDING;
    float minSize = Math.min(maxSize, SheetLayout.MIN_FONT_SIZE);
    float size = maxSize;
    while (longestWord(clean, font, size) > innerWidth && size - SIZE_STEP >= minSize) {
      size -= SIZE_STEP;
    }
    List<String> lines = wrap(clean, font, size, innerWidth);
    while (lines.size() * size * LEADING > innerHeight && size - SIZE_STEP >= minSize) {
      size -= SIZE_STEP;
      lines = wrap(clean, font, size, innerWidth);
    }
    int fitting = Math.max(1, (int) Math.floor(innerHeight / (size * LEADING)));
    if (lines.size() > fitting) {
      List<String> cut = new ArrayList<>(lines.subList(0, fitting));
      cut.set(fitting - 1, ellipsize(cut.get(fitting - 1), font, size, innerWidth));
      lines = cut;
    }
    float blockHeight = lines.size() * size * LEADING;
    float top = y + PADDING + Math.max(0, (innerHeight - blockHeight) / 2);
    stream.setFont(font, size);
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      float lineWidth = width(line, font, size);
      float left = centered ? x + (width - lineWidth) / 2 : x + PADDING;
      float baseline = top + i * size * LEADING + size * ASCENT;
      stream.beginText();
      stream.newLineAtOffset(left, height - baseline);
      stream.showText(line);
      stream.endText();
    }
  }

  @Override
  public void close() throws IOException {
    stream.close();
  }

  /**
   * The text with every character Helvetica (WinAnsi) cannot show replaced: a few known signs by a
   * close equivalent (minus sign by a hyphen, ≥ by "&gt;=", …), anything else by "?". Line breaks
   * are kept for wrapping, tabs become spaces.
   */
  static @NonNull String printable(@NonNull String text) {
    StringBuilder result = new StringBuilder(text.length());
    text.codePoints().forEach(codePoint -> {
      if (codePoint == '\n') {
        result.append('\n');
      } else if (codePoint == '\t' || codePoint == '\r') {
        result.append(' ');
      } else if (REPLACEMENTS.containsKey(codePoint)) {
        result.append(REPLACEMENTS.get(codePoint));
      } else {
        String character = new String(java.lang.Character.toChars(codePoint));
        result.append(encodable(character) ? character : "?");
      }
    });
    return result.toString();
  }

  private static boolean encodable(@NonNull String character) {
    try {
      REGULAR.encode(character);
      return true;
    } catch (IllegalArgumentException | IOException e) {
      return false;
    }
  }

  /** Regular text broken into the lines a cell of {@code width} shows at {@code size}, without shrinking. */
  static @NonNull List<String> wrapped(@NonNull String text, float size, float width) throws IOException {
    return wrap(printable(text).strip(), REGULAR, size, width - 2 * PADDING);
  }

  /** Lines of at most {@code width}; explicit line breaks are kept, over-long words are split. */
  private static @NonNull List<String> wrap(@NonNull String text, @NonNull PDFont font, float size, float width)
      throws IOException {
    List<String> lines = new ArrayList<>();
    for (String paragraph : text.split("\n", -1)) {
      StringBuilder line = new StringBuilder();
      for (String word : paragraph.strip().split(" +", -1)) {
        String candidate = line.isEmpty() ? word : line + " " + word;
        if (width(candidate, font, size) <= width) {
          line.setLength(0);
          line.append(candidate);
          continue;
        }
        if (!line.isEmpty()) {
          lines.add(line.toString());
          line.setLength(0);
        }
        String rest = word;
        while (width(rest, font, size) > width && rest.length() > 1) {
          int cut = rest.length() - 1;
          while (cut > 1 && width(rest.substring(0, cut), font, size) > width) {
            cut--;
          }
          lines.add(rest.substring(0, cut));
          rest = rest.substring(cut);
        }
        line.append(rest);
      }
      lines.add(line.toString());
    }
    return lines;
  }

  /** Width of the widest word, so text shrinks before a word has to be split. */
  private static float longestWord(@NonNull String text, @NonNull PDFont font, float size) throws IOException {
    float widest = 0;
    for (String word : text.split("[ \n]+", -1)) {
      widest = Math.max(widest, width(word, font, size));
    }
    return widest;
  }

  private static @NonNull String ellipsize(@NonNull String line, @NonNull PDFont font, float size, float width)
      throws IOException {
    String shortened = line;
    while (!shortened.isEmpty() && width(shortened + ELLIPSIS, font, size) > width) {
      shortened = shortened.substring(0, shortened.length() - 1);
    }
    return shortened.stripTrailing() + ELLIPSIS;
  }

  private static float width(@NonNull String text, @NonNull PDFont font, float size) throws IOException {
    return font.getStringWidth(text) / 1000f * size;
  }
}
