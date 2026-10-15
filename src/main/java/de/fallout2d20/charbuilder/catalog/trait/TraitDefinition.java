package de.fallout2d20.charbuilder.catalog.trait;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One trait from the catalog: the Survivor traits (CRB p. 56) and the Tribal traits (WG p. 15).
 * Named "definition" to keep it apart from the save-file record {@code persistence.TraitData}.
 *
 * <p>Only effects the builder computes are structured; play rules are described in
 * {@code benefit} and {@code penalty} (own words). Optional JSON fields default to "no effect".
 *
 * @param id stable catalog id, referenced by save files
 * @param name name as printed in the book
 * @param list id of the trait list, matching the origins' {@code traitLists} ({@code survivor},
 *     {@code tribal})
 * @param sources book and page of the trait table
 * @param benefit short summary of the benefit
 * @param penalty short summary of the penalty
 * @param extraTagSkills additional free Tag skills (Educated: 1, CRB p. 56); counted by the skill
 *     allocation rules
 * @param attributeChoices number of different attributes the player raises by 1 each (Gifted: 2,
 *     CRB p. 56)
 * @param forbiddenTagSkills skills that may not be Tag skills (Nomad: Science, WG p. 15)
 * @param carryWeightPerStrength carry weight per STR point instead of 10 (Small Frame: 5,
 *     CRB p. 56); {@code null} for the normal formula
 * @param meleeBonusDice additional bonus Combat Dice on melee damage (Heavy Handed: 1, CRB p. 56)
 * @param luckPointPenalty fewer maximum Luck points (Gifted: 1, CRB p. 56)
 */
public record TraitDefinition(
    @JsonProperty(required = true) @NonNull String id,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull String list,
    @JsonProperty(required = true) @NonNull List<Source> sources,
    @JsonProperty(required = true) @NonNull String benefit,
    @JsonProperty(required = true) @NonNull String penalty,
    int extraTagSkills,
    int attributeChoices,
    @NonNull List<Skill> forbiddenTagSkills,
    @Nullable Integer carryWeightPerStrength,
    int meleeBonusDice,
    int luckPointPenalty) {

  public TraitDefinition {
    if (isBlank(id) || isBlank(name) || isBlank(list) || isBlank(benefit) || isBlank(penalty)
        || sources == null || sources.isEmpty()) {
      throw new IllegalArgumentException("Trait needs id, name, list, benefit, penalty and a source: " + id);
    }
    if (extraTagSkills < 0 || attributeChoices < 0 || attributeChoices > 7 || meleeBonusDice < 0
        || luckPointPenalty < 0 || (carryWeightPerStrength != null && carryWeightPerStrength < 1)) {
      throw new IllegalArgumentException("Invalid effect values for trait " + id);
    }
    sources = List.copyOf(sources);
    forbiddenTagSkills = forbiddenTagSkills == null ? List.of() : List.copyOf(forbiddenTagSkills);
  }

  private static boolean isBlank(@Nullable String text) {
    return text == null || text.isBlank();
  }
}
