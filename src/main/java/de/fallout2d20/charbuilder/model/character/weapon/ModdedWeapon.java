package de.fallout2d20.charbuilder.model.character.weapon;

import de.fallout2d20.charbuilder.catalog.weapon.DamageType;
import de.fallout2d20.charbuilder.catalog.weapon.ModEffect;
import de.fallout2d20.charbuilder.catalog.weapon.ModEffectType;
import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.derived.Weight;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The values of a carried weapon with its mods applied, computed on every call (never stored). Base values
 * are the catalog row, or the custom weapon's values with its effects and qualities read as comma-separated
 * lists.
 *
 * <p>Rules: replacing values (damage, damage type, ammunition) come first, then the changes in slot order.
 * CRB p. 88: a quality is not gained twice; Accurate and Inaccurate cancel each other (likewise Reliable and
 * Unreliable, which a weapon may not both have, CRB p. 90); every Piercing a mod gives raises the rating by 1. A
 * quality a mod removes stays removed, whichever slot gains it (house rule, user decision 2026-10-19: the book
 * does not say; e.g. a Grip removing Inaccurate beats a Beam Splitter gaining it); range moves in steps between Close and Extreme. Fire rate stays within 0–6
 * (CRB p. 89) and damage at 0 or more; these limits apply to the sum of all changes, so the order of the
 * mods does not matter for them. Gained qualities and gained and removed effects follow slot order. Weight and cost add the mods' values (user decision 2026-10-17); a
 * {@code <1} weapon stays {@code <1} unless the mods add weight. The modded name puts the mods' name prefixes
 * in slot order before the name, with a Stock mod the book's rifle name (CRB p. 87).
 *
 * @param name the weapon's own name
 * @param moddedName the name with the mods' prefixes; equals {@code name} without mods
 * @param damage damage rating in CD
 * @param damageEffects damage effects, e.g. {@code Piercing 2}
 * @param damageType damage type(s) as text, e.g. {@code Physical/Energy}
 * @param fireRate fire rate; {@code null} for none
 * @param range table range; {@code null} for none
 * @param qualities qualities
 * @param ammunition ids of the ammunition types
 * @param weight weight as printed ({@code 4}, {@code <1}); {@code null} for none
 * @param cost cost in caps; {@code null} for none
 * @param specials rules of the mods that change no value, each with the mod's name
 * @param removedQualities the qualities mods removed or kept away, each with the mod's name, e.g.
 *     {@code Inaccurate (removed by Comfort Grip)}
 */
public record ModdedWeapon(
    @NonNull String name,
    @NonNull String moddedName,
    int damage,
    @NonNull List<String> damageEffects,
    @NonNull String damageType,
    @Nullable Integer fireRate,
    @Nullable WeaponRange range,
    @NonNull List<String> qualities,
    @NonNull List<String> ammunition,
    @Nullable String weight,
    @Nullable Integer cost,
    @NonNull List<String> specials,
    @NonNull List<String> removedQualities) {

  private static final int MAX_FIRE_RATE = 6;
  private static final @NonNull String LIGHT = "<1";
  private static final @NonNull Pattern PIERCING = Pattern.compile("Piercing (\\d+)", Pattern.CASE_INSENSITIVE);
  private static final @NonNull Map<String, String> OPPOSITES = Map.of("accurate", "inaccurate", "inaccurate", "accurate",
      "reliable", "unreliable", "unreliable", "reliable");

  public ModdedWeapon {
    damageEffects = List.copyOf(damageEffects);
    qualities = List.copyOf(qualities);
    ammunition = List.copyOf(ammunition);
    specials = List.copyOf(specials);
    removedQualities = List.copyOf(removedQualities);
  }

  /** The weapon with its known installed mods; empty for an id outside the catalog. */
  public static @NonNull Optional<ModdedWeapon> of(@NonNull CarriedWeapon weapon) {
    return unmodded(weapon).map(base -> base.apply(WeaponMods.installedMods(weapon), stockName(weapon)));
  }

  /** The values of a catalog row, e.g. of the Unarmed Strike every character has. */
  public static @NonNull ModdedWeapon of(@NonNull WeaponData data) {
    return new ModdedWeapon(data.name(), data.name(), data.damage(), data.damageEffects(),
        data.damageTypes().stream().map(DamageType::displayName).collect(Collectors.joining("/")), data.fireRate(),
        data.range(), data.qualities(), data.ammunition(), data.weight(), data.cost(), List.of(), List.of());
  }

  /**
   * A weapon whose catalog entry is unknown: only its name, no values (the attack then shows no target
   * number).
   */
  public static @NonNull ModdedWeapon unknown(@NonNull String name) {
    return new ModdedWeapon(name, name, 0, List.of(), "", null, null, List.of(), List.of(), null, null, List.of(),
        List.of());
  }

  /** The weapon's values without mods (also shown as the base value next to a changed one). */
  public static @NonNull Optional<ModdedWeapon> unmodded(@NonNull CarriedWeapon weapon) {
    CustomWeapon custom = weapon.custom();
    if (custom != null) {
      String ammunition = custom.ammunition();
      return Optional.of(new ModdedWeapon(custom.name(), custom.name(), custom.damage(), list(custom.effects()),
          custom.damageType(), custom.fireRate(), custom.range(), list(custom.qualities()),
          ammunition == null ? List.of() : List.of(ammunition), null, null, List.of(), List.of()));
    }
    return weapon.data().map(ModdedWeapon::of);
  }

  private static @Nullable String stockName(@NonNull CarriedWeapon weapon) {
    return weapon.data().map(WeaponData::stockName).orElse(null);
  }

  private @NonNull ModdedWeapon apply(@NonNull List<WeaponModData> mods, @Nullable String stockName) {
    Changes changes = new Changes(this, removals(mods));
    for (WeaponModData mod : mods) {
      mod.effects().forEach(changes::replace);
    }
    for (WeaponModData mod : mods) {
      for (ModEffect effect : mod.effects()) {
        changes.change(mod, effect);
      }
    }
    Integer weightChange = total(mods, WeaponModData::weight);
    String newWeight = weightChange == null ? weight : addWeight(weight, weightChange);
    Integer costChange = total(mods, WeaponModData::cost);
    Integer newCost = cost;
    if (costChange != null) {
      newCost = (cost == null ? 0 : cost) + costChange;
    }
    WeaponRange newRange = range == null ? null : step(range, changes.rangeSteps);
    return new ModdedWeapon(name, moddedName(mods, stockName), Math.max(0, changes.damage), changes.effects,
        changes.damageType, changes.fireRate(fireRate), newRange, changes.qualities, changes.ammunition, newWeight,
        newCost, changes.specials, changes.removed);
  }

  /** The qualities the mods remove, each with the first mod that removes it (house rule: removals win). */
  private static @NonNull Map<String, String> removals(@NonNull List<WeaponModData> mods) {
    Map<String, String> removals = new LinkedHashMap<>();
    for (WeaponModData mod : mods) {
      for (ModEffect effect : mod.effects()) {
        if (effect.type() == ModEffectType.REMOVE_QUALITY) {
          removals.putIfAbsent(key(effect.text()), mod.name());
        }
      }
    }
    return removals;
  }

  /** The sum of a value of the mods (weight, cost); {@code null} if no mod has one. */
  private static @Nullable Integer total(@NonNull List<WeaponModData> mods,
      @NonNull Function<WeaponModData, @Nullable Integer> value) {
    Integer total = null;
    for (WeaponModData mod : mods) {
      Integer modValue = value.apply(mod);
      if (modValue != null) {
        total = (total == null ? 0 : total) + modValue;
      }
    }
    return total;
  }

  /**
   * The weapon's values while the mods' effects are applied: first the replacing values of all mods
   * ({@link #replace}), then the changes in slot order ({@link #change}). Qualities the mods remove are taken
   * off the base qualities up front and kept away from gains.
   */
  private static final class Changes {

    private final @NonNull Map<String, String> removals;
    private int damage;
    private @NonNull String damageType;
    private @NonNull List<String> ammunition;
    private int rateChange;
    private int rangeSteps;
    private final @NonNull List<String> effects;
    private final @NonNull List<String> qualities;
    private final @NonNull List<String> specials = new ArrayList<>();
    private final @NonNull List<String> removed = new ArrayList<>();

    Changes(@NonNull ModdedWeapon base, @NonNull Map<String, String> removals) {
      this.removals = removals;
      damage = base.damage;
      damageType = base.damageType;
      ammunition = base.ammunition;
      effects = new ArrayList<>(base.damageEffects);
      qualities = new ArrayList<>(base.qualities);
      qualities.removeIf(this::blocked);
    }

    void replace(@NonNull ModEffect effect) {
      switch (effect.type()) {
        case SET_DAMAGE -> damage = effect.number();
        case SET_DAMAGE_TYPE -> damageType = DamageType.valueOf(effect.text()).displayName();
        case SET_AMMUNITION -> ammunition = List.of(effect.text());
        default -> {
          // every other effect is a change, applied afterwards by change()
        }
      }
    }

    void change(@NonNull WeaponModData mod, @NonNull ModEffect effect) {
      switch (effect.type()) {
        case DAMAGE -> damage += effect.number();
        case FIRE_RATE -> rateChange += effect.number();
        case RANGE -> rangeSteps += effect.number();
        case GAIN_QUALITY -> gainUnlessRemoved(effect.text());
        case GAIN_EFFECT -> gainEffect(effect.text());
        case REMOVE_EFFECT -> removeEffect(effect.text());
        case SPECIAL -> specials.add(mod.name() + ": " + effect.text());
        case REMOVE_QUALITY, SET_DAMAGE, SET_DAMAGE_TYPE, SET_AMMUNITION, ALLOWS_SLOT -> {
          // removals were applied up front, replacing values by replace(); slots change no value
        }
      }
    }

    private void gainUnlessRemoved(@NonNull String quality) {
      if (!blocked(quality)) {
        gainQuality(quality);
      }
    }

    /**
     * Whether a mod removes the quality; if so it is noted once in {@code removed}, e.g. "Inaccurate (removed by
     * Comfort Grip)".
     */
    private boolean blocked(@NonNull String quality) {
      String mod = removals.get(key(quality));
      if (mod == null) {
        return false;
      }
      String note = quality + " (removed by " + mod + ")";
      if (removed.stream().noneMatch(entry -> key(entry).equals(key(note)))) {
        removed.add(note);
      }
      return true;
    }

    /** CRB p. 88: Accurate and Inaccurate cancel (Reliable and Unreliable too, CRB p. 90); never gained twice. */
    private void gainQuality(@NonNull String quality) {
      String opposite = OPPOSITES.get(key(quality));
      if (opposite != null && qualities.removeIf(present -> key(present).equals(opposite))) {
        return;
      }
      if (qualities.stream().noneMatch(present -> key(present).equals(key(quality)))) {
        qualities.add(quality);
      }
    }

    /** CRB p. 88: every Piercing gained raises the rating by 1; other effects are not gained twice. */
    private void gainEffect(@NonNull String effect) {
      if (PIERCING.matcher(effect).matches()) {
        for (int i = 0; i < effects.size(); i++) {
          Matcher piercing = PIERCING.matcher(effects.get(i));
          if (piercing.matches()) {
            effects.set(i, "Piercing " + (Integer.parseInt(piercing.group(1)) + 1));
            return;
          }
        }
      }
      if (effects.stream().noneMatch(present -> key(present).equals(key(effect)))) {
        effects.add(effect);
      }
    }

    private void removeEffect(@NonNull String effect) {
      effects.removeIf(present -> key(present).equals(key(effect)));
    }

    /** The fire rate with the changes; their sum stays within 0–6 (CRB p. 89). */
    @Nullable Integer fireRate(@Nullable Integer base) {
      if (rateChange == 0) {
        return base;
      }
      return Math.clamp((long) (base == null ? 0 : base) + rateChange, 0, MAX_FIRE_RATE);
    }
  }

  private @NonNull String moddedName(@NonNull List<WeaponModData> mods, @Nullable String stockName) {
    boolean stock = mods.stream().anyMatch(mod -> mod.slot() == ModSlot.STOCK);
    StringBuilder text = new StringBuilder();
    for (WeaponModData mod : mods) {
      String prefix = mod.namePrefix();
      if (prefix != null) {
        text.append(prefix).append(' ');
      }
    }
    return text.append(stock && stockName != null ? stockName : name).toString();
  }

  /** Weight with the mods' change; a {@code <1} weapon gets the added pounds, never below nothing. */
  private static @NonNull String addWeight(@Nullable String printed, int change) {
    int pounds = Weight.pounds(printed) + change;
    if (pounds > 0) {
      return Integer.toString(pounds);
    }
    return LIGHT.equals(printed) ? LIGHT : "0";
  }

  /** The declaration order of {@link WeaponRange} is the order of the range steps (CRB p. 90). */
  @SuppressWarnings("EnumOrdinal")
  private static @NonNull WeaponRange step(@NonNull WeaponRange range, int steps) {
    WeaponRange[] ranges = WeaponRange.values();
    return ranges[Math.clamp((long) range.ordinal() + steps, 0, ranges.length - 1)];
  }

  /** Compares names regardless of case and of the book's "Close-Quarters" / "Close Quarters" spellings. */
  private static @NonNull String key(@NonNull String value) {
    return value.strip().replace('-', ' ').toLowerCase(Locale.ROOT);
  }

  private static @NonNull List<String> list(@NonNull String text) {
    return Arrays.stream(text.split(",")).map(String::strip).filter(part -> !part.isEmpty()).toList();
  }
}
