package de.fallout2d20.charbuilder.model.character;

import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * The seven S.P.E.C.I.A.L. attributes.
 *
 * <p>CRB p. 42: every character has these seven attributes; their rank normally lies between 4 and
 * 10, although some creatures and perks go beyond that.
 */
public enum Attribute {
  STRENGTH("str", "Strength"),
  PERCEPTION("per", "Perception"),
  ENDURANCE("end", "Endurance"),
  CHARISMA("cha", "Charisma"),
  INTELLIGENCE("int", "Intelligence"),
  AGILITY("agi", "Agility"),
  LUCK("lck", "Luck");

  private final String id;
  private final String displayName;

  Attribute(String id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  /** Stable identifier used in save files, e.g. {@code "str"}. */
  public String id() {
    return id;
  }

  public String displayName() {
    return displayName;
  }

  /** Three-letter abbreviation as used on the character sheet, e.g. {@code STR}. */
  public @NonNull String abbreviation() {
    return id.toUpperCase(java.util.Locale.ROOT);
  }

  public static Optional<Attribute> fromId(String id) {
    for (Attribute attribute : values()) {
      if (attribute.id.equals(id)) {
        return Optional.of(attribute);
      }
    }
    return Optional.empty();
  }
}
