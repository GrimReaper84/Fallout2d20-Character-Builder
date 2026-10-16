package de.fallout2d20.charbuilder.catalog.origin;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One character origin from the catalog (CRB p. 51 ff., WOA p. 18 ff., WG p. 10 ff.).
 *
 * <p>Besides identity, it holds the effects on derived statistics and the creation rules of the
 * origin. Equipment packs (incl. Mister Handy arm attachments and Protectron robot mods) are not part
 * of it. Optional JSON fields default to "no rule".
 *
 * @param id stable catalog id, referenced by save files
 * @param name name as printed in the book
 * @param sources every book describing this origin (Child of Atom: WOA and WG)
 * @param traitName name of the origin trait; {@code null} when the origin offers a list of traits
 *     instead (Survivor CRB p. 56, Tribal WG p. 15)
 * @param traitSummary short summary of the trait in own words
 * @param fixedCarryWeight carry weight in lbs. that STR and perks do not change (Mister Handy CRB
 *     p. 55, Protectron WOA p. 21, Assaultron WG p. 11); {@code null} for the normal formula
 * @param robot whether characters of this origin are robots
 * @param radiationImmune immune to radiation damage (e.g. Super Mutant CRB p. 53, Ghoul CRB p. 52)
 * @param poisonImmune immune to poison damage (e.g. Super Mutant CRB p. 53)
 * @param baseRadiationDr innate radiation Damage Resistance (Child of Atom 1, WOA p. 25)
 * @param attributeModifiers change of the starting attribute value (Super Mutant CRB p. 53, Nightkin
 *     WG p. 14: STR and END +2)
 * @param attributeMaximums attribute maximums that differ from the general maximum of 10 (CRB p. 58)
 * @param skillRankCap highest rank any skill may reach, if lower than the general 6 (Super Mutant,
 *     Nightkin: 4)
 * @param fixedTagSkill skill the origin tags in addition to the three free Tag skills (Ghoul:
 *     Survival, CRB p. 52 – read as an additional Tag skill, user decision)
 * @param extraTagSkill additional Tag skill the player chooses (Brotherhood Initiate CRB p. 51,
 *     Vault Dweller CRB p. 57, Generation 3 Synth WOA p. 19, Brotherhood Outcast WG p. 12)
 * @param traitLists ids of the trait lists the origin offers; the character takes two traits from
 *     one list, or one trait plus one extra perk (Survivor CRB p. 56, Tribal WG p. 15)
 * @param extraPerks perks granted in addition at level 1 (Child of Atom 1, WOA p. 25)
 * @param purposeSuggestions suggested purposes; non-empty means the origin requires a purpose
 *     (Protectron, WOA p. 21)
 * @param maxRadiationPoints most Radiation Points the character can store (Child of Atom Rad Sponge: 5,
 *     WOA p. 25, WG p. 13); 0 for origins without them
 */
public record OriginData(
    @JsonProperty(required = true) @NonNull String id,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull List<Source> sources,
    @Nullable String traitName,
    @JsonProperty(required = true) @NonNull String traitSummary,
    @Nullable Integer fixedCarryWeight,
    boolean robot,
    boolean radiationImmune,
    boolean poisonImmune,
    int baseRadiationDr,
    @NonNull Map<Attribute, Integer> attributeModifiers,
    @NonNull Map<Attribute, Integer> attributeMaximums,
    @Nullable Integer skillRankCap,
    @Nullable Skill fixedTagSkill,
    @Nullable ExtraTagSkill extraTagSkill,
    @NonNull List<String> traitLists,
    int extraPerks,
    @NonNull List<String> purposeSuggestions,
    int maxRadiationPoints) {

  /** CRB p. 58: no attribute may exceed 10 unless the origin changes the maximum. */
  public static final int DEFAULT_ATTRIBUTE_MAXIMUM = 10;

  public OriginData {
    if (id == null || id.isBlank() || name == null || name.isBlank() || traitSummary == null
        || traitSummary.isBlank() || sources == null || sources.isEmpty()) {
      throw new IllegalArgumentException("Origin needs id, name, trait summary and a source: " + id);
    }
    if ((fixedCarryWeight != null && fixedCarryWeight < 1) || baseRadiationDr < 0) {
      throw new IllegalArgumentException("Invalid effect values for origin " + id);
    }
    sources = List.copyOf(sources);
    attributeModifiers = attributeModifiers == null ? Map.of() : Map.copyOf(attributeModifiers);
    attributeMaximums = attributeMaximums == null ? Map.of() : Map.copyOf(attributeMaximums);
    traitLists = traitLists == null ? List.of() : List.copyOf(traitLists);
    purposeSuggestions = purposeSuggestions == null ? List.of() : List.copyOf(purposeSuggestions);
    if (attributeModifiers.values().stream().anyMatch(value -> value < -2 || value > 2)
        || attributeMaximums.values().stream().anyMatch(value -> value < 1 || value > 12)
        || (skillRankCap != null && (skillRankCap < 1 || skillRankCap > 6)) || extraPerks < 0
        || maxRadiationPoints < 0) {
      throw new IllegalArgumentException("Invalid creation rules for origin " + id);
    }
    if (fixedTagSkill != null && extraTagSkill != null && extraTagSkill.options().contains(fixedTagSkill)) {
      throw new IllegalArgumentException("Fixed Tag skill must not be an extra Tag option: " + id);
    }
  }

  /** Starting attribute change granted by this origin (0 if none). */
  public int attributeModifier(@NonNull Attribute attribute) {
    return attributeModifiers.getOrDefault(attribute, 0);
  }

  /** Highest value the attribute may have for this origin (CRB p. 58: 10 unless changed). */
  public int attributeMaximum(@NonNull Attribute attribute) {
    return attributeMaximums.getOrDefault(attribute, DEFAULT_ATTRIBUTE_MAXIMUM);
  }

  /** Whether one of the books describing this origin is enabled. */
  public boolean isAvailableWith(@NonNull Set<Book> books) {
    return sources.stream().anyMatch(source -> books.contains(source.book()));
  }
}
