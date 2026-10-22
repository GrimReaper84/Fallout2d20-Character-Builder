package de.fallout2d20.charbuilder.catalog.perk;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One perk from the catalog (CRB p. 59–73). Named "definition" to keep it apart from the save-file
 * record {@code persistence.PerkData}.
 *
 * <p>Only effects the builder computes are structured; everything else is described in
 * {@code summary} (own words). Optional JSON fields default to "no effect".
 *
 * @param id stable catalog id, referenced by save files
 * @param name name as printed in the book
 * @param sources book and page where the perk starts
 * @param ranks how often the perk can be taken
 * @param requirements what the character needs; {@code null} in JSON means none
 * @param choice what the player chooses per rank, or {@code null}
 * @param summary short description in own words
 * @param legacyNames names used by the legacy app where they differ from {@code name}
 * @param carryWeightPerRank carry weight in lbs. per rank (Strong Back: 25, CRB p. 73)
 * @param physicalDrPerRank physical DR on all locations per rank (Toughness: 1, CRB p. 73)
 * @param energyDrPerRank energy DR per rank (Refractor: 1, CRB p. 70)
 * @param radiationDrPerRank radiation DR per rank (Rad Resistance: 1, CRB p. 70)
 * @param poisonDrPerRank poison DR per rank (Snakeater: 2, CRB p. 72)
 * @param maxHpPerEndurance maximum HP per rank as a multiple of END (Life Giver: 1, CRB p. 66)
 * @param physicalDrByStrength physical DR from the STR tiers of the melee bonus (Barbarian, CRB p. 60)
 * @param skillPointsPerRank skill ranks to distribute per rank (Skilled: 2, CRB p. 71)
 * @param drByAgility physical and energy DR on all locations from AGI: +1 at 7–8, +2 at 9–10, +3 at 11+, not while
 *     over-encumbered (Evasive, WG p. 51)
 * @param wornArmorDrPerRank physical and energy DR on all locations per rank while armor is worn (Ironclad: 1,
 *     WG p. 48)
 * @param junkDrPerRank physical and energy DR for every 5 junk carried, at most this value per rank (Junk Shield:
 *     1, WG p. 52)
 * @param armorWeightQuartersPerRank quarters of the worn armor's weight that stop counting per rank (Sturdy Frame:
 *     1, so ¾, ½, ¼ remain, WG p. 45)
 * @param junkWeightHalved whether carried junk counts half its weight (Pack Rat, WG p. 45)
 */
public record PerkDefinition(
    @JsonProperty(required = true) @NonNull String id,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull List<Source> sources,
    @JsonProperty(required = true) int ranks,
    @NonNull PerkRequirements requirements,
    @Nullable PerkChoice choice,
    @JsonProperty(required = true) @NonNull String summary,
    @NonNull List<String> legacyNames,
    int carryWeightPerRank,
    int physicalDrPerRank,
    int energyDrPerRank,
    int radiationDrPerRank,
    int poisonDrPerRank,
    int maxHpPerEndurance,
    boolean physicalDrByStrength,
    int skillPointsPerRank,
    boolean drByAgility,
    int wornArmorDrPerRank,
    int junkDrPerRank,
    int armorWeightQuartersPerRank,
    boolean junkWeightHalved) {

  /** CRB p. 59: perks with ranks go up to 10 (Intense Training, Skilled). */
  private static final int MAX_RANKS = 10;

  public PerkDefinition {
    if (isBlank(id) || isBlank(name) || isBlank(summary) || sources == null || sources.isEmpty()) {
      throw new IllegalArgumentException("Perk needs id, name, summary and a source: " + id);
    }
    if (ranks < 1 || ranks > MAX_RANKS || carryWeightPerRank < 0 || physicalDrPerRank < 0
        || energyDrPerRank < 0 || radiationDrPerRank < 0 || poisonDrPerRank < 0 || maxHpPerEndurance < 0
        || skillPointsPerRank < 0 || wornArmorDrPerRank < 0 || junkDrPerRank < 0 || armorWeightQuartersPerRank < 0) {
      throw new IllegalArgumentException("Invalid ranks or effect values for perk " + id);
    }
    sources = List.copyOf(sources);
    requirements = requirements == null ? PerkRequirements.NONE : requirements;
    legacyNames = legacyNames == null ? List.of() : List.copyOf(legacyNames);
  }

  /** Whether one of the books describing the perk is enabled. */
  public boolean isAvailableWith(@NonNull Set<Book> books) {
    return sources.stream().anyMatch(source -> books.contains(source.book()));
  }

  private static boolean isBlank(@Nullable String text) {
    return text == null || text.isBlank();
  }
}
