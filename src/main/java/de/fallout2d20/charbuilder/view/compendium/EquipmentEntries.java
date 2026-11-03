package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModSlot;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModTable;
import de.fallout2d20.charbuilder.catalog.item.ItemCatalog;
import de.fallout2d20.charbuilder.catalog.item.ItemCategory;
import de.fallout2d20.charbuilder.catalog.item.ItemData;
import de.fallout2d20.charbuilder.catalog.item.MagazineData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorLocation;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryCatalog;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponType;
import de.fallout2d20.charbuilder.catalog.origin.OriginCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.AmmunitionData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponQuality;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.derived.Weight;
import de.fallout2d20.charbuilder.view.catalog.CatalogColumns;
import de.fallout2d20.charbuilder.view.dialog.ModTexts;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The equipment sections of the GM compendium (H2): weapons, weapon mods, ammunition, apparel, apparel mods, items
 * (with magazines) and legendary properties, built from the catalogs for the enabled books.
 */
final class EquipmentEntries {

  private static final @NonNull String MAGAZINES = "Magazines";
  private static final @NonNull String WEAPON_PROPERTIES = "Weapon properties";
  private static final @NonNull String ARMOR_PROPERTIES = "Armor properties";
  /** Prefixes of the legendary ids, which may repeat between the weapon and the armor table. */
  private static final @NonNull String WEAPON_PREFIX = "weapon:";
  private static final @NonNull String ARMOR_PREFIX = "armor:";

  private EquipmentEntries() {}

  static @NonNull SectionContent weapons(@NonNull Set<Book> books) {
    List<CompendiumEntry> entries = WeaponCatalog.available(books).stream().map(EquipmentEntries::weapon).toList();
    return new SectionContent(Section.WEAPONS, List.of(Column.text("Skill"), Column.number("Damage"),
        Column.text("Range"), Column.number("FR"), Column.number("Weight"), Column.number("Cost"),
        Column.number("Rarity"), Column.text("Source")),
        // Every skill: bows use Athletics (WG), the filter offers only skills that weapons use.
        Arrays.stream(Skill.values()).map(Skill::displayName).toList(),
        entries);
  }

  private static @NonNull CompendiumEntry weapon(@NonNull WeaponData weapon) {
    List<Reference> qualities = WeaponQuality.parse(String.join(", ", weapon.qualities())).qualities().stream()
        .map(quality -> new Reference(Section.QUALITIES, quality.name(), quality.displayName())).toList();
    List<DetailLine> details = new Details()
        .value("Skill", weapon.skill().displayName())
        .value("Damage", CatalogColumns.damageText(weapon))
        .list("Damage effects", weapon.damageEffects())
        .value("Damage type", CatalogColumns.damageTypesText(weapon))
        .value("Fire rate", weapon.fireRate())
        .value("Range", CatalogColumns.rangeText(weapon))
        .list("Qualities", weapon.qualities())
        .links("Quality rules", qualities)
        .value("Weight", weightText(weapon.weight()))
        .value("Cost", Details.amount(weapon.cost(), " caps"))
        .value("Rarity", weapon.rarity())
        .value("Special", weapon.special())
        .links("Ammunition", weapon.ammunition().stream().map(id -> WeaponCatalog.ammunitionById(id)
            .map(ammo -> new Reference(Section.AMMUNITION, ammo.id(), ammo.name()))).flatMap(Optional::stream).toList())
        .links("Mods", weapon.mods().stream().map(id -> WeaponCatalog.modById(id)
            .map(mod -> new Reference(Section.WEAPON_MODS, mod.id(), mod.name()))).flatMap(Optional::stream).toList())
        .links("Built in for", weapon.origins().stream().map(id -> OriginCatalog.byId(id)
            .map(origin -> new Reference(Section.ORIGINS, origin.id(), origin.name()))).flatMap(Optional::stream)
            .toList())
        .source(weapon.sources());
    return new CompendiumEntry(weapon.id(), weapon.name(), weapon.skill().displayName(), List.of(
        Cell.text(weapon.skill().displayName()), Cell.number(weapon.damage(), CatalogColumns.damageText(weapon)),
        Cell.text(CatalogColumns.rangeText(weapon)), Cell.number(weapon.fireRate()), weight(weapon.weight()),
        Cell.number(weapon.cost()), Cell.number(weapon.rarity()), Cell.text(firstSource(weapon.sources()))), details);
  }

  static @NonNull SectionContent weaponMods(@NonNull Set<Book> books) {
    List<WeaponModData> mods = WeaponCatalog.mods().stream().filter(mod -> mod.isAvailableWith(books)).toList();
    List<WeaponData> weapons = WeaponCatalog.available(books);
    List<CompendiumEntry> entries = mods.stream().map(mod -> weaponMod(mod, weapons)).toList();
    return new SectionContent(Section.WEAPON_MODS, List.of(Column.text("Slot"), Column.text("Effects"),
        Column.number("Weight"), Column.number("Cost"), Column.text("Source")),
        mods.stream().map(WeaponModData::tableName).distinct().toList(), entries);
  }

  private static @NonNull CompendiumEntry weaponMod(@NonNull WeaponModData mod, @NonNull List<WeaponData> weapons) {
    List<DetailLine> details = new Details()
        .value("Slot", mod.slot().displayName())
        .value("Table", mod.tableName())
        .value("Effects", ModTexts.effects(mod))
        .value("Name prefix", mod.namePrefix())
        .value("Weight", Details.amount(mod.weight(), " lbs."))
        .value("Cost", Details.amount(mod.cost(), " caps"))
        .value("Perks", ModTexts.perks(mod.perks()))
        .value("Installed with", mod.skill().displayName())
        .links("Fits", weapons.stream().filter(weapon -> weapon.mods().contains(mod.id()))
            .map(weapon -> new Reference(Section.WEAPONS, weapon.id(), weapon.name())).toList())
        .source(mod.sources());
    return new CompendiumEntry(mod.id(), mod.name(), mod.tableName(), List.of(Cell.text(mod.slot().displayName()),
        Cell.text(ModTexts.effects(mod)), Cell.number(mod.weight()), Cell.number(mod.cost()),
        Cell.text(firstSource(mod.sources()))), details);
  }

  static @NonNull SectionContent ammunition(@NonNull Set<Book> books) {
    List<WeaponData> weapons = WeaponCatalog.available(books);
    List<CompendiumEntry> entries = WeaponCatalog.ammunition().stream()
        .filter(ammo -> isAvailable(ammo.sources(), books)).map(ammo -> ammunition(ammo, weapons)).toList();
    return new SectionContent(Section.AMMUNITION, List.of(Column.text("Found"), Column.number("Weight"),
        Column.number("Cost"), Column.number("Rarity"), Column.text("Source")), List.of(), entries);
  }

  private static @NonNull CompendiumEntry ammunition(@NonNull AmmunitionData ammo, @NonNull List<WeaponData> weapons) {
    List<DetailLine> details = new Details()
        .value("Quantity found", ammo.quantityFound())
        .value("Weight", weightText(ammo.weight()))
        .value("Cost", Details.amount(ammo.cost(), " caps"))
        .value("Rarity", ammo.rarity())
        .value("Special", ammo.special())
        .links("Used by", weapons.stream().filter(weapon -> weapon.ammunition().contains(ammo.id()))
            .map(weapon -> new Reference(Section.WEAPONS, weapon.id(), weapon.name())).toList())
        .source(ammo.sources());
    String found = ammo.quantityFound();
    return new CompendiumEntry(ammo.id(), ammo.name(), "", List.of(Cell.text(found == null ? "–" : found),
        weight(ammo.weight()), Cell.number(ammo.cost()), Cell.number(ammo.rarity()),
        Cell.text(firstSource(ammo.sources()))), details);
  }

  static @NonNull SectionContent apparel(@NonNull Set<Book> books) {
    List<ApparelModData> mods = ApparelCatalog.mods().stream().filter(mod -> mod.isAvailableWith(books)).toList();
    List<CompendiumEntry> entries = ApparelCatalog.available(books).stream().map(item -> apparel(item, mods)).toList();
    return new SectionContent(Section.APPAREL, List.of(Column.text("Covers"), Column.number("Phys."),
        Column.number("Energy"), Column.number("Rad."), Column.number("Weight"), Column.number("Cost"),
        Column.text("Source")), Arrays.stream(ApparelKind.values()).map(ApparelKind::displayName).toList(), entries);
  }

  private static @NonNull CompendiumEntry apparel(@NonNull ApparelData item, @NonNull List<ApparelModData> mods) {
    ApparelModTable table = item.modTable();
    List<DetailLine> details = new Details()
        .value("Kind", item.kind().displayName())
        .value("Covers", CatalogColumns.coverageText(item))
        .value("Damage resistance", "Physical " + item.physical() + ", Energy " + item.energy() + ", Radiation "
            + CatalogColumns.radiationText(item))
        .value("Weight", weightText(item.weight()))
        .value("Cost", Details.amount(item.cost(), " caps"))
        .value("Rarity", item.rarity())
        .value("Carry weight", item.carryWeight() == 0 ? null : ModTexts.bonus(item.carryWeight()) + " lbs.")
        .value("Perk", item.perk())
        .value("Special", item.special())
        .flag("Counts as armor", item.countsAsArmor(), "yes")
        .flag("Super mutant fit", item.superMutantFit(), "yes")
        .value("Mod table", table == null ? null : table.displayName())
        .links("Mods", table == null ? List.of() : mods.stream().filter(mod -> mod.table() == table)
            .map(mod -> new Reference(Section.APPAREL_MODS, mod.id(), mod.name())).toList())
        .source(item.sources());
    return new CompendiumEntry(item.id(), item.name(), item.kind().displayName(), List.of(
        Cell.text(CatalogColumns.coverageText(item)), Cell.number(item.physical()), Cell.number(item.energy()),
        item.radiationImmune() ? Cell.number(Integer.MAX_VALUE, "Immune") : Cell.number(item.radiation()),
        weight(item.weight()), Cell.number(item.cost()), Cell.text(firstSource(item.sources()))), details);
  }

  static @NonNull SectionContent apparelMods(@NonNull Set<Book> books) {
    List<ApparelData> items = ApparelCatalog.available(books);
    List<CompendiumEntry> entries = ApparelCatalog.mods().stream().filter(mod -> mod.isAvailableWith(books))
        .map(mod -> apparelMod(mod, items)).toList();
    return new SectionContent(Section.APPAREL_MODS, List.of(Column.text("Table"), Column.text("Effects"),
        Column.number("Weight"), Column.number("Cost"), Column.text("Source")),
        Arrays.stream(ApparelModSlot.values()).map(ApparelModSlot::displayName).toList(), entries);
  }

  private static @NonNull CompendiumEntry apparelMod(@NonNull ApparelModData mod, @NonNull List<ApparelData> items) {
    ApparelModTable table = mod.table();
    String tableName = table == null ? "Any" : table.displayName();
    List<DetailLine> details = new Details()
        .value("Slot", mod.slot().displayName())
        .value("Table", tableName)
        .value("Placement", mod.placement() == null ? null : mod.placement().displayName())
        .value("Effects", ModTexts.effects(mod))
        .value("Weight", Details.amount(mod.weight(), " lbs."))
        .value("Cost", Details.amount(mod.cost(), " caps"))
        .value("Perks", ModTexts.perks(mod.perks()))
        .links("Fits", table == null ? List.of() : items.stream().filter(item -> item.modTable() == table)
            .map(item -> new Reference(Section.APPAREL, item.id(), item.name())).toList())
        .source(mod.sources());
    return new CompendiumEntry(mod.id(), mod.name(), mod.slot().displayName(), List.of(Cell.text(tableName),
        Cell.text(ModTexts.effects(mod)), Cell.number(mod.weight()), Cell.number(mod.cost()),
        Cell.text(firstSource(mod.sources()))), details);
  }

  static @NonNull SectionContent items(@NonNull Set<Book> books) {
    List<CompendiumEntry> entries = new ArrayList<>(ItemCatalog.available(books).stream()
        .map(EquipmentEntries::item).toList());
    ItemCatalog.availableMagazines(books).stream().map(EquipmentEntries::magazine).forEach(entries::add);
    List<String> categories = new ArrayList<>(Arrays.stream(ItemCategory.values()).map(ItemCategory::displayName)
        .toList());
    categories.add(MAGAZINES);
    return new SectionContent(Section.ITEMS, List.of(Column.text("Category"), Column.number("Weight"),
        Column.number("Cost"), Column.number("Rarity"), Column.text("Source")), categories, entries);
  }

  private static @NonNull CompendiumEntry item(@NonNull ItemData item) {
    String rarePerk = item.rarePerk();
    List<DetailLine> details = new Details()
        .value("Category", item.category().displayName())
        .value("Effects", item.effects())
        .value("HP healed", item.hpHealed())
        .flag("Irradiated", item.irradiated(), item.irradiationDice() == null ? "yes"
            : item.irradiationDice() + " CD radiation damage")
        .value("Duration", item.duration() == null ? null : item.duration().displayName())
        .value("Addictive", item.addictive())
        .value("Perks", item.perks())
        .value("Components", item.components())
        .value("Weight", weightText(item.weight()))
        .value("Cost", Details.amount(item.cost(), " caps"))
        .value("Rarity", item.rarity())
        .links("Teaches", rarePerk == null ? List.of() : PerkCatalog.byId(rarePerk)
            .map(perk -> List.of(new Reference(Section.PERKS, perk.id(), perk.name()))).orElse(List.of()))
        .source(item.sources());
    return new CompendiumEntry(item.id(), item.name(), item.category().displayName(), List.of(
        Cell.text(item.category().displayName()), weight(item.weight()), Cell.number(item.cost()),
        Cell.number(item.rarity()), Cell.text(firstSource(item.sources()))), details);
  }

  private static @NonNull CompendiumEntry magazine(@NonNull MagazineData magazine) {
    Details details = new Details().value("Summary", magazine.summary());
    magazine.issues().forEach(issue -> details.value(issue.roll(), issue.title() + ": " + issue.effect()));
    return new CompendiumEntry("magazine:" + magazine.id(), magazine.name(), MAGAZINES, List.of(
        Cell.text(MAGAZINES), Cell.number(null), Cell.number(null), Cell.number(null),
        Cell.text(firstSource(magazine.sources()))), details.source(magazine.sources()));
  }

  static @NonNull SectionContent legendary(@NonNull Set<Book> books) {
    List<CompendiumEntry> entries = new ArrayList<>();
    LegendaryCatalog.weaponProperties().stream().filter(property -> property.isAvailableWith(books))
        .map(EquipmentEntries::legendaryWeapon).forEach(entries::add);
    LegendaryCatalog.armorProperties().stream().filter(property -> property.isAvailableWith(books))
        .map(EquipmentEntries::legendaryArmor).forEach(entries::add);
    return new SectionContent(Section.LEGENDARY, List.of(Column.text("Kind"), Column.text("d20"),
        Column.text("Source")), List.of(WEAPON_PROPERTIES, ARMOR_PROPERTIES), entries);
  }

  private static @NonNull CompendiumEntry legendaryWeapon(@NonNull LegendaryWeaponData property) {
    String rolls = rolls(property.rolls(), LegendaryWeaponType::displayName);
    List<DetailLine> details = new Details()
        .value("Rule", property.rule())
        .value("Table", "Table " + property.table())
        .value("d20", rolls)
        .source(property.sources());
    return new CompendiumEntry(legendaryWeaponId(property.id()), property.name(), WEAPON_PROPERTIES, List.of(
        Cell.text("Weapon, table " + property.table()), Cell.text(rolls), Cell.text(firstSource(property.sources()))),
        details);
  }

  private static @NonNull CompendiumEntry legendaryArmor(@NonNull LegendaryArmorData property) {
    String rolls = rolls(property.rolls(), LegendaryArmorLocation::displayName);
    List<DetailLine> details = new Details()
        .value("Rule", property.rule())
        .value("d20", rolls)
        .value("Poison DR", property.poison() == 0 ? null : ModTexts.bonus(property.poison()))
        .source(property.sources());
    return new CompendiumEntry(legendaryArmorId(property.id()), property.name(), ARMOR_PROPERTIES, List.of(
        Cell.text("Armor"), Cell.text(rolls), Cell.text(firstSource(property.sources()))), details);
  }

  /** The entry id of a legendary weapon property in the Legendary section. */
  static @NonNull String legendaryWeaponId(@NonNull String propertyId) {
    return WEAPON_PREFIX + propertyId;
  }

  /** The entry id of a legendary armor property in the Legendary section. */
  static @NonNull String legendaryArmorId(@NonNull String propertyId) {
    return ARMOR_PREFIX + propertyId;
  }

  /** The table columns and results, e.g. "Small Guns 3, Big Guns 4". */
  private static <K extends Enum<K>> @NonNull String rolls(@NonNull Map<K, Integer> rolls,
      @NonNull Function<K, String> name) {
    return rolls.entrySet().stream().sorted(Map.Entry.comparingByKey())
        .map(entry -> name.apply(entry.getKey()) + " " + entry.getValue()).collect(Collectors.joining(", "));
  }

  /** The weight as in the books, e.g. "<1" or "4 lbs.". */
  private static @Nullable String weightText(@Nullable String weight) {
    return weight == null ? null : weight + (weight.matches("[\\d.]+") ? " lbs." : "");
  }

  private static @NonNull Cell weight(@Nullable String weight) {
    return weight == null ? Cell.number(null) : Cell.number(Weight.pounds(weight), weight);
  }

  static @NonNull String firstSource(@NonNull List<Source> sources) {
    return sources.isEmpty() ? "–" : sources.getFirst().toString();
  }

  static boolean isAvailable(@NonNull List<Source> sources, @NonNull Set<Book> books) {
    return sources.stream().anyMatch(source -> books.contains(source.book()));
  }
}
