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
 * One weapon from the catalog, with the values of its table row (CRB p. 88–91: weapon type, damage
 * rating, damage effects, damage type, fire rate, range, qualities, weight, cost, rarity) and the
 * ammunition named in its description. Values are the base entries before mods.
 *
 * @param id stable catalog id, referenced by save files
 * @param name name as printed in the book
 * @param sources book and page of the table row
 * @param skill the weapon type, i.e. the skill used to attack (CRB p. 88)
 * @param damage damage rating in Combat Dice
 * @param damageEffects damage effects as printed, with their rating, e.g. {@code Piercing 1}
 * @param damageTypes the damage type(s)
 * @param fireRate fire rate 0–6; {@code null} for weapons without one (melee, most explosives)
 * @param range ideal range; {@code null} for melee and unarmed weapons
 * @param qualities qualities as printed, e.g. {@code Close Quarters}, {@code Thrown (M)}
 * @param weight weight in lbs. as printed, e.g. {@code <1} (the unit is decided with carry weight, C4);
 *     {@code null} where the table has none (Unarmed Strike, Gun Bash: as the gun)
 * @param cost cost in caps; {@code null} where the table has none
 * @param rarity rarity 0–6; {@code null} where the table has none
 * @param ammunition ids of the ammunition types the weapon can use, usually one (Gatling Laser: fusion cells
 *     or a fusion core, CRB p. 107); empty if none
 * @param legacyNames names used by the legacy app where they differ from {@code name}
 */
public record WeaponData(
    @JsonProperty(required = true) @NonNull String id,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull List<Source> sources,
    @JsonProperty(required = true) @NonNull Skill skill,
    @JsonProperty(required = true) int damage,
    @NonNull List<String> damageEffects,
    @JsonProperty(required = true) @NonNull List<DamageType> damageTypes,
    @Nullable Integer fireRate,
    @Nullable WeaponRange range,
    @NonNull List<String> qualities,
    @Nullable String weight,
    @Nullable Integer cost,
    @Nullable Integer rarity,
    @NonNull List<String> ammunition,
    @NonNull List<String> legacyNames) {

  /** CRB p. 89: fire rate is a number from 0 to 6. */
  private static final int MAX_FIRE_RATE = 6;

  public WeaponData {
    if (id == null || id.isBlank() || name == null || name.isBlank() || sources == null || sources.isEmpty()
        || skill == null || damageTypes == null || damageTypes.isEmpty() || (weight != null && weight.isBlank())) {
      throw new IllegalArgumentException("Weapon needs id, name, source, skill and damage type: " + id);
    }
    if (damage < 0 || (cost != null && cost < 0) || (rarity != null && rarity < 0)
        || (fireRate != null && (fireRate < 0 || fireRate > MAX_FIRE_RATE))) {
      throw new IllegalArgumentException("Invalid values for weapon " + id);
    }
    sources = List.copyOf(sources);
    damageEffects = damageEffects == null ? List.of() : List.copyOf(damageEffects);
    damageTypes = List.copyOf(damageTypes);
    qualities = qualities == null ? List.of() : List.copyOf(qualities);
    ammunition = ammunition == null ? List.of() : List.copyOf(ammunition);
    legacyNames = legacyNames == null ? List.of() : List.copyOf(legacyNames);
  }

  /** Whether one of the books describing the weapon is enabled. */
  public boolean isAvailableWith(@NonNull Set<Book> books) {
    return sources.stream().anyMatch(source -> books.contains(source.book()));
  }
}
