package de.fallout2d20.charbuilder.model.character.weapon;

import de.fallout2d20.charbuilder.catalog.weapon.DamageType;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * What a character rolls with a weapon, computed on every call (never stored).
 *
 * <p>CRB p. 27: the attack is a test of the skill of the weapon type with its governing attribute (STR +
 * Melee Weapons, AGI + Small Guns, END + Big Guns, PER + Energy Weapons, PER + Explosives, AGI + Throwing,
 * STR + Unarmed), so the target number is attribute + skill rank; a catalog weapon may name another
 * attribute (WOA p. 22: Self-Destruct, END + Explosives). CRB p. 13: with a Tag skill every d20
 * up to the skill rank is a critical success, otherwise only a 1. CRB p. 88: melee and unarmed attacks
 * add the melee bonus (CRB p. 48) to the damage. Perk damage bonuses and mods are not included (user
 * decision).
 *
 * @param name weapon name
 * @param skill the attack skill; {@code null} if unknown (custom weapon without skill, unknown id)
 * @param attribute the attribute rolled with the skill; {@code null} without skill
 * @param targetNumber attribute + skill rank; {@code null} without skill
 * @param criticalRange highest d20 result that is a critical success; {@code null} without skill
 * @param damage damage rating in CD
 * @param meleeBonus bonus CD from the melee bonus (melee and unarmed weapons only)
 * @param effects damage effects as text, empty for none
 * @param damageType damage type as text
 * @param fireRate fire rate; {@code null} for none
 * @param range ideal range; {@code null} for none
 * @param qualities qualities as text, empty for none
 * @param ammunition ids of the ammunition types the weapon uses
 */
public record WeaponAttack(
    @NonNull String name,
    @Nullable Skill skill,
    @Nullable Attribute attribute,
    @Nullable Integer targetNumber,
    @Nullable Integer criticalRange,
    int damage,
    int meleeBonus,
    @NonNull String effects,
    @NonNull String damageType,
    @Nullable Integer fireRate,
    @Nullable WeaponRange range,
    @NonNull String qualities,
    @NonNull List<String> ammunition) {

  public WeaponAttack {
    ammunition = List.copyOf(ammunition);
  }

  /** The attack with a carried weapon (catalog or custom). */
  /** Damage as shown in the app and on the sheet, e.g. "4 CD" or "4 CD + 1 CD" with the melee bonus. */
  public @NonNull String damageText() {
    return damage + " CD" + (meleeBonus > 0 ? " + " + meleeBonus + " CD" : "");
  }

  public static @NonNull WeaponAttack of(@NonNull Character character, @NonNull CarriedWeapon weapon) {
    CustomWeapon custom = weapon.custom();
    if (custom != null) {
      String ammunition = custom.ammunition();
      Skill skill = custom.skill();
      return create(character, custom.name(), skill, skill == null ? null : skill.governingAttribute(),
          custom.damage(), custom.effects(),
          custom.damageType(), custom.fireRate(), custom.range(), custom.qualities(),
          ammunition == null ? List.of() : List.of(ammunition));
    }
    Optional<WeaponData> data = weapon.data();
    if (data.isEmpty()) {
      return create(character, weapon.displayName(), null, null, 0, "", "", null, null, "", List.of());
    }
    return of(character, data.get());
  }

  /** The attack with a catalog weapon, e.g. the Unarmed Strike every character has (CRB p. 111). */
  public static @NonNull WeaponAttack of(@NonNull Character character, @NonNull WeaponData weapon) {
    return create(character, weapon.name(), weapon.skill(), weapon.attackAttribute(), weapon.damage(),
        String.join(", ", weapon.damageEffects()),
        weapon.damageTypes().stream().map(DamageType::displayName).collect(Collectors.joining("/")),
        weapon.fireRate(), weapon.range(), String.join(", ", weapon.qualities()), weapon.ammunition());
  }

  private static @NonNull WeaponAttack create(@NonNull Character character, @NonNull String name,
      @Nullable Skill skill, @Nullable Attribute attribute, int damage, @NonNull String effects, @NonNull String damageType,
      @Nullable Integer fireRate, @Nullable WeaponRange range, @NonNull String qualities,
      @NonNull List<String> ammunition) {
    Integer targetNumber = null;
    Integer criticalRange = null;
    int meleeBonus = 0;
    if (skill != null && attribute != null) {
      int rank = character.skillRank(skill);
      targetNumber = character.attributeValue(attribute) + rank;
      criticalRange = character.isTagged(skill) ? Math.max(1, rank) : 1;
      if (skill == Skill.MELEE_WEAPONS || skill == Skill.UNARMED) {
        meleeBonus = DerivedStats.of(character).meleeBonusDice();
      }
    }
    return new WeaponAttack(name, skill, attribute, targetNumber, criticalRange, damage, meleeBonus, effects, damageType,
        fireRate, range, qualities, ammunition);
  }
}
