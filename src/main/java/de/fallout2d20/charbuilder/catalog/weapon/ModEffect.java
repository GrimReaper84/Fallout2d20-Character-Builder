package de.fallout2d20.charbuilder.catalog.weapon;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One change of a weapon mod: a number for the damage, fire rate and range kinds, a text (quality, damage
 * effect, damage type, ammunition id, slot, rule) for the others.
 *
 * @param type what changes
 * @param value the amount for {@link ModEffectType#DAMAGE}, {@code SET_DAMAGE}, {@code FIRE_RATE}, {@code RANGE}
 * @param text the name or rule for every other type
 */
public record ModEffect(
    @JsonProperty(required = true) @NonNull ModEffectType type,
    @Nullable Integer value,
    @Nullable String text) {

  public ModEffect {
    boolean numeric = switch (type) {
      case DAMAGE, SET_DAMAGE, FIRE_RATE, RANGE -> true;
      default -> false;
    };
    if (numeric ? value == null : text == null || text.isBlank()) {
      throw new IllegalArgumentException("Mod effect " + type + " needs a " + (numeric ? "value" : "text"));
    }
    if (text != null && type == ModEffectType.SET_DAMAGE_TYPE) {
      DamageType.valueOf(text);
    }
    if (text != null && type == ModEffectType.ALLOWS_SLOT) {
      ModSlot.valueOf(text);
    }
  }

  /** The effect as shown to the player, e.g. "+1 CD damage", "Gain Accurate", "Range +1 step". */
  public @NonNull String description() {
    return switch (type) {
      case DAMAGE -> signed(number()) + " CD damage";
      case SET_DAMAGE -> "Damage " + number() + " CD";
      case FIRE_RATE -> signed(number()) + " Fire Rate";
      case RANGE -> "Range " + signed(number()) + (Math.abs(number()) == 1 ? " step" : " steps");
      case GAIN_QUALITY, GAIN_EFFECT -> "Gain " + text();
      case REMOVE_QUALITY, REMOVE_EFFECT -> "Remove " + text();
      case SET_DAMAGE_TYPE -> "Damage type " + DamageType.valueOf(text()).displayName();
      case SET_AMMUNITION -> "Ammunition " + WeaponCatalog.ammunitionById(text()).map(AmmunitionData::name)
          .orElse(text());
      case ALLOWS_SLOT -> "Allows a " + ModSlot.valueOf(text()).displayName() + " mod";
      case SPECIAL -> text();
    };
  }

  /** The value of a numeric effect. */
  public int number() {
    if (value == null) {
      throw new IllegalStateException(type + " has no value");
    }
    return value;
  }

  /** The text of a non-numeric effect. */
  @Override
  public @NonNull String text() {
    if (text == null) {
      throw new IllegalStateException(type + " has no text");
    }
    return text;
  }

  private static @NonNull String signed(int number) {
    return number > 0 ? "+" + number : Integer.toString(number);
  }
}
