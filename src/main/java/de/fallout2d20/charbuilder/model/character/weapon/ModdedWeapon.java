package de.fallout2d20.charbuilder.model.character.weapon;

import de.fallout2d20.charbuilder.catalog.weapon.DamageType;
import de.fallout2d20.charbuilder.catalog.weapon.ModEffect;
import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.derived.Weight;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
 * CRB p. 88: a quality is not gained twice; Accurate and Inaccurate cancel each other; every Piercing a mod
 * gives raises the rating by 1; range moves in steps between Close and Extreme. Fire rate stays within 0–6
 * (CRB p. 89) and damage at 0 or more; these limits apply to the sum of all changes, so the order of the
 * mods does not matter for them. Gained and removed qualities and effects do follow slot order. Weight and cost add the mods' values (user decision 2026-10-17); a
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
    @NonNull List<String> specials) {

  private static final int MAX_FIRE_RATE = 6;
  private static final @NonNull String LIGHT = "<1";
  private static final @NonNull Pattern PIERCING = Pattern.compile("Piercing (\\d+)", Pattern.CASE_INSENSITIVE);
  private static final @NonNull Map<String, String> OPPOSITES = Map.of("accurate", "inaccurate", "inaccurate", "accurate");

  public ModdedWeapon {
    damageEffects = List.copyOf(damageEffects);
    qualities = List.copyOf(qualities);
    ammunition = List.copyOf(ammunition);
    specials = List.copyOf(specials);
  }

  /** The weapon with its known installed mods; empty for an id outside the catalog. */
  public static @NonNull Optional<ModdedWeapon> of(@NonNull CarriedWeapon weapon) {
    return unmodded(weapon).map(base -> base.apply(WeaponMods.installedMods(weapon), stockName(weapon)));
  }

  /** The values of a catalog row, e.g. of the Unarmed Strike every character has. */
  public static @NonNull ModdedWeapon of(@NonNull WeaponData data) {
    return new ModdedWeapon(data.name(), data.name(), data.damage(), data.damageEffects(),
        data.damageTypes().stream().map(DamageType::displayName).collect(Collectors.joining("/")), data.fireRate(),
        data.range(), data.qualities(), data.ammunition(), data.weight(), data.cost(), List.of());
  }

  /**
   * A weapon whose catalog entry is unknown: only its name, no values (the attack then shows no target
   * number).
   */
  public static @NonNull ModdedWeapon unknown(@NonNull String name) {
    return new ModdedWeapon(name, name, 0, List.of(), "", null, null, List.of(), List.of(), null, null, List.of());
  }

  /** The weapon's values without mods (also shown as the base value next to a changed one). */
  public static @NonNull Optional<ModdedWeapon> unmodded(@NonNull CarriedWeapon weapon) {
    CustomWeapon custom = weapon.custom();
    if (custom != null) {
      String ammunition = custom.ammunition();
      return Optional.of(new ModdedWeapon(custom.name(), custom.name(), custom.damage(), list(custom.effects()),
          custom.damageType(), custom.fireRate(), custom.range(), list(custom.qualities()),
          ammunition == null ? List.of() : List.of(ammunition), null, null, List.of()));
    }
    return weapon.data().map(ModdedWeapon::of);
  }

  private static @Nullable String stockName(@NonNull CarriedWeapon weapon) {
    return weapon.data().map(WeaponData::stockName).orElse(null);
  }

  private @NonNull ModdedWeapon apply(@NonNull List<WeaponModData> mods, @Nullable String stockName) {
    int newDamage = damage;
    String newType = damageType;
    List<String> newAmmunition = ammunition;
    for (WeaponModData mod : mods) {
      for (ModEffect effect : mod.effects()) {
        switch (effect.type()) {
          case SET_DAMAGE -> newDamage = effect.number();
          case SET_DAMAGE_TYPE -> newType = DamageType.valueOf(effect.text()).displayName();
          case SET_AMMUNITION -> newAmmunition = List.of(effect.text());
          default -> { }
        }
      }
    }
    int rateChange = 0;
    int rangeSteps = 0;
    List<String> newEffects = new ArrayList<>(damageEffects);
    List<String> newQualities = new ArrayList<>(qualities);
    List<String> newSpecials = new ArrayList<>();
    int weightChange = 0;
    boolean weightChanged = false;
    int costChange = 0;
    boolean costChanged = false;
    for (WeaponModData mod : mods) {
      for (ModEffect effect : mod.effects()) {
        switch (effect.type()) {
          case DAMAGE -> newDamage += effect.number();
          case FIRE_RATE -> rateChange += effect.number();
          case RANGE -> rangeSteps += effect.number();
          case GAIN_QUALITY -> gainQuality(newQualities, effect.text());
          case REMOVE_QUALITY -> remove(newQualities, effect.text());
          case GAIN_EFFECT -> gainEffect(newEffects, effect.text());
          case REMOVE_EFFECT -> remove(newEffects, effect.text());
          case SPECIAL -> newSpecials.add(mod.name() + ": " + effect.text());
          case SET_DAMAGE, SET_DAMAGE_TYPE, SET_AMMUNITION, ALLOWS_SLOT -> { }
        }
      }
      Integer modWeight = mod.weight();
      if (modWeight != null) {
        weightChange += modWeight;
        weightChanged = true;
      }
      Integer modCost = mod.cost();
      if (modCost != null) {
        costChange += modCost;
        costChanged = true;
      }
    }
    Integer newRate = fireRate;
    if (rateChange != 0) {
      newRate = Math.clamp((fireRate == null ? 0 : fireRate) + rateChange, 0, MAX_FIRE_RATE);
    }
    WeaponRange newRange = range == null ? null : step(range, rangeSteps);
    Integer newCost = cost;
    if (costChanged) {
      newCost = (cost == null ? 0 : cost) + costChange;
    }
    return new ModdedWeapon(name, moddedName(mods, stockName), Math.max(0, newDamage), newEffects, newType, newRate, newRange,
        newQualities, newAmmunition, weightChanged ? addWeight(weight, weightChange) : weight, newCost, newSpecials);
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
  private static @Nullable String addWeight(@Nullable String printed, int change) {
    int pounds = Weight.pounds(printed) + change;
    if (pounds > 0) {
      return Integer.toString(pounds);
    }
    return LIGHT.equals(printed) ? LIGHT : "0";
  }

  private static @NonNull WeaponRange step(@NonNull WeaponRange range, int steps) {
    WeaponRange[] ranges = WeaponRange.values();
    return ranges[Math.clamp(range.ordinal() + steps, 0, ranges.length - 1)];
  }

  /** CRB p. 88: Accurate and Inaccurate cancel; a quality is never gained twice. */
  private static void gainQuality(@NonNull List<String> qualities, @NonNull String quality) {
    String opposite = OPPOSITES.get(key(quality));
    if (opposite != null && qualities.removeIf(present -> key(present).equals(opposite))) {
      return;
    }
    if (qualities.stream().noneMatch(present -> key(present).equals(key(quality)))) {
      qualities.add(quality);
    }
  }

  /** CRB p. 88: every Piercing gained raises the rating by 1; other effects are not gained twice. */
  private static void gainEffect(@NonNull List<String> effects, @NonNull String effect) {
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

  private static void remove(@NonNull List<String> values, @NonNull String value) {
    values.removeIf(present -> key(present).equals(key(value)));
  }

  /** Compares names regardless of case and of the book's "Close-Quarters" / "Close Quarters" spellings. */
  private static @NonNull String key(@NonNull String value) {
    return value.strip().replace('-', ' ').toLowerCase(Locale.ROOT);
  }

  private static @NonNull List<String> list(@NonNull String text) {
    return Arrays.stream(text.split(",")).map(String::strip).filter(part -> !part.isEmpty()).toList();
  }
}
