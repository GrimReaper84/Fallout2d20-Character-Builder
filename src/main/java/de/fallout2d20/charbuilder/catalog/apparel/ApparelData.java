package de.fallout2d20.charbuilder.catalog.apparel;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One clothing, outfit, headgear, armor or robot armor row from the catalog (CRB p. 124–125, 130–132,
 * 146–147), with the values of the table; mods are not included.
 *
 * @param id stable catalog id, referenced by save files
 * @param name name as printed (robot armor rows repeat the name per location; the id tells them apart)
 * @param sources book and page of the table row
 * @param kind clothing, outfit, headgear, armor, robot plating or robot armor
 * @param coverage the locations covered
 * @param physical physical damage resistance (robot armor: added to the plating)
 * @param energy energy damage resistance
 * @param radiation radiation damage resistance (0 for robot items, whose table has none)
 * @param radiationImmune whether the item makes the covered locations immune to radiation (Hazmat Suit)
 * @param weight weight in lbs. as printed, e.g. {@code <1}; {@code null} if the table has none (robot items)
 * @param cost cost in caps; {@code null} if the table has none
 * @param rarity rarity; {@code null} if the table has none (robot items)
 * @param carryWeight change of a robot's carry weight in lbs.; 0 for none
 * @param perk perk needed to install the robot armor, e.g. {@code Armorer 1}; {@code null} for none
 */
public record ApparelData(
    @JsonProperty(required = true) @NonNull String id,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull List<Source> sources,
    @JsonProperty(required = true) @NonNull ApparelKind kind,
    @JsonProperty(required = true) @NonNull List<Coverage> coverage,
    int physical,
    int energy,
    int radiation,
    boolean radiationImmune,
    @Nullable String weight,
    @Nullable Integer cost,
    @Nullable Integer rarity,
    int carryWeight,
    @Nullable String perk) {

  public ApparelData {
    if (id == null || id.isBlank() || name == null || name.isBlank() || sources == null || sources.isEmpty()
        || kind == null || coverage == null || coverage.isEmpty()) {
      throw new IllegalArgumentException("Apparel needs id, name, source, kind and coverage: " + id);
    }
    if (physical < 0 || energy < 0 || radiation < 0 || (radiationImmune && radiation != 0)
        || (cost != null && cost < 0) || (rarity != null && rarity < 0)) {
      throw new IllegalArgumentException("Invalid values for apparel " + id);
    }
    sources = List.copyOf(sources);
    coverage = List.copyOf(coverage);
  }

  /** Whether one of the books describing the item is enabled. */
  public boolean isAvailableWith(@NonNull Set<Book> books) {
    return sources.stream().anyMatch(source -> books.contains(source.book()));
  }

  /** Whether a single arm or leg is chosen when the item is worn. */
  public boolean needsLocation() {
    return coverage.stream().anyMatch(Coverage::isChosen);
  }

  /** CRB p. 146: a Mister Handy may wear headgear with "Hat" in its name, without effect. */
  public boolean isHat() {
    return kind == ApparelKind.HEADGEAR && name.contains("Hat");
  }
}
