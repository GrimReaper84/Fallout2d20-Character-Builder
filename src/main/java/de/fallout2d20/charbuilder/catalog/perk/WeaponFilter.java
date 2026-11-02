package de.fallout2d20.charbuilder.catalog.perk;

import de.fallout2d20.charbuilder.catalog.weapon.WeaponKind;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Which weapons a perk's weapon bonus applies to; every condition given must hold, an empty or missing one is any.
 *
 * <p>Terms of the perks (CRB pp. 59–73, WG pp. 44–53) as the catalog writes them: the weapon type is the attack skill
 * (heavy weapons and big guns are Big Guns, CRB p. 26; energy weapons are Energy Weapons; bows and crossbows are
 * Athletics); a weapon is two-handed when it has the Two-Handed quality after its mods; the kinds the tables do not
 * mark are {@link WeaponKind}s. Ranged perks name Throwing and Explosives too even where they need a fire rate: no
 * catalogued thrown weapon or explosive has one, but a custom weapon may.
 *
 * @param skills attack skills of the weapons, any of them; empty for any weapon
 * @param twoHanded {@code true} for two-handed weapons only, {@code false} for one-handed ones only, {@code null} for
 *     both
 * @param minFireRate lowest fire rate; the weapon needs a fire rate, so {@code 0} means any weapon with one (Quick
 *     Hands); {@code null} for no bound
 * @param maxFireRate highest fire rate; the weapon needs a fire rate; {@code null} for no bound
 * @param kinds kinds of the weapon, any of them; empty for any
 * @param qualities qualities the weapon needs, any of them, without rating (e.g. {@code Blast}); empty for any
 * @param weapons catalog ids of the weapons, any of them (Basher: the Gun Bash rows); empty for any
 * @param radiation whether the weapon must deal Radiation damage or have the Radioactive damage effect
 */
public record WeaponFilter(
    @NonNull Set<Skill> skills,
    @Nullable Boolean twoHanded,
    @Nullable Integer minFireRate,
    @Nullable Integer maxFireRate,
    @NonNull Set<WeaponKind> kinds,
    @NonNull List<String> qualities,
    @NonNull List<String> weapons,
    boolean radiation) {

  /** Every weapon. */
  public static final @NonNull WeaponFilter ANY = new WeaponFilter(Set.of(), null, null, null, Set.of(), List.of(),
      List.of(), false);

  public WeaponFilter {
    skills = skills == null || skills.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(skills));
    kinds = WeaponKind.copyOf(kinds);
    qualities = qualities == null ? List.of() : List.copyOf(qualities);
    weapons = weapons == null ? List.of() : List.copyOf(weapons);
  }

  /** Whether the bonus applies to the weapon. */
  public boolean matches(@NonNull FilteredWeapon weapon) {
    if (!skills.isEmpty() && (weapon.skill() == null || !skills.contains(weapon.skill()))) {
      return false;
    }
    if (twoHanded != null && twoHanded != weapon.isTwoHanded()) {
      return false;
    }
    if (minFireRate != null || maxFireRate != null) {
      Integer fireRate = weapon.fireRate();
      if (fireRate == null || (minFireRate != null && fireRate < minFireRate)
          || (maxFireRate != null && fireRate > maxFireRate)) {
        return false;
      }
    }
    if (!kinds.isEmpty() && kinds.stream().noneMatch(weapon.kinds()::contains)) {
      return false;
    }
    if (!qualities.isEmpty() && qualities.stream().noneMatch(weapon::hasQuality)) {
      return false;
    }
    if (!weapons.isEmpty() && (weapon.id() == null || !weapons.contains(weapon.id()))) {
      return false;
    }
    return !radiation || weapon.dealsRadiation();
  }
}
