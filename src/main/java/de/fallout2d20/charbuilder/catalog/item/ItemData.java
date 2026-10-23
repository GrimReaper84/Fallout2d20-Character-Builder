package de.fallout2d20.charbuilder.catalog.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One row of the item tables (CRB pp. 149–187: food, beverages, chems, other consumables, tools and utility
 * items, robot modules; WG pp. 108–125: rare books, irradiated food, junk) with its values; the effect is a short
 * paraphrase, not the book text.
 *
 * @param id stable catalog id, referenced by save files
 * @param name name as printed
 * @param sources book and page of the table row
 * @param category the table the item comes from
 * @param effects the effect in short; {@code null} for none (plain food)
 * @param hpHealed HP healed by food and beverages; {@code null} for other items
 * @param irradiated whether the food or beverage is irradiated (CRB p. 149)
 * @param irradiationDice the CD rolled for radiation when it is eaten, if more than one (WG p. 114: 2 CD);
 *     {@code null} for one
 * @param duration chem duration; {@code null} for other items
 * @param addictive addiction number of an addictive chem; {@code null} if not addictive
 * @param weight weight in lbs. as printed, e.g. {@code <1}; {@code null} where the table has none
 * @param cost cost in caps; {@code null} where the table has none
 * @param rarity rarity 0–6; {@code null} where the table has none
 * @param perks perks needed to install a robot module, as printed; {@code null} for none
 * @param robotCarryWeight change of a robot's carry weight by an installed module (the negative weight
 *     column, CRB p. 185); 0 for none
 * @param carryWeightPerStrength carry weight per STR point a worn backpack adds (CRB p. 181); 0 for none
 * @param aliases other names the item is found by, e.g. wordings of equipment packs
 * @param rarePerk id of the rare perk a rare book gives access to (WG p. 108); {@code null} for other items
 * @param components what a junk item breaks down into, as printed in short (WG pp. 121–125); {@code null} for
 *     other items
 * @param junkTables the junk tables listing the item (WG pp. 121–125); empty for other items
 */
public record ItemData(
    @JsonProperty(required = true) @NonNull String id,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull List<Source> sources,
    @JsonProperty(required = true) @NonNull ItemCategory category,
    @Nullable String effects,
    @Nullable Integer hpHealed,
    boolean irradiated,
    @Nullable ChemDuration duration,
    @Nullable Integer addictive,
    @Nullable String weight,
    @Nullable Integer cost,
    @Nullable Integer rarity,
    @Nullable String perks,
    int robotCarryWeight,
    int carryWeightPerStrength,
    @NonNull List<String> aliases,
    @Nullable Integer irradiationDice,
    @Nullable String rarePerk,
    @Nullable String components,
    @NonNull List<String> junkTables) {

  public ItemData {
    if (id == null || id.isBlank() || name == null || name.isBlank() || sources == null || sources.isEmpty()
        || category == null || (weight != null && weight.isBlank())) {
      throw new IllegalArgumentException("Item needs id, name, source and category: " + id);
    }
    if ((hpHealed != null && hpHealed < 0) || (addictive != null && addictive < 1) || (cost != null && cost < 0)
        || (rarity != null && rarity < 0) || robotCarryWeight > 0 || carryWeightPerStrength < 0
        || (irradiationDice != null && (!irradiated || irradiationDice < 2))) {
      throw new IllegalArgumentException("Invalid values for item " + id);
    }
    sources = List.copyOf(sources);
    aliases = aliases == null ? List.of() : List.copyOf(aliases);
    junkTables = junkTables == null ? List.of() : List.copyOf(junkTables);
  }

  /** The CD rolled for radiation when the item is eaten or drunk (CRB p. 149: one; WG p. 114: some two); 0 if clean. */
  public int radiationDice() {
    if (!irradiated) {
      return 0;
    }
    return irradiationDice == null ? 1 : irradiationDice;
  }

  /** Whether one of the books describing the item is enabled. */
  public boolean isAvailableWith(@NonNull Set<Book> books) {
    return sources.stream().anyMatch(source -> books.contains(source.book()));
  }
}
