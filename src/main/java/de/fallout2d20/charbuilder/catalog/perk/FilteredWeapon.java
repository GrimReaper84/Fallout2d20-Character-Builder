package de.fallout2d20.charbuilder.catalog.perk;

import de.fallout2d20.charbuilder.catalog.weapon.WeaponKind;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The values of a weapon a {@link WeaponFilter} looks at, taken after its mods and legendary property.
 *
 * @param id catalog id; {@code null} for a custom weapon
 * @param skill attack skill; {@code null} if unknown
 * @param fireRate fire rate; {@code null} for none
 * @param qualities qualities as text, e.g. {@code Thrown (M)}
 * @param damageEffects damage effects as text
 * @param damageType damage type(s) as text, e.g. {@code Physical/Energy}
 * @param kinds the weapon's kinds, including those of its mods
 */
public record FilteredWeapon(
    @Nullable String id,
    @Nullable Skill skill,
    @Nullable Integer fireRate,
    @NonNull List<String> qualities,
    @NonNull List<String> damageEffects,
    @NonNull String damageType,
    @NonNull Set<WeaponKind> kinds) {

  private static final @NonNull String TWO_HANDED = "two handed";
  private static final @NonNull String RADIOACTIVE = "radioactive";
  private static final @NonNull String RADIATION = "radiation";

  public FilteredWeapon {
    qualities = List.copyOf(qualities);
    damageEffects = List.copyOf(damageEffects);
    kinds = WeaponKind.copyOf(kinds);
  }

  /** Whether the weapon has the quality, compared without its rating, case and hyphens. */
  public boolean hasQuality(@NonNull String quality) {
    return qualities.stream().anyMatch(present -> key(present).equals(key(quality)));
  }

  boolean isTwoHanded() {
    return hasQuality(TWO_HANDED);
  }

  boolean dealsRadiation() {
    return damageType.toLowerCase(Locale.ROOT).contains(RADIATION)
        || damageEffects.stream().anyMatch(effect -> key(effect).equals(RADIOACTIVE));
  }

  /** A quality or effect without its rating, regardless of case and hyphens: {@code Thrown (M)} → {@code thrown}. */
  private static @NonNull String key(@NonNull String value) {
    int bracket = value.indexOf('(');
    String name = bracket < 0 ? value : value.substring(0, bracket);
    return name.strip().replace('-', ' ').toLowerCase(Locale.ROOT);
  }
}
