package de.fallout2d20.charbuilder.export.sheet;

import static de.fallout2d20.charbuilder.export.sheet.SheetLayout.BAR_HEIGHT;
import static de.fallout2d20.charbuilder.export.sheet.SheetLayout.GAP;
import static de.fallout2d20.charbuilder.export.sheet.SheetLayout.LABEL_SIZE;
import static de.fallout2d20.charbuilder.export.sheet.SheetLayout.MARGIN;

import de.fallout2d20.charbuilder.AppInfo;
import de.fallout2d20.charbuilder.model.character.Attribute;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.jspecify.annotations.NonNull;

/**
 * Writes the printable character sheet as PDF: page 1 with header, S.P.E.C.I.A.L., skills, combat
 * values, hit locations and weapons, page 2 with caps, ammo, gear, carry weight and perks &amp; traits,
 * then continuation pages for rows that did not fit and for the notes. A plain black-and-white drawing
 * that follows the arrangement of the CRB sheet (CRB pp. 430–431) without any of its artwork; the
 * official sheet is for personal use only and is never reproduced. Pencil fields stay empty.
 */
public final class CharacterSheetPdf {

  private CharacterSheetPdf() {}

  /** Writes the sheet to {@code out}; the stream is not closed. */
  public static void write(@NonNull SheetContent content, @NonNull OutputStream out) throws IOException {
    try (PDDocument document = new PDDocument()) {
      PDDocumentInformation info = document.getDocumentInformation();
      info.setTitle(content.name().isBlank() ? "Character sheet" : content.name());
      info.setCreator(AppInfo.NAME);
      try (PdfCanvas canvas = newPage(document, content.name())) {
        firstPage(canvas, content);
      }
      try (PdfCanvas canvas = newPage(document, content.name())) {
        secondPage(canvas, content);
      }
      try (ContinuationPages more = new ContinuationPages(document, content.name())) {
        more.table("WEAPONS", SheetLayout.WEAPON_COLUMNS, weaponCells(rest(content.weapons(), SheetLayout.WEAPON_ROWS)),
            SheetLayout.CONTINUED_WEAPON_ROW_HEIGHT);
        more.table("AMMO", SheetLayout.AMMO_COLUMNS, ammoCells(rest(content.ammunition(), SheetLayout.AMMO_ROWS)),
            SheetLayout.LIST_ROW_HEIGHT);
        more.table("GEAR", SheetLayout.GEAR_COLUMNS, gearCells(rest(content.gear(), SheetLayout.GEAR_ROWS)),
            SheetLayout.LIST_ROW_HEIGHT);
        more.table("PERKS & TRAITS", SheetLayout.PERK_COLUMNS, perkCells(rest(content.perks(), SheetLayout.PERK_ROWS)),
            SheetLayout.CONTINUED_PERK_ROW_HEIGHT);
        more.notes(content.notes());
      }
      document.save(out);
    }
  }

  /** A new page with the footer (character name, page number, app name). */
  static @NonNull PdfCanvas newPage(@NonNull PDDocument document, @NonNull String characterName) throws IOException {
    PdfCanvas canvas = PdfCanvas.newPage(document);
    String name = characterName.isBlank() ? "" : characterName + " · ";
    canvas.text(MARGIN, SheetLayout.FOOTER_Y, SheetLayout.CONTENT_WIDTH, 10f,
        name + "Page " + document.getNumberOfPages() + " · " + AppInfo.NAME, PdfCanvas.REGULAR, LABEL_SIZE, true);
    return canvas;
  }

  private static void firstPage(@NonNull PdfCanvas canvas, @NonNull SheetContent content) throws IOException {
    header(canvas, content);
    special(canvas, content);
    float y = SheetLayout.SECTION_Y;
    canvas.section(MARGIN, y, SheetLayout.SKILLS_WIDTH, "SKILLS");
    SheetTable.draw(canvas, MARGIN, y + BAR_HEIGHT, SheetLayout.SKILLS_WIDTH, SheetLayout.SKILL_COLUMNS,
        content.skills().stream().map(row -> List.of(row.name() + " [" + row.attribute() + "]",
            row.tagged() ? "x" : "", Integer.toString(row.rank()))).toList(),
        content.skills().size(), SheetLayout.SKILL_ROW_HEIGHT);
    combat(canvas, content);
    hitLocations(canvas, content);
    canvas.section(MARGIN, SheetLayout.WEAPONS_Y, SheetLayout.CONTENT_WIDTH, "WEAPONS");
    SheetTable.draw(canvas, MARGIN, SheetLayout.WEAPONS_Y + BAR_HEIGHT, SheetLayout.CONTENT_WIDTH,
        SheetLayout.WEAPON_COLUMNS, weaponCells(first(content.weapons(), SheetLayout.WEAPON_ROWS)),
        SheetLayout.WEAPON_ROWS, SheetLayout.WEAPON_ROW_HEIGHT);
  }

  private static void header(@NonNull PdfCanvas canvas, @NonNull SheetContent content) throws IOException {
    float y = SheetLayout.HEADER_Y;
    labelledValue(canvas, MARGIN, y, SheetLayout.NAME_WIDTH, SheetLayout.VALUE_HEIGHT, "CHARACTER NAME",
        content.name(), 16f, false);
    String[][] rows = {
      {"XP EARNED", content.xpEarned()},
      {"XP TO NEXT LEVEL", content.xpNextLevel()},
      {"ORIGIN", content.origin()}};
    for (int i = 0; i < rows.length; i++) {
      float rowY = y + i * SheetLayout.XP_ROW_HEIGHT;
      canvas.bar(SheetLayout.XP_X, rowY, SheetLayout.XP_LABEL_WIDTH, SheetLayout.XP_ROW_HEIGHT, rows[i][0], false);
      float valueX = SheetLayout.XP_X + SheetLayout.XP_LABEL_WIDTH;
      float valueWidth = SheetLayout.XP_WIDTH - SheetLayout.XP_LABEL_WIDTH;
      canvas.box(valueX, rowY, valueWidth, SheetLayout.XP_ROW_HEIGHT);
      canvas.text(valueX, rowY, valueWidth, SheetLayout.XP_ROW_HEIGHT, rows[i][1], PdfCanvas.BOLD, 9f, false);
    }
    labelledValue(canvas, SheetLayout.LEVEL_X, y, SheetLayout.LEVEL_WIDTH, SheetLayout.VALUE_HEIGHT, "LEVEL",
        content.level(), 20f, true);
  }

  private static void special(@NonNull PdfCanvas canvas, @NonNull SheetContent content) throws IOException {
    Attribute[] attributes = Attribute.values();
    float width = (SheetLayout.CONTENT_WIDTH - (attributes.length - 1) * SheetLayout.SPECIAL_GAP) / attributes.length;
    for (int i = 0; i < attributes.length; i++) {
      float x = MARGIN + i * (width + SheetLayout.SPECIAL_GAP);
      Integer value = content.attributes().get(attributes[i]);
      labelledValue(canvas, x, SheetLayout.SPECIAL_Y, width, SheetLayout.SPECIAL_VALUE_HEIGHT,
          attributes[i].displayName().toUpperCase(Locale.ROOT), value == null ? "" : value.toString(),
          22f, true);
    }
  }

  /** Melee damage, defense, initiative and the empty Luck points box (with the maximum as caption). */
  private static void combat(@NonNull PdfCanvas canvas, @NonNull SheetContent content) throws IOException {
    float y = SheetLayout.SECTION_Y;
    canvas.section(SheetLayout.RIGHT_X, y, SheetLayout.RIGHT_WIDTH, "COMBAT");
    float width = (SheetLayout.RIGHT_WIDTH - 3 * SheetLayout.COMBAT_GAP) / 4;
    String[][] cells = {
      {"MELEE DAMAGE", content.meleeDamage()},
      {"DEFENSE", content.defense()},
      {"INITIATIVE", content.initiative()},
      {"LUCK POINTS", ""}};
    float top = y + BAR_HEIGHT + 4;
    for (int i = 0; i < cells.length; i++) {
      float x = SheetLayout.RIGHT_X + i * (width + SheetLayout.COMBAT_GAP);
      labelledValue(canvas, x, top, width, SheetLayout.COMBAT_VALUE_HEIGHT, cells[i][0], cells[i][1], 12f, true);
    }
    float luckX = SheetLayout.RIGHT_X + 3 * (width + SheetLayout.COMBAT_GAP);
    canvas.text(luckX, top + BAR_HEIGHT + SheetLayout.COMBAT_VALUE_HEIGHT - 9, width - 2, 9,
        "max " + content.luckPointsMax(), PdfCanvas.REGULAR, LABEL_SIZE, false);
  }

  /**
   * Hit locations arranged like a body: first location (head/optics) top center, second (torso/main
   * body) in the middle, the limbs left and right; poison DR and health fill the top corners.
   */
  private static void hitLocations(@NonNull PdfCanvas canvas, @NonNull SheetContent content) throws IOException {
    float width = SheetLayout.LOCATION_WIDTH;
    float step = width + SheetLayout.LOCATION_GAP;
    float rowStep = SheetLayout.LOCATION_HEIGHT + SheetLayout.LOCATION_GAP;
    float x0 = SheetLayout.RIGHT_X;
    float y0 = SheetLayout.LOCATION_Y;
    labelledValue(canvas, x0, y0, width, SheetLayout.LOCATION_LINE_HEIGHT, "POISON DR", content.poisonDr(), 11f, true);
    health(canvas, x0 + 2 * step, y0, width, content.maxHp());
    int[][] cells = {{1, 0}, {1, 1}, {0, 1}, {2, 1}, {0, 2}, {2, 2}};
    List<HitLocationRow> locations = content.hitLocations();
    for (int i = 0; i < locations.size() && i < cells.length; i++) {
      location(canvas, x0 + cells[i][0] * step, y0 + cells[i][1] * rowStep, width, locations.get(i));
    }
  }

  private static void health(@NonNull PdfCanvas canvas, float x, float y, float width, @NonNull String maxHp)
      throws IOException {
    float line = SheetLayout.LOCATION_LINE_HEIGHT;
    canvas.bar(x, y, width, BAR_HEIGHT, "HEALTH", false);
    canvas.box(x, y + BAR_HEIGHT, width, 2 * line);
    canvas.gridLine(x, y + BAR_HEIGHT + line, x + width, y + BAR_HEIGHT + line);
    float labelWidth = width * 0.65f;
    canvas.text(x, y + BAR_HEIGHT, labelWidth, line, "Maximum HP", PdfCanvas.REGULAR, 7f, false);
    canvas.text(x + labelWidth, y + BAR_HEIGHT, width - labelWidth, line, maxHp, PdfCanvas.BOLD, 11f, true);
    canvas.text(x, y + BAR_HEIGHT + line, labelWidth, line, "Current HP", PdfCanvas.REGULAR, 7f, false);
  }

  /** One location box: title with d20 range, Phys./En./Rad. DR and an empty HP field. */
  private static void location(@NonNull PdfCanvas canvas, float x, float y, float width, @NonNull HitLocationRow row)
      throws IOException {
    float line = SheetLayout.LOCATION_LINE_HEIGHT;
    canvas.bar(x, y, width, BAR_HEIGHT, row.name().toUpperCase(Locale.ROOT) + " (" + row.dieRange() + ")",
        true);
    canvas.box(x, y + BAR_HEIGHT, width, 2 * line);
    canvas.gridLine(x, y + BAR_HEIGHT + line, x + width, y + BAR_HEIGHT + line);
    canvas.gridLine(x + width / 2, y + BAR_HEIGHT, x + width / 2, y + BAR_HEIGHT + 2 * line);
    String[][] cells = {{"Phys. DR", row.physical()}, {"Rad. DR", row.radiation()}, {"En. DR", row.energy()},
      {"HP", ""}};
    float half = width / 2;
    float labelWidth = half * 0.5f;
    for (int i = 0; i < cells.length; i++) {
      int column = i % 2;
      int lineIndex = i / 2;
      float cellX = x + column * half;
      float cellY = y + BAR_HEIGHT + lineIndex * line;
      canvas.text(cellX, cellY, labelWidth, line, cells[i][0], PdfCanvas.REGULAR, LABEL_SIZE, false);
      canvas.text(cellX + labelWidth, cellY, half - labelWidth, line, cells[i][1], PdfCanvas.BOLD, 10f, true);
    }
  }

  private static void secondPage(@NonNull PdfCanvas canvas, @NonNull SheetContent content) throws IOException {
    float left = SheetLayout.LEFT_WIDTH;
    float y = SheetLayout.HEADER_Y;
    labelledValue(canvas, MARGIN, y, left, SheetLayout.CAPS_HEIGHT, "CAPS", "", 12f, true);
    canvas.section(MARGIN, SheetLayout.AMMO_Y, left, "AMMO");
    SheetTable.draw(canvas, MARGIN, SheetLayout.AMMO_Y + BAR_HEIGHT, left, SheetLayout.AMMO_COLUMNS,
        ammoCells(first(content.ammunition(), SheetLayout.AMMO_ROWS)), SheetLayout.AMMO_ROWS,
        SheetLayout.LIST_ROW_HEIGHT);
    canvas.section(MARGIN, SheetLayout.GEAR_Y, left, "GEAR");
    float gearBottom = SheetTable.draw(canvas, MARGIN, SheetLayout.GEAR_Y + BAR_HEIGHT, left,
        SheetLayout.GEAR_COLUMNS, gearCells(first(content.gear(), SheetLayout.GEAR_ROWS)), SheetLayout.GEAR_ROWS,
        SheetLayout.LIST_ROW_HEIGHT);
    String[][] carry = {
      {"CURRENT CARRY WEIGHT", content.carryWeightCurrent()},
      {"MAXIMUM CARRY WEIGHT", content.carryWeightMax()}};
    float labelWidth = SheetLayout.CARRY_LABEL_WIDTH;
    for (int i = 0; i < carry.length; i++) {
      float rowY = gearBottom + GAP / 2 + i * SheetLayout.LIST_ROW_HEIGHT;
      canvas.bar(MARGIN, rowY, labelWidth, SheetLayout.LIST_ROW_HEIGHT, carry[i][0], false);
      canvas.box(MARGIN + labelWidth, rowY, left - labelWidth, SheetLayout.LIST_ROW_HEIGHT);
      canvas.text(MARGIN + labelWidth, rowY, left - labelWidth, SheetLayout.LIST_ROW_HEIGHT, carry[i][1],
          PdfCanvas.BOLD, 9f, true);
    }
    canvas.section(SheetLayout.PERKS_X, y, SheetLayout.PERKS_WIDTH, "PERKS & TRAITS");
    SheetTable.draw(canvas, SheetLayout.PERKS_X, y + BAR_HEIGHT, SheetLayout.PERKS_WIDTH, SheetLayout.PERK_COLUMNS,
        perkCells(first(content.perks(), SheetLayout.PERK_ROWS)), SheetLayout.PERK_ROWS, SheetLayout.PERK_ROW_HEIGHT);
  }

  /** A black label bar with a value box below. */
  private static void labelledValue(@NonNull PdfCanvas canvas, float x, float y, float width, float valueHeight,
      @NonNull String label, @NonNull String value, float size, boolean centered) throws IOException {
    canvas.bar(x, y, width, BAR_HEIGHT, label, centered);
    canvas.box(x, y + BAR_HEIGHT, width, valueHeight);
    canvas.text(x, y + BAR_HEIGHT, width, valueHeight, value, PdfCanvas.BOLD, size, centered);
  }

  private static @NonNull List<List<String>> weaponCells(@NonNull List<WeaponRow> rows) {
    return rows.stream().map(row -> List.of(row.name(), row.skill(), row.targetNumber(), row.tagged() ? "x" : "",
        row.damage(), row.effects(), row.type(), row.rate(), row.range(), row.qualities(), row.ammunition(),
        row.weight())).toList();
  }

  /** Caliber only: the quantity is a pencil field. */
  private static @NonNull List<List<String>> ammoCells(@NonNull List<String> calibers) {
    return calibers.stream().map(caliber -> List.of(caliber, "")).toList();
  }

  private static @NonNull List<List<String>> gearCells(@NonNull List<GearRow> rows) {
    return rows.stream().map(row -> List.of(row.item(), row.pounds())).toList();
  }

  private static @NonNull List<List<String>> perkCells(@NonNull List<PerkRow> rows) {
    return rows.stream().map(row -> List.of(row.name(), row.rank(), row.effect())).toList();
  }

  private static <T> @NonNull List<T> first(@NonNull List<T> list, int count) {
    return list.subList(0, Math.min(count, list.size()));
  }

  private static <T> @NonNull List<T> rest(@NonNull List<T> list, int count) {
    return list.subList(Math.min(count, list.size()), list.size());
  }
}
