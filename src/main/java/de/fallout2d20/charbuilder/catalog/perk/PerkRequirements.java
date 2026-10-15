package de.fallout2d20.charbuilder.catalog.perk;

import de.fallout2d20.charbuilder.model.character.Attribute;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * What a character needs to take a perk (CRB p. 59–73).
 *
 * @param attributes minimum value per attribute
 * @param level minimum level for the first rank; {@code null} means level 1 (user decision)
 * @param levelIncreasePerRank levels added to the requirement for every further rank (CRB p. 59);
 *     {@code null} if the book names none
 * @param notRobot whether robots cannot take the perk
 * @param excludedPerks ids of perks that cannot be held together with this one (Cautious Nature and
 *     Daring Nature, CRB p. 62)
 */
public record PerkRequirements(
    @NonNull Map<Attribute, Integer> attributes,
    @Nullable Integer level,
    @Nullable Integer levelIncreasePerRank,
    boolean notRobot,
    @NonNull List<String> excludedPerks) {

  /** No requirements at all ("None" in the book). */
  public static final @NonNull PerkRequirements NONE = new PerkRequirements(Map.of(), null, null, false, List.of());

  public PerkRequirements {
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    excludedPerks = excludedPerks == null ? List.of() : List.copyOf(excludedPerks);
    if (attributes.values().stream().anyMatch(value -> value < 1 || value > 12)
        || (level != null && level < 1) || (levelIncreasePerRank != null && levelIncreasePerRank < 1)) {
      throw new IllegalArgumentException("Invalid perk requirements");
    }
  }

  /**
   * Level needed for the given rank: the first rank needs the base level (1 if none is given), every
   * further rank the step more (CRB p. 59: Life Giver at level 5, rank 2 at 10, rank 3 at 15).
   */
  public int levelForRank(int rank) {
    int base = level == null ? 1 : level;
    int step = levelIncreasePerRank == null ? 0 : levelIncreasePerRank;
    return base + (rank - 1) * step;
  }
}
