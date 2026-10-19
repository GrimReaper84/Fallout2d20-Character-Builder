package de.fallout2d20.charbuilder.model.character.weapon;

import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * add the melee bonus (CRB p. 48) to the damage. Installed mods are applied ({@link ModdedWeapon}); perk
 * damage bonuses are not included (user decision).
 *
 * @param name weapon name
 * @param moddedName weapon name with the mods' prefixes; equals {@code name} without mods
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
    @NonNull String moddedName,
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

  /** The Thrown quality with its range letter, e.g. {@code Thrown (M)} (CRB p. 90). */
  private static final @NonNull Pattern THROWN = Pattern.compile("\\bThrown \\(([CMLX])\\)");

  public WeaponAttack {
    ammunition = List.copyOf(ammunition);
  }

  /**
   * The range the weapon is used at: its table range, else the range of its Thrown quality (CRB p. 90: a
   * {@code Thrown (C)} weapon can be thrown as a ranged attack with ideal range Close), as the thrown
   * weapons and grenades of the tables have no range of their own; {@code null} for neither (melee weapons,
   * mines that are only placed).
   */
  public @Nullable WeaponRange effectiveRange() {
    if (range != null) {
      return range;
    }
    Matcher thrown = THROWN.matcher(qualities);
    return thrown.find() ? WeaponRange.byCode(thrown.group(1)).orElse(null) : null;
  }

  /** Damage as shown in the app and on the sheet, e.g. "4 CD" or "4 CD + 1 CD" with the melee bonus. */
  public @NonNull String damageText() {
    return damage + " CD" + (meleeBonus > 0 ? " + " + meleeBonus + " CD" : "");
  }

  /** The attack with a carried weapon (catalog or custom) and its mods. */
  public static @NonNull WeaponAttack of(@NonNull PlayerCharacter character, @NonNull CarriedWeapon weapon) {
    Skill skill = weapon.skill().orElse(null);
    Attribute attribute = weapon.data().map(WeaponData::attackAttribute)
        .orElse(skill == null ? null : skill.governingAttribute());
    return create(character, ModdedWeapon.of(weapon).orElseGet(() -> ModdedWeapon.unknown(weapon.displayName())),
        skill, attribute);
  }

  /** The attack with a catalog weapon, e.g. the Unarmed Strike every character has (CRB p. 111). */
  public static @NonNull WeaponAttack of(@NonNull PlayerCharacter character, @NonNull WeaponData weapon) {
    return create(character, ModdedWeapon.of(weapon), weapon.skill(), weapon.attackAttribute());
  }

  private static @NonNull WeaponAttack create(@NonNull PlayerCharacter character, @NonNull ModdedWeapon weapon,
      @Nullable Skill skill, @Nullable Attribute attribute) {
    Integer targetNumber = null;
    Integer criticalRange = null;
    int meleeBonus = 0;
    if (skill != null && attribute != null) {
      int rank = character.abilities().skillRank(skill);
      targetNumber = character.abilities().attributeValue(attribute) + rank;
      criticalRange = character.abilities().isTagged(skill) ? Math.max(1, rank) : 1;
      if (skill == Skill.MELEE_WEAPONS || skill == Skill.UNARMED) {
        meleeBonus = DerivedStats.of(character).meleeBonusDice();
      }
    }
    return new WeaponAttack(weapon.name(), weapon.moddedName(), skill, attribute, targetNumber, criticalRange,
        weapon.damage(), meleeBonus, String.join(", ", weapon.damageEffects()), weapon.damageType(), weapon.fireRate(),
        weapon.range(), String.join(", ", weapon.qualities()), weapon.ammunition());
  }
}
