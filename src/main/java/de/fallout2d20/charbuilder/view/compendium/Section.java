package de.fallout2d20.charbuilder.view.compendium;

import org.jspecify.annotations.NonNull;

/** The sections of the GM compendium (H2), in the order of its tab row. */
enum Section {
  PARTY("Party"),
  WEAPONS("Weapons"),
  WEAPON_MODS("Weapon mods"),
  AMMUNITION("Ammunition"),
  APPAREL("Apparel"),
  APPAREL_MODS("Apparel mods"),
  ITEMS("Items"),
  LEGENDARY("Legendary"),
  ORIGINS("Origins"),
  TRAITS("Traits"),
  PERKS("Perks"),
  SKILLS("Skills"),
  PACKS("Equipment packs"),
  QUALITIES("Qualities"),
  SCAVENGING("Scavenging"),
  ROLL_TABLES("Roll tables");

  private final @NonNull String title;

  Section(@NonNull String title) {
    this.title = title;
  }

  @NonNull String title() {
    return title;
  }

  /** Whether the section is a catalog list (master–detail), not the party, the scavenging or the roll tables. */
  boolean isCatalog() {
    return this != PARTY && this != SCAVENGING && this != ROLL_TABLES;
  }
}
