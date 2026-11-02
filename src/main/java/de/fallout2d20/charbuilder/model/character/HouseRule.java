package de.fallout2d20.charbuilder.model.character;

import org.jspecify.annotations.NonNull;

/**
 * House rules a group can switch on per character (Character ▸ Rules); saved in the character file and named on the
 * sheet's notes page (user decisions 2026-11-02). Rules that are always on stay in the code (e.g. human apparel on
 * a Protectron).
 */
public enum HouseRule {
  /**
   * The book is not clear here: CRB p. 122 counts helmets as headgear (one item only), but the armor tables list the
   * helmets as pieces of their armor sets (CRB p. 130 ff.), and armor cannot be worn with an outfit. The builder
   * treats armor helmets as armor (user decision 2026-10-22). With this rule an armor piece covering only the head
   * counts as headgear instead: it may be worn with an outfit, but is the one headgear item (user decisions
   * 2026-10-22 and 2026-11-02).
   */
  ARMOR_HELMETS_AS_HEADGEAR("Armor helmets count as headgear",
      "Armor pieces that cover only the head are worn like headgear: allowed with outfits, but never together with "
          + "a hat, mask or other headgear.");

  private final @NonNull String displayName;
  private final @NonNull String description;

  HouseRule(@NonNull String displayName, @NonNull String description) {
    this.displayName = displayName;
    this.description = description;
  }

  public @NonNull String displayName() {
    return displayName;
  }

  /** What the rule changes, in one or two sentences. */
  public @NonNull String description() {
    return description;
  }
}
