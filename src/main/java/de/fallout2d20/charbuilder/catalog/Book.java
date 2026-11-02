package de.fallout2d20.charbuilder.catalog;

import org.jspecify.annotations.NonNull;

/**
 * The rulebooks this application supports. Catalog entries cite them by their constant name
 * ({@code "CRB"}); content of other books is not supported.
 */
public enum Book {
  CRB("Core Rulebook", "The basic game: character creation and advancement, the Commonwealth origins from Vault "
      + "Dweller to Mister Handy, skills, traits and perks, and the weapons, armor, mods and items of the wasteland. "
      + "Always on, as every other book builds on it."),
  WOA("Winter of Atom", "A campaign book whose player options this builder supports: the Generation 3 Synth, "
      + "Protectron and Child of Atom origins with their equipment packs, and the Cryojet with its mods."),
  WG("Wanderer's Guide", "A player's supplement: the Assaultron, Brotherhood Outcast, Nightkin and Tribal origins "
      + "(and more for the Child of Atom), new traits and perks including rare ones, many weapons, mods, apparel and "
      + "items, legendary weapons and armor, and new equipment packs.");

  private final @NonNull String title;
  private final @NonNull String summary;

  Book(@NonNull String title, @NonNull String summary) {
    this.title = title;
    this.summary = summary;
  }

  public @NonNull String title() {
    return title;
  }

  /** What the book adds to the builder, in a few sentences of own words (Character ▸ Rules). */
  public @NonNull String summary() {
    return summary;
  }
}
