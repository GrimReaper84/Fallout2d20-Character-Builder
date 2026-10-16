package de.fallout2d20.charbuilder.export.sheet;

/** How a table column shows its cells. */
enum ColumnKind {
  /** Text, left aligned. */
  TEXT,
  /** Short value, centered. */
  CENTERED,
  /** A Tag box, filled when the cell text is not empty. */
  CHECKBOX
}
