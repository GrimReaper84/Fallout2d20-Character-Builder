package de.fallout2d20.charbuilder.view;

import javafx.scene.layout.Region;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The standard MigLayout panes and cell constraints of the views, so all of them use the same spacing. Built
 * with MigLayout's typed constraints ({@link LC}, {@link AC}, {@link CC}) instead of constraint strings; new
 * views take a preset, and one-off layouts use the spacing constants.
 */
public final class Layouts {

  /** Between the rows of a table or a column. */
  public static final int ROW_GAP = 4;
  /** Between the cells of a table and the buttons of a row. */
  public static final int GAP = 8;
  /** Between the rows of a details pane (name, value). */
  public static final int DETAIL_ROW_GAP = 6;
  /** Between name and value of a details pane, the columns of a wide table, and above a new section. */
  public static final int WIDE_GAP = 16;
  /** Around a tab's content. */
  public static final int PAGE_INSETS = 16;
  /** Around a dialog's form. */
  public static final int DIALOG_INSETS = 8;
  /** Indent of a line that belongs to the one above it (e.g. under a checkbox's text: two indents). */
  public static final int INDENT = 12;
  /** Between the two halves of a tab. */
  private static final int HALVES_GAP = 32;

  private Layouts() {}

  /**
   * A tab's four-column grid: name, value, name, value. The columns keep their natural width and the content
   * stays on the left, so values and tables are not pulled apart in a wide window; never squeezed below its
   * natural width (that cut the row names to "…"), a too narrow window scrolls.
   */
  public static @NonNull MigPane page() {
    MigPane pane = new MigPane(gaps(new LC().insetsAll(size(PAGE_INSETS)).wrapAfter(4), GAP, ROW_GAP),
        new AC().align("right", 0, 2).align("left", 1, 3));
    pane.setMinWidth(Region.USE_PREF_SIZE);
    return pane;
  }

  /** Like {@link #page()}, but the value columns share the window width (for the flowing perk cards). */
  public static @NonNull MigPane fillingPage() {
    return new MigPane(gaps(new LC().insetsAll(size(PAGE_INSETS)).wrapAfter(4), GAP, ROW_GAP),
        new AC().align("right", 0, 2).grow(100, 1, 3).fill(1, 3));
  }

  /**
   * Two panes side by side with the same width, sharing the window width (e.g. two {@link #column()}s). Never
   * squeezed below its natural width; a too narrow window scrolls.
   */
  public static @NonNull MigPane halves(@NonNull MigPane left, @NonNull MigPane right) {
    MigPane pane = new MigPane(gaps(new LC().insetsAll(size(PAGE_INSETS)), HALVES_GAP, 0),
        new AC().sizeGroup("half", 0, 1).grow(100, 0, 1).fill(0, 1));
    pane.setMinWidth(Region.USE_PREF_SIZE);
    pane.add(left, top());
    pane.add(right, top());
    return pane;
  }

  /** A single column filling its width: headings, sections, a tree with its buttons. */
  public static @NonNull MigPane column() {
    return new MigPane(gaps(new LC().insetsAll("0").wrapAfter(1), 0, ROW_GAP), new AC().grow().fill());
  }

  /** A row of buttons or small controls side by side. */
  public static @NonNull MigPane row() {
    return new MigPane(gaps(new LC().insetsAll("0"), GAP, 0));
  }

  /** No spacing at all, e.g. to stack a heading and a control. */
  public static @NonNull MigPane bare() {
    return new MigPane(new LC().insetsAll("0"));
  }

  /** A table of {@code columns} left-aligned columns of natural width (S.P.E.C.I.A.L., skills, statistics). */
  public static @NonNull MigPane table(int columns) {
    return new MigPane(gaps(new LC().insetsAll("0").wrapAfter(columns), GAP, ROW_GAP), leftColumns(columns));
  }

  /** Like {@link #table(int)}, the last column taking the rest of the width (e.g. trait descriptions). */
  public static @NonNull MigPane tableWithGrowingLast(int columns) {
    return new MigPane(gaps(new LC().insetsAll("0").wrapAfter(columns), GAP, ROW_GAP),
        leftColumns(columns).grow(100, columns - 1));
  }

  /** Like {@link #table(int)} with wide column gaps (damage resistance table, checkbox grids). */
  public static @NonNull MigPane wideTable(int columns) {
    return new MigPane(gaps(new LC().insetsAll("0").wrapAfter(columns), WIDE_GAP, ROW_GAP), leftColumns(columns));
  }

  /**
   * A details pane: row names left, values filling the rest, plus {@code extraColumns} columns of natural width
   * after the value (e.g. an edit button).
   */
  public static @NonNull MigPane details(int extraColumns) {
    return new MigPane(gaps(new LC().insetsAll("0").wrapAfter(2 + extraColumns), WIDE_GAP, DETAIL_ROW_GAP),
        new AC().align("left", 0).grow(100, 1).fill(1).index(1 + extraColumns));
  }

  /** A dialog's form: right-aligned labels, controls filling the rest. */
  public static @NonNull MigPane dialogForm() {
    return new MigPane(new LC().insetsAll(size(DIALOG_INSETS)).wrapAfter(2),
        new AC().align("right", 0).grow(100, 1).fill(1));
  }

  /** A cell without constraints, to chain MigLayout's own {@link CC} methods. */
  public static @NonNull CC cell() {
    return new CC();
  }

  /** A cell spanning {@code columns} columns. */
  public static @NonNull CC span(int columns) {
    return new CC().spanX(columns);
  }

  /** A cell aligned to the top of its row (multi-line neighbors, the halves of a tab). */
  public static @NonNull CC top() {
    return new CC().alignY("top");
  }

  /** A cell that grows and takes the free space in both directions (a tree in its column). */
  public static @NonNull CC filling() {
    return new CC().grow().push();
  }

  /** A cell indented by {@code levels} × {@link #INDENT}. */
  public static @NonNull CC indented(int levels) {
    return new CC().gapLeft(size(levels * INDENT));
  }

  /** A cell with space above it, starting a new block within a pane. */
  public static @NonNull CC spaced() {
    return spaced(new CC());
  }

  /** A cell that starts a new section, with space above it. */
  public static @NonNull CC sectionStart() {
    return sectionStart(new CC());
  }

  /** Space above a cell that starts a new block within a pane. */
  public static @NonNull CC spaced(@NonNull CC cell) {
    return cell.gapTop(size(GAP));
  }

  /** Space above a cell that starts a new section. */
  public static @NonNull CC sectionStart(@NonNull CC cell) {
    return cell.gapTop(size(WIDE_GAP));
  }

  private static @NonNull LC gaps(@NonNull LC layout, int horizontal, int vertical) {
    return layout.gridGap(size(horizontal), size(vertical));
  }

  /** Index-based {@link AC} methods add the columns they name ({@code AC.count(n)} would add one more). */
  private static @NonNull AC leftColumns(int columns) {
    int[] all = new int[columns];
    for (int i = 0; i < columns; i++) {
      all[i] = i;
    }
    return new AC().align("left", all);
  }

  /** Unitless like the constraint strings, so MigLayout's default unit (logical pixels, scaled with the DPI) applies. */
  private static @NonNull String size(int pixels) {
    return Integer.toString(pixels);
  }
}
