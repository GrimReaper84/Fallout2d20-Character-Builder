package de.fallout2d20.charbuilder.export.sheet;

import java.util.List;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.jspecify.annotations.NonNull;

/**
 * Positions and sizes of the sheet in points (1/72 inch) on A4, measured from the top left. The
 * arrangement follows the CRB sheet (CRB pp. 430–431); the number of weapon, gear and perk rows is
 * larger than on the book's sheet where the plain layout leaves the room.
 */
final class SheetLayout {

  static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
  static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
  static final float MARGIN = 28f;
  static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
  static final float BOTTOM = PAGE_HEIGHT - MARGIN;
  static final float GAP = 8f;
  static final float BAR_HEIGHT = 12f;
  static final float HEADER_ROW_HEIGHT = 13f;
  static final float MIN_FONT_SIZE = 5f;
  static final float TEXT_SIZE = 8f;
  static final float LABEL_SIZE = 6f;
  static final float FOOTER_Y = PAGE_HEIGHT - 16f;

  // Page 1: header
  static final float HEADER_Y = 28f;
  static final float NAME_WIDTH = 210f;
  static final float VALUE_HEIGHT = 36f;
  static final float XP_X = MARGIN + NAME_WIDTH + GAP;
  static final float XP_WIDTH = 220f;
  static final float XP_LABEL_WIDTH = 92f;
  static final float XP_ROW_HEIGHT = 16f;
  static final float LEVEL_X = XP_X + XP_WIDTH + GAP;
  static final float LEVEL_WIDTH = MARGIN + CONTENT_WIDTH - LEVEL_X;

  // Page 1: S.P.E.C.I.A.L.
  static final float SPECIAL_Y = 88f;
  static final float SPECIAL_GAP = 6f;
  static final float SPECIAL_VALUE_HEIGHT = 40f;

  // Page 1: skills (left) and combat with hit locations (right)
  static final float SECTION_Y = 152f;
  static final float SKILLS_WIDTH = NAME_WIDTH;
  static final float SKILL_ROW_HEIGHT = 17f;
  static final float RIGHT_X = MARGIN + SKILLS_WIDTH + GAP;
  static final float RIGHT_WIDTH = MARGIN + CONTENT_WIDTH - RIGHT_X;
  static final float COMBAT_GAP = 6f;
  static final float COMBAT_VALUE_HEIGHT = 22f;
  static final float LOCATION_Y = 212f;
  static final float LOCATION_GAP = 8f;
  static final float LOCATION_LINE_HEIGHT = 20f;
  static final float LOCATION_HEIGHT = BAR_HEIGHT + 2 * LOCATION_LINE_HEIGHT;
  static final float LOCATION_WIDTH = (RIGHT_WIDTH - 2 * LOCATION_GAP) / 3;

  // Page 1: weapons
  static final float WEAPONS_Y = 481f;
  static final int WEAPON_ROWS = 7;
  static final float WEAPON_ROW_HEIGHT = 43f;
  static final float CONTINUED_WEAPON_ROW_HEIGHT = 30f;

  // Page 2: caps, ammo and gear (left), perks & traits (right)
  static final float LEFT_WIDTH = 200f;
  static final float PERKS_X = MARGIN + LEFT_WIDTH + 10f;
  static final float PERKS_WIDTH = MARGIN + CONTENT_WIDTH - PERKS_X;
  static final float CAPS_HEIGHT = 26f;
  static final float AMMO_Y = 76f;
  static final int AMMO_ROWS = 10;
  static final float LIST_ROW_HEIGHT = 16f;
  static final float GEAR_Y = 276f;
  static final int GEAR_ROWS = 29;
  static final float CARRY_LABEL_WIDTH = 160f;
  static final int PERK_ROWS = 18;
  static final float PERK_ROW_HEIGHT = 42f;
  static final float CONTINUED_PERK_ROW_HEIGHT = 30f;

  static final @NonNull List<Column> SKILL_COLUMNS = List.of(
      Column.text("NAME", 140), Column.checkbox("TAG", 30), Column.centered("RANK", 40));
  static final @NonNull List<Column> WEAPON_COLUMNS = List.of(
      Column.text("NAME", 92), Column.text("SKILL", 52), Column.centered("TN", 22), Column.checkbox("TAG", 20),
      Column.centered("DAMAGE", 42), Column.text("EFFECTS", 60), Column.centered("TYPE", 38),
      Column.centered("RATE", 24), Column.centered("RANGE", 30), Column.text("QUALITIES", 80),
      Column.text("AMMO", 50), Column.centered("WEIGHT", 29));
  static final @NonNull List<Column> AMMO_COLUMNS = List.of(
      Column.text("CALIBER", 150), Column.centered("QUANTITY", 50));
  static final @NonNull List<Column> GEAR_COLUMNS = List.of(Column.text("ITEM", 160), Column.centered("LBS.", 40));
  static final @NonNull List<Column> PERK_COLUMNS = List.of(
      Column.text("NAME", 80), Column.centered("RANK", 28), Column.text("EFFECT", 221));

  private SheetLayout() {}
}
