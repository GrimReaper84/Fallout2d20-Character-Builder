package de.fallout2d20.charbuilder.model.scavenging;

/**
 * The rolls a location offers in one item category (CRB p. 195).
 *
 * @param min the rolls of a basic success
 * @param max the most rolls AP can reach
 */
public record ItemRange(int min, int max) {

  public ItemRange {
    if (min < 0 || max < min) {
      throw new IllegalArgumentException("Invalid item range " + min + "-" + max);
    }
  }
}
