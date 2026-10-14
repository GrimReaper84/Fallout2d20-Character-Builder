package de.fallout2d20.charbuilder.model.character;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Attribute values and skill ranks taken over from a legacy save.
 *
 * <p>Legacy saves only contain effective values (origin modifiers, traits and perks already
 * applied), so the original allocation cannot be reconstructed. These values replace the base and
 * the creation allocation of an imported character; later advancement builds on top of them.
 *
 * @param attributes value for every attribute
 * @param skillRanks rank for every skill, including the Tag skill bonus
 */
public record LegacyBaseline(Map<Attribute, Integer> attributes, Map<Skill, Integer> skillRanks) {

  public LegacyBaseline {
    if (attributes.size() != Attribute.values().length || skillRanks.size() != Skill.values().length) {
      throw new IllegalArgumentException("Legacy baseline needs a value for every attribute and skill");
    }
    attributes = Map.copyOf(new EnumMap<>(attributes));
    skillRanks = Map.copyOf(new EnumMap<>(skillRanks));
  }

  public int attribute(Attribute attribute) {
    // Complete by construction, see the compact constructor.
    return Objects.requireNonNull(attributes.get(attribute));
  }

  public int skillRank(Skill skill) {
    return Objects.requireNonNull(skillRanks.get(skill));
  }
}
