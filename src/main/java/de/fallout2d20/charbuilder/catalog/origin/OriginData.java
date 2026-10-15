package de.fallout2d20.charbuilder.catalog.origin;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One character origin from the catalog (CRB p. 51 ff., WOA p. 18 ff., WG p. 10 ff.).
 *
 * <p>Only identity and the effects already used by the application are modeled; the creation rules
 * of an origin (attribute modifiers and limits, Tag skills, trait lists, equipment packs) follow with
 * the origin feature.
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
    int baseRadiationDr) {

  public OriginData {
    if (id == null || id.isBlank() || name == null || name.isBlank() || traitSummary == null
        || traitSummary.isBlank() || sources == null || sources.isEmpty()) {
      throw new IllegalArgumentException("Origin needs id, name, trait summary and a source: " + id);
    }
    if ((fixedCarryWeight != null && fixedCarryWeight < 1) || baseRadiationDr < 0) {
      throw new IllegalArgumentException("Invalid effect values for origin " + id);
    }
    sources = List.copyOf(sources);
  }

  /** Whether one of the books describing this origin is enabled. */
  public boolean isAvailableWith(@NonNull Set<Book> books) {
    return sources.stream().anyMatch(source -> books.contains(source.book()));
  }
}
