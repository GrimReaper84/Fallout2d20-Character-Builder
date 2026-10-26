package de.fallout2d20.charbuilder.catalog.legendary;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.weapon.ModEffect;
import de.fallout2d20.charbuilder.catalog.weapon.ModEffectType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A property of Legendary Weapons (WG pp. 97–103): the table it stands in, its d20 result per weapon type, the
 * values it changes and its rule in own words. Powerful and Mighty, one property in the book (p. 102), are two
 * entries because the tables list them in different columns.
 *
 * @param id stable catalog id, {@code legendary-weapon-<name>}, referenced by save files
 * @param name name as printed, e.g. {@code Two-Shot}
 * @param sources book and page of the description
 * @param table Legendary Weapons Table 1 or 2
 * @param rolls the d20 result per weapon type; a type without one cannot get the property
 * @param effects the value changes, applied like mod effects ({@code ModdedWeapon}), only of the kinds in
 *     {@link #SUPPORTED_EFFECTS}; empty if the property changes no value
 * @param rule the whole property in own words, shown to the player
 */
public record LegendaryWeaponData(
    @JsonProperty(required = true) @NonNull String id,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull List<Source> sources,
    @JsonProperty(required = true) int table,
    @JsonProperty(required = true) @NonNull Map<LegendaryWeaponType, Integer> rolls,
    @NonNull List<ModEffect> effects,
    @JsonProperty(required = true) @NonNull String rule) {

  /** The kinds of change {@code ModdedWeapon} applies for a legendary property. */
  public static final @NonNull Set<ModEffectType> SUPPORTED_EFFECTS = Collections.unmodifiableSet(EnumSet.of(
      ModEffectType.DAMAGE, ModEffectType.FIRE_RATE, ModEffectType.GAIN_EFFECT, ModEffectType.GAIN_QUALITY,
      ModEffectType.ADD_DAMAGE_TYPE));

  public LegendaryWeaponData {
    if (id == null || id.isBlank() || name == null || name.isBlank() || sources == null || sources.isEmpty()
        || rolls == null || rolls.isEmpty() || rule == null || rule.isBlank() || (table != 1 && table != 2)) {
      throw new IllegalArgumentException("Legendary weapon property needs id, name, source, table, rolls and rule: "
          + id);
    }
    sources = List.copyOf(sources);
    rolls = Map.copyOf(new EnumMap<>(rolls));
    effects = effects == null ? List.of() : List.copyOf(effects);
    for (ModEffect effect : effects) {
      if (!SUPPORTED_EFFECTS.contains(effect.type())) {
        throw new IllegalArgumentException("Legendary weapon property " + id + " has unsupported effect "
            + effect.type());
      }
    }
  }

  /** Whether a weapon of this type can get the property. */
  public boolean fits(@NonNull LegendaryWeaponType type) {
    return rolls.containsKey(type);
  }

  /** The d20 result for this weapon type; {@code null} if it has none. */
  public @Nullable Integer roll(@NonNull LegendaryWeaponType type) {
    return rolls.get(type);
  }

  /** Whether one of the books describing the property is enabled. */
  public boolean isAvailableWith(@NonNull Set<Book> books) {
    return sources.stream().anyMatch(source -> books.contains(source.book()));
  }
}
