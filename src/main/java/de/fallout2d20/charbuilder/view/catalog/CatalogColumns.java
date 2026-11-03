package de.fallout2d20.charbuilder.view.catalog;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.Coverage;
import de.fallout2d20.charbuilder.catalog.weapon.DamageType;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.derived.Weight;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponAttack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;

/**
 * The columns of the weapon and apparel catalog lists, shared by the add dialogs and the GM compendium (H2), so both
 * show the same values.
 */
public final class CatalogColumns {

  /** Sorts radiation immunity above every resistance. */
  private static final int IMMUNE = Integer.MAX_VALUE;

  private CatalogColumns() {}

  /** A weapon list: Name and the given category, Skill, Damage, Range, Weight, Source; the other values as tooltip. */
  public static @NonNull CatalogPage<WeaponData> weapons(@NonNull List<WeaponData> weapons,
      @NonNull Function<WeaponData, String> categoryOf, @NonNull List<String> categories) {
    CatalogPage<WeaponData> page = new CatalogPage<>(weapons, WeaponData::name, categoryOf, categories,
        CatalogColumns::weaponDetails);
    page.addColumn("Skill", weapon -> weapon.skill().displayName());
    page.addNumberColumn("Damage", WeaponData::damage, CatalogColumns::damageText);
    page.addColumn("Range", CatalogColumns::rangeText);
    page.addNumberColumn("Weight", weapon -> Weight.pounds(weapon.weight()),
        weapon -> weapon.weight() == null ? "–" : weapon.weight());
    page.addColumn("Source", weapon -> weapon.sources().getFirst().toString());
    return page;
  }

  /** An apparel list: Name, Kind, Covers, physical/energy/radiation resistance, Source. */
  public static @NonNull CatalogPage<ApparelData> apparel(@NonNull List<ApparelData> items,
      @NonNull Function<ApparelData, String> categoryOf, @NonNull List<String> categories) {
    CatalogPage<ApparelData> page = new CatalogPage<>(items, ApparelData::name, categoryOf, categories, _ -> "");
    page.addColumn("Kind", item -> item.kind().displayName());
    page.addColumn("Covers", CatalogColumns::coverageText);
    page.addNumberColumn("Phys.", ApparelData::physical, item -> Integer.toString(item.physical()));
    page.addNumberColumn("Energy", ApparelData::energy, item -> Integer.toString(item.energy()));
    page.addNumberColumn("Rad.", item -> item.radiationImmune() ? IMMUNE : item.radiation(),
        CatalogColumns::radiationText);
    page.addColumn("Source", item -> item.sources().getFirst().toString());
    return page;
  }

  /** The damage as in the weapon table, e.g. "4 CD". */
  public static @NonNull String damageText(@NonNull WeaponData weapon) {
    return weapon.specialDamage() ? WeaponAttack.SPECIAL_DAMAGE : weapon.damage() + " CD";
  }

  /** The effective range like the Weapons tree: the table range, else the Thrown range, e.g. "Medium (thrown)". */
  public static @NonNull String rangeText(@NonNull WeaponData weapon) {
    WeaponRange range = WeaponAttack.effectiveRange(weapon);
    if (range == null) {
      return "–";
    }
    return range.displayName() + (weapon.range() == null ? " (thrown)" : "");
  }

  /** The damage types, e.g. "Physical/Energy". */
  public static @NonNull String damageTypesText(@NonNull WeaponData weapon) {
    return weapon.damageTypes().stream().map(DamageType::displayName).collect(Collectors.joining("/"));
  }

  /** The covered locations, e.g. "Arm, Leg". */
  public static @NonNull String coverageText(@NonNull ApparelData item) {
    return item.coverage().stream().map(Coverage::displayName).collect(Collectors.joining(", "));
  }

  /** The radiation resistance, or "Immune". */
  public static @NonNull String radiationText(@NonNull ApparelData item) {
    return item.radiationImmune() ? "Immune" : Integer.toString(item.radiation());
  }

  /** The row tooltip: the values the table has no column for. */
  private static @NonNull String weaponDetails(@NonNull WeaponData weapon) {
    List<String> lines = new ArrayList<>();
    if (!weapon.damageEffects().isEmpty()) {
      lines.add("Effects: " + String.join(", ", weapon.damageEffects()));
    }
    lines.add("Damage type: " + damageTypesText(weapon));
    if (weapon.fireRate() != null) {
      lines.add("Fire rate: " + weapon.fireRate());
    }
    if (!weapon.qualities().isEmpty()) {
      lines.add("Qualities: " + String.join(", ", weapon.qualities()));
    }
    return String.join("\n", lines);
  }
}
