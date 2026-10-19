package de.fallout2d20.charbuilder.model.character.item;

import org.jspecify.annotations.NonNull;

/**
 * A holotape the character carries, named and labelled by the player (user decision 2026-10-19). The book
 * has no holotape entry, only the player to play them (CRB p. 181) and notes or holotapes as finds
 * (CRB p. 207), so a holotape is always described by hand; it weighs nothing (house rule: no weight in the
 * book).
 *
 * @param name what the tape is called, e.g. {@code Vault 81 entry code}
 * @param label free text: its label, contents or code; empty for none
 */
public record Holotape(@NonNull String name, @NonNull String label) {

  /** Name of a holotape just added, until the player names it. */
  public static final @NonNull String NEW_NAME = "New holotape";

  public Holotape {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("A holotape needs a name");
    }
    label = label == null ? "" : label;
  }
}
