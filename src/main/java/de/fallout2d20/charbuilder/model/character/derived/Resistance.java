package de.fallout2d20.charbuilder.model.character.derived;

import org.jspecify.annotations.NonNull;

/**
 * Innate Damage Resistance against one damage type, or immunity to it.
 *
 * <p>CRB p. 47: Damage Resistance is subtracted from damage of the same type before it reduces
 * health. Some origins are immune to a damage type instead (e.g. super mutants to radiation and
 * poison, CRB p. 53).
 *
 * @param value resistance points; always 0 when {@code immune}
 * @param immune whether damage of this type is ignored completely
 */
public record Resistance(int value, boolean immune) {

  public static final @NonNull Resistance IMMUNITY = new Resistance(0, true);

  public Resistance {
    if (value < 0 || (immune && value != 0)) {
      throw new IllegalArgumentException("Resistance must not be negative and 0 when immune");
    }
  }

  public static @NonNull Resistance of(int value) {
    return new Resistance(value, false);
  }

  /** Display text as on the character sheet: {@code Immune} or the value. */
  @Override
  public @NonNull String toString() {
    return immune ? "Immune" : Integer.toString(value);
  }
}
