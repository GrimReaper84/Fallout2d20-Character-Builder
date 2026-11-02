package de.fallout2d20.charbuilder.model.character.weapon;

import de.fallout2d20.charbuilder.catalog.perk.FilteredWeapon;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponKind;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponQuality;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import java.util.List;
import java.util.Set;
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
 * add the melee bonus (CRB p. 48) to the damage, except a weapon without it (Chainsaw, WG p. 78). Installed mods
 * are applied ({@link ModdedWeapon}), then the changes of the character's perks ({@link PerkWeaponBonuses}, plan C8);
 * situational perk bonuses are only listed as hints. WG p. 57: a weapon with
 * Recoil (X) is harder to use when the character's STR is below X; WG pp. 79–81: some explosives have "Special"
 * damage instead of a rating.
 *
 * @param name weapon name
 * @param moddedName weapon name with the legendary mark and property and the mods' prefixes; equals {@code name} for
 *     an ordinary weapon without mods
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
 * @param specialDamage whether the damage is "Special" (the rating means nothing; the type is empty)
 * @param unmetRecoil the X of the weapon's Recoil (X) when the character's STR is below it; {@code null} otherwise
 * @param perkChanges the perks' changes included in the values ({@link PerkWeaponChange#text()}, e.g.
 *     {@code Commando 2: +2 CD}); empty for none
 * @param perkHints the situational perk bonuses for this weapon, not included, one per perk; empty for none
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
    @NonNull List<String> ammunition,
    boolean specialDamage,
    @Nullable Integer unmetRecoil,
    @NonNull List<PerkWeaponChange> perkChanges,
    @NonNull List<PerkWeaponHint> perkHints) {

  /** What weapons with "Special" damage show instead of a damage rating (WG pp. 79–81). */
  public static final @NonNull String SPECIAL_DAMAGE = "Special";

  /** The Thrown quality with its range letter, e.g. {@code Thrown (M)} (CRB p. 90). */
  private static final @NonNull Pattern THROWN = Pattern.compile("\\bThrown \\(([CMLX])\\)");

  public WeaponAttack {
    ammunition = List.copyOf(ammunition);
    perkChanges = List.copyOf(perkChanges);
    perkHints = List.copyOf(perkHints);
  }

  /**
   * The range the weapon is used at: its table range, else the range of its Thrown quality (CRB p. 90: a
   * {@code Thrown (C)} weapon can be thrown as a ranged attack with ideal range Close), as the thrown
   * weapons and grenades of the tables have no range of their own; {@code null} for neither (melee weapons,
   * mines that are only placed).
   */
  public @Nullable WeaponRange effectiveRange() {
    return effectiveRange(range, qualities);
  }

  /** {@link #effectiveRange()} of a catalog row, e.g. for the add dialog. */
  public static @Nullable WeaponRange effectiveRange(@NonNull WeaponData weapon) {
    return effectiveRange(weapon.range(), String.join(", ", weapon.qualities()));
  }

  /** {@link #effectiveRange()} of a weapon with this table range and these qualities. */
  public static @Nullable WeaponRange effectiveRange(@Nullable WeaponRange range, @NonNull String qualities) {
    if (range != null) {
      return range;
    }
    Matcher thrown = THROWN.matcher(qualities);
    return thrown.find() ? WeaponRange.byCode(thrown.group(1)).orElse(null) : null;
  }

  /** Damage as shown in the app and on the sheet, e.g. "4 CD", "4 CD + 1 CD" with the melee bonus, "Special". */
  public @NonNull String damageText() {
    if (specialDamage) {
      return SPECIAL_DAMAGE;
    }
    return damage + " CD" + (meleeBonus > 0 ? " + " + meleeBonus + " CD" : "");
  }

  /** The attack with a carried weapon (catalog or custom) and its mods. */
  public static @NonNull WeaponAttack of(@NonNull PlayerCharacter character, @NonNull CarriedWeapon weapon) {
    Skill skill = weapon.skill().orElse(null);
    Attribute attribute = weapon.data().map(WeaponData::attackAttribute)
        .orElse(skill == null ? null : skill.governingAttribute());
    return create(character, ModdedWeapon.of(weapon).orElseGet(() -> ModdedWeapon.unknown(weapon.displayName())),
        skill, attribute, weapon.data().orElse(null), weapon.kinds());
  }

  /** The attack with a catalog weapon, e.g. the Unarmed Strike every character has (CRB p. 111). */
  public static @NonNull WeaponAttack of(@NonNull PlayerCharacter character, @NonNull WeaponData weapon) {
    return create(character, ModdedWeapon.of(weapon), weapon.skill(), weapon.attackAttribute(), weapon, weapon.kinds());
  }

  /**
   * @param modded the weapon with its mods and legendary property
   * @param data the catalog row; {@code null} for a custom weapon or an id outside the catalog
   * @param kinds the weapon's kinds, including its mods'
   */
  private static @NonNull WeaponAttack create(@NonNull PlayerCharacter character, @NonNull ModdedWeapon modded,
      @Nullable Skill skill, @Nullable Attribute attribute, @Nullable WeaponData data, @NonNull Set<WeaponKind> kinds) {
    PerkWeaponResult perks = PerkWeaponBonuses.of(character, new FilteredWeapon(
        data == null ? null : data.id(), skill, modded.fireRate(), modded.qualities(), modded.damageEffects(),
        modded.damageType(), kinds));
    ModdedWeapon weapon = modded.withPerks(perks.changes());
    Integer targetNumber = null;
    Integer criticalRange = null;
    int meleeBonus = 0;
    if (skill != null && attribute != null) {
      int rank = character.abilities().skillRank(skill);
      targetNumber = character.abilities().attributeValue(attribute) + rank;
      criticalRange = character.abilities().isTagged(skill) ? Math.max(1, rank) : 1;
      if ((skill == Skill.MELEE_WEAPONS || skill == Skill.UNARMED) && (data == null || !data.noMeleeBonus())) {
        meleeBonus = DerivedStats.of(character).meleeBonusDice();
      }
    }
    String qualities = String.join(", ", weapon.qualities());
    Integer recoil = WeaponQuality.parse(qualities).ratings().get(WeaponQuality.RECOIL);
    Integer unmetRecoil = recoil != null && character.abilities().attributeValue(Attribute.STRENGTH) < recoil
        ? recoil : null;
    boolean specialDamage = data != null && data.specialDamage();
    return new WeaponAttack(weapon.name(), weapon.moddedName(), skill, attribute, targetNumber, criticalRange,
        weapon.damage(), meleeBonus, String.join(", ", weapon.damageEffects()),
        weapon.damageType(), weapon.fireRate(), weapon.range(), qualities,
        weapon.ammunition(), specialDamage, unmetRecoil,
        perks.changes(), perks.hints());
  }
}
