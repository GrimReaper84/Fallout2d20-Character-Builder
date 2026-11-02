package de.fallout2d20.charbuilder.catalog.perk;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * What a perk does to the weapons it names: either a change computed into their values (damage, Piercing, fire rate,
 * gained damage effects, removed qualities) or, for a bonus that depends on the situation (sneak attacks, aiming,
 * enemy type, HP or radiation), a hint shown with the weapon and never computed.
 *
 * @param fromRank the perk rank from which the entry applies (Iron Fist rank 2: Vicious); missing in JSON means 1
 * @param weapons the weapons it applies to; missing in JSON means all
 * @param damage CD added to the damage
 * @param piercing Piercing gained, or added to an existing Piercing rating
 * @param fireRate fire rate added
 * @param perRank whether the numbers are multiplied by the perk's rank (Commando: +1 CD per rank)
 * @param gainedEffects damage effects gained, e.g. {@code Vicious}
 * @param removedQualities qualities the weapons lose (Licensed Plumber: Unreliable)
 * @param hint the situational bonus in own words, where {@code {rank}} stands for the perk's rank and {@code {rank+N}}
 *     for the rank plus N (Bullet Shield: +2 DR at rank 1); {@code null} for a computed entry
 */
public record PerkWeaponBonus(
    int fromRank,
    @NonNull WeaponFilter weapons,
    int damage,
    int piercing,
    int fireRate,
    boolean perRank,
    @NonNull List<String> gainedEffects,
    @NonNull List<String> removedQualities,
    @Nullable String hint) {

  private static final @NonNull Pattern RANK = Pattern.compile("\\{rank(?:\\+(\\d+))?}");

  public PerkWeaponBonus {
    fromRank = fromRank == 0 ? 1 : fromRank;
    weapons = weapons == null ? WeaponFilter.ANY : weapons;
    gainedEffects = gainedEffects == null ? List.of() : List.copyOf(gainedEffects);
    removedQualities = removedQualities == null ? List.of() : List.copyOf(removedQualities);
    if (hint != null && hint.isBlank()) {
      hint = null;
    }
    boolean hasValues = damage != 0 || piercing != 0 || fireRate != 0 || !gainedEffects.isEmpty()
        || !removedQualities.isEmpty();
    if (fromRank < 1 || damage < 0 || piercing < 0 || fireRate < 0 || (perRank && fromRank != 1)
        || hasValues == (hint != null)) {
      throw new IllegalArgumentException("A weapon bonus is either a computed change or a hint, with valid values");
    }
  }

  /** Whether the entry is only shown as a hint. */
  public boolean isHint() {
    return hint != null;
  }

  /** The hint at the perk's {@code rank}, its rank placeholders filled in; {@code null} for a computed entry. */
  public @Nullable String hintAt(int rank) {
    if (hint == null) {
      return null;
    }
    Matcher placeholder = RANK.matcher(hint);
    return placeholder.replaceAll(match -> String.valueOf(rank
        + (match.group(1) == null ? 0 : Integer.parseInt(match.group(1)))));
  }

  /** Whether the entry applies at the perk's {@code rank}. */
  public boolean appliesAt(int rank) {
    return rank >= fromRank;
  }

  /** A number of the entry at the perk's {@code rank}: times the rank if {@link #perRank}. */
  public int at(int value, int rank) {
    return perRank ? value * rank : value;
  }
}
