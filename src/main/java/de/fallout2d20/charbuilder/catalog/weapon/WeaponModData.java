package de.fallout2d20.charbuilder.catalog.weapon;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One weapon mod of a mod table (CRB pp. 100–119, WOA p. 23): its slot, the name prefix it gives the
 * weapon, its effects, the weight and cost it adds and the perks and skill needed to install it (CRB p. 87).
 * The same name can stand in several tables with other values (e.g. the Gatling Laser's own Photon
 * Exciter), so the id names the table.
 *
 * @param id stable catalog id, {@code <table>-<name>}, referenced by save files
 * @param name name as printed in the table
 * @param table id of the mod table, e.g. {@code small-guns}, {@code gatling-laser}
 * @param tableName title of the mod table as shown to the player
 * @param sources book and page of the table row
 * @param slot where the mod sits
 * @param skill the skill used to install it (Repair or Science)
 * @param namePrefix what the mod adds to the weapon's name; {@code null} for none ("–")
 * @param effects the changes, in table order
 * @param weight lbs. added (negative: removed); {@code null} for "–"
 * @param cost caps added; {@code null} for "–"
 * @param perks perk ranks needed to install it; empty for none
 */
public record WeaponModData(
    @JsonProperty(required = true) @NonNull String id,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull String table,
    @JsonProperty(required = true) @NonNull String tableName,
    @JsonProperty(required = true) @NonNull List<Source> sources,
    @JsonProperty(required = true) @NonNull ModSlot slot,
    @JsonProperty(required = true) @NonNull Skill skill,
    @Nullable String namePrefix,
    @NonNull List<ModEffect> effects,
    @Nullable Integer weight,
    @Nullable Integer cost,
    @NonNull List<ModPerk> perks) {

  public WeaponModData {
    if (id == null || id.isBlank() || name == null || name.isBlank() || table == null || table.isBlank()
        || tableName == null || sources == null || sources.isEmpty() || slot == null || skill == null) {
      throw new IllegalArgumentException("Weapon mod needs id, name, table, source, slot and skill: " + id);
    }
    sources = List.copyOf(sources);
    effects = effects == null ? List.of() : List.copyOf(effects);
    perks = perks == null ? List.of() : List.copyOf(perks);
  }

  /** Whether one of the books describing the mod is enabled. */
  public boolean isAvailableWith(@NonNull Set<Book> books) {
    return sources.stream().anyMatch(source -> books.contains(source.book()));
  }
}
