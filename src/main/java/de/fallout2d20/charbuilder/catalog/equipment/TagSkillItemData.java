package de.fallout2d20.charbuilder.catalog.equipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.HashSet;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Items a Tag skill grants at character creation (catalog entry).
 *
 * <p>CRB p. 81 (Items Gained from Tag Skills): each Tag skill grants the listed items; some rows offer
 * a choice between two items, and Barter grants extra caps rolled on d20s.
 *
 * @param options one entry for a fixed item line, two or more to pick one; empty only for a row that
 *     grants nothing but caps
 * @param capsD20 number of d20 rolled for extra caps (Barter: 2), else 0
 */
public record TagSkillItemData(
    @JsonProperty(required = true) @NonNull Skill skill,
    @NonNull List<String> options,
    int capsD20,
    @JsonProperty(required = true) @NonNull List<Source> sources) {

  public TagSkillItemData {
    options = options == null ? List.of() : List.copyOf(options);
    if (skill == null || sources == null || sources.isEmpty() || capsD20 < 0
        || (options.isEmpty() && capsD20 == 0) || options.stream().anyMatch(String::isBlank)
        || new HashSet<>(options).size() != options.size()) {
      throw new IllegalArgumentException("Invalid Tag skill items for " + skill);
    }
    sources = List.copyOf(sources);
  }

  /** Whether the player picks one of several options. */
  public boolean isChoice() {
    return options.size() > 1;
  }
}
