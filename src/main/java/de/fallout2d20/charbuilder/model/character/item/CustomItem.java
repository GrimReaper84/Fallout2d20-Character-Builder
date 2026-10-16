package de.fallout2d20.charbuilder.model.character.item;

import org.jspecify.annotations.NonNull;

/**
 * An item outside the catalog (homebrew, other books, unrecognized equipment pack lines), described by
 * the player.
 *
 * @param name the item's name
 * @param weight weight of one piece in whole lbs.; 0 for light items ({@code <1}, CRB p. 87)
 * @param effects what it does, free text; empty for none
 */
public record CustomItem(@NonNull String name, int weight, @NonNull String effects) {

  public CustomItem {
    if (name == null || name.isBlank() || weight < 0) {
      throw new IllegalArgumentException("A custom item needs a name and a weight of at least 0");
    }
    effects = effects == null ? "" : effects;
  }
}
