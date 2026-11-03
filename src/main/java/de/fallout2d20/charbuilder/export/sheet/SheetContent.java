package de.fallout2d20.charbuilder.export.sheet;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModData;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.catalog.trait.TraitCatalog;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.catalog.weapon.AmmunitionData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.ExperienceTable;
import de.fallout2d20.charbuilder.model.character.HouseRule;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import de.fallout2d20.charbuilder.model.character.apparel.ApparelMods;
import de.fallout2d20.charbuilder.model.character.apparel.ArmorResistances;
import de.fallout2d20.charbuilder.model.character.apparel.LocationResistance;
import de.fallout2d20.charbuilder.model.character.apparel.ModdedApparel;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.derived.CarryLoad;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import de.fallout2d20.charbuilder.model.character.derived.Weight;
import de.fallout2d20.charbuilder.model.character.equipment.ArmSelection;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.Materials;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.InnateWeapons;
import de.fallout2d20.charbuilder.model.character.weapon.ModdedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponAttack;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Everything the printed character sheet shows, as ready texts, so the content can be tested without a
 * PDF. Modelled on the sections of the CRB sheet (CRB pp. 430–431). Values the players track with a
 * pencil during play – current HP, Luck points, ammunition quantity and caps – are deliberately absent
 * (user decision 2026-10-16); derived values are computed from the character at the time of export.
 *
 * @param luckPointsMax maximum Luck points, printed as a small caption next to the empty Luck box
 * @param weapons all weapons, also those that do not fit on page 1 (the renderer moves them on)
 * @param notes free lines for the notes page: the house rules switched on, origin choices, Mister Handy arms,
 *     trinket, the typed notes of the character, its weapons, apparel and items, and the rules of legendary
 *     properties, of the apparel and its mods
 */
public record SheetContent(
    @NonNull String name,
    @NonNull String origin,
    @NonNull String level,
    @NonNull String xpEarned,
    @NonNull String xpNextLevel,
    @NonNull Map<Attribute, Integer> attributes,
    @NonNull List<SkillRow> skills,
    @NonNull String meleeDamage,
    @NonNull String defense,
    @NonNull String initiative,
    @NonNull String luckPointsMax,
    @NonNull String maxHp,
    @NonNull String poisonDr,
    @NonNull List<HitLocationRow> hitLocations,
    @NonNull List<WeaponRow> weapons,
    @NonNull List<String> ammunition,
    @NonNull List<GearRow> gear,
    @NonNull String carryWeightCurrent,
    @NonNull String carryWeightMax,
    @NonNull List<PerkRow> perks,
    @NonNull List<String> notes) {

  public SheetContent {
    attributes = Map.copyOf(attributes);
    skills = List.copyOf(skills);
    hitLocations = List.copyOf(hitLocations);
    weapons = List.copyOf(weapons);
    ammunition = List.copyOf(ammunition);
    gear = List.copyOf(gear);
    perks = List.copyOf(perks);
    notes = List.copyOf(notes);
  }

  /** Collects the sheet texts of a character in its current state (also during creation). */
  public static @NonNull SheetContent of(@NonNull PlayerCharacter character) {
    DerivedStats derived = DerivedStats.of(character);
    Map<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
    for (Attribute attribute : Attribute.values()) {
      attributes.put(attribute, character.abilities().attributeValue(attribute));
    }
    List<SkillRow> skills = new ArrayList<>();
    for (Skill skill : Skill.values()) {
      skills.add(new SkillRow(skill.displayName(), skill.governingAttribute().abbreviation(),
          character.abilities().isTagged(skill), character.abilities().skillRank(skill)));
    }
    List<HitLocationRow> locations = new ArrayList<>();
    for (LocationResistance resistance : ArmorResistances.of(character)) {
      locations.add(new HitLocationRow(resistance.location().displayName(), resistance.location().dieRange(),
          resistance.physical().toString(), resistance.energy().toString(), resistance.radiation().toString()));
    }
    CarryLoad load = CarryLoad.of(character);
    return new SheetContent(
        character.getName(),
        originName(character),
        Integer.toString(character.advancement().getLevel()),
        Long.toString(character.advancement().getXp()),
        Long.toString(ExperienceTable.xpForLevel(character.advancement().getLevel() + 1)),
        attributes,
        skills,
        derived.meleeDamageText(),
        Integer.toString(derived.defense()),
        Integer.toString(load.initiative(derived.initiative())),
        Integer.toString(derived.maxLuckPoints()),
        Integer.toString(derived.maxHp()),
        derived.poison().toString(),
        locations,
        weapons(character),
        ammunition(character),
        gear(character),
        Integer.toString(load.carried()),
        Integer.toString(load.carryWeight()),
        perks(character),
        notes(character));
  }

  private static @NonNull String originName(@NonNull PlayerCharacter character) {
    CatalogRef origin = character.getOrigin();
    if (origin == null) {
      return "";
    }
    return character.getOriginData().map(OriginData::name).orElseGet(origin::displayName);
  }

  /** The weapons of the origin's trait (built in, WG p. 11), then the carried weapons. */
  private static @NonNull List<WeaponRow> weapons(@NonNull PlayerCharacter character) {
    List<WeaponRow> rows = new ArrayList<>();
    for (WeaponData innate : InnateWeapons.of(character)) {
      rows.add(row(character, WeaponAttack.of(character, innate), 1, "–", innate.special()));
    }
    for (CarriedWeapon weapon : character.gear().getWeapons()) {
      String weight = weapon.isIntegrated() ? "–"
          : ModdedWeapon.of(weapon).map(ModdedWeapon::weight).orElse("");
      rows.add(row(character, WeaponAttack.of(character, weapon), weapon.quantity(), weight,
          weapon.data().map(WeaponData::special).orElse(null)));
    }
    return rows;
  }

  /** One weapon row; a rule of the weapon no column covers ({@link WeaponData#special()}) follows its effects. */
  private static @NonNull WeaponRow row(@NonNull PlayerCharacter character, @NonNull WeaponAttack attack,
      int quantity, @NonNull String weight, @Nullable String special) {
    Skill skill = attack.skill();
    String effects = special == null ? attack.effects()
        : attack.effects().isEmpty() ? special : attack.effects() + "; " + special;
    return new WeaponRow(
        withQuantity(attack.moddedName(), quantity),
        skill == null ? "" : skill.displayName(),
        attack.targetNumber() == null ? "" : attack.targetNumber().toString(),
        skill != null && character.abilities().isTagged(skill),
        attack.damageText(),
        effects,
        attack.damageType(),
        attack.fireRate() == null ? "–" : attack.fireRate().toString(),
        attack.range() == null ? "–" : attack.range().code(),
        attack.qualities(),
        String.join(", ", attack.ammunition().stream().map(SheetContent::ammunitionName).toList()),
        weight);
  }

  /** Ammunition types of the carried and innate weapons, then any other type the character has shots of. */
  private static @NonNull List<String> ammunition(@NonNull PlayerCharacter character) {
    Set<String> ids = InnateWeapons.ammunitionUsed(character);
    character.gear().getAmmunition().forEach((id, shots) -> {
      if (shots > 0) {
        ids.add(id);
      }
    });
    return ids.stream().map(SheetContent::ammunitionName).toList();
  }

  private static @NonNull String ammunitionName(@NonNull String id) {
    return WeaponCatalog.ammunitionById(id).map(AmmunitionData::name).orElse(id);
  }

  /** Items with quantity and stack weight, apparel (carried pieces marked), junk and materials. */
  private static @NonNull List<GearRow> gear(@NonNull PlayerCharacter character) {
    List<GearRow> rows = new ArrayList<>();
    for (CarriedItem item : character.gear().getItems()) {
      String name = item.issue() == null ? item.displayName() : item.displayName() + " (" + item.issue() + ")";
      rows.add(new GearRow(withQuantity(name, item.quantity()), pounds(item.printedWeight(), item.quantity())));
    }
    for (WornApparel apparel : character.gear().getApparel()) {
      List<String> mods = ApparelMods.installedMods(apparel).stream().map(ApparelModData::name).toList();
      String name = mods.isEmpty() ? apparel.legendaryName()
          : apparel.legendaryName() + " (" + String.join(", ", mods) + ")";
      rows.add(new GearRow(apparel.carried() ? name + " (carried)" : name,
          pounds(ModdedApparel.of(apparel).map(ModdedApparel::weight).orElse(null), 1)));
    }
    Materials materials = character.gear().getMaterials();
    addMaterial(rows, "Junk", materials.junk(), Materials.JUNK_WEIGHT);
    addMaterial(rows, "Common materials", materials.common(), Materials.MATERIAL_WEIGHT);
    addMaterial(rows, "Uncommon materials", materials.uncommon(), Materials.MATERIAL_WEIGHT);
    addMaterial(rows, "Rare materials", materials.rare(), Materials.MATERIAL_WEIGHT);
    return rows;
  }

  private static void addMaterial(@NonNull List<GearRow> rows, @NonNull String name, int amount, int weight) {
    if (amount > 0) {
      rows.add(new GearRow(withQuantity(name, amount), Integer.toString(amount * weight)));
    }
  }

  /** Weight of a stack: whole pounds multiplied, a printed "<1" (or no weight) kept as printed. */
  private static @NonNull String pounds(@Nullable String printed, int quantity) {
    if (printed == null) {
      return "";
    }
    int pounds = Weight.pounds(printed);
    return pounds == 0 ? printed : Integer.toString(pounds * quantity);
  }

  private static @NonNull String withQuantity(@NonNull String name, int quantity) {
    return quantity > 1 ? name + " ×" + quantity : name;
  }

  /**
   * Origin trait first (origins without a named trait, such as Survivor, only lead to chosen traits),
   * then the chosen traits, then the perks with rank and choices.
   */
  private static @NonNull List<PerkRow> perks(@NonNull PlayerCharacter character) {
    List<PerkRow> rows = new ArrayList<>();
    character.getOriginData().ifPresent(origin -> {
      if (origin.traitName() != null) {
        rows.add(new PerkRow(origin.traitName(), "", origin.traitSummary()));
      }
    });
    for (TraitSelection selection : character.getTraits()) {
      Optional<TraitDefinition> trait = TraitCatalog.byId(selection.trait().id());
      rows.add(trait.map(definition -> new PerkRow(definition.name(), "",
              definition.benefit() + " Penalty: " + definition.penalty()))
          .orElseGet(() -> new PerkRow(selection.trait().displayName(), "", "")));
    }
    for (PerkSelection selection : character.getPerks()) {
      String choices = selection.choiceText();
      Optional<PerkDefinition> perk = PerkCatalog.byId(selection.perk().id());
      String name = perk.map(PerkDefinition::name).orElseGet(() -> selection.perk().displayName());
      rows.add(new PerkRow(choices.isEmpty() ? name : name + " (" + choices + ")",
          Integer.toString(selection.rank()), perk.map(PerkDefinition::summary).orElse("")));
    }
    return rows;
  }

  private static @NonNull List<String> notes(@NonNull PlayerCharacter character) {
    List<String> notes = new ArrayList<>();
    if (!character.getHouseRules().isEmpty()) {
      notes.add("House rules: " + character.getHouseRules().stream().map(HouseRule::displayName)
          .collect(Collectors.joining(", ")) + ".");
    }
    String purpose = character.getOriginChoices().purpose();
    if (purpose != null && !purpose.isBlank()) {
      notes.add("Purpose: " + purpose.strip());
    }
    List<ArmSelection> arms = character.startingGear().getSelection().arms();
    for (int i = 0; i < arms.size(); i++) {
      ArmSelection arm = arms.get(i);
      String fitted = arm.attachment() != null ? arm.attachment().displayName() : arm.weapon();
      notes.add("Arm " + (i + 1) + ": " + (fitted == null ? "open" : fitted));
    }
    String trinket = character.startingGear().getSelection().trinket();
    if (trinket != null) {
      notes.add("Trinket: " + trinket);
    }
    for (CarriedWeapon weapon : character.gear().getWeapons()) {
      List<String> rules = weapon.legendaryProperty()
          .map(property -> List.of(sentence(property.name() + ": " + property.rule()))).orElse(List.of());
      addNote(notes, weapon.legendaryName(), withRules(weapon.notes(), rules));
    }
    for (WornApparel apparel : character.gear().getApparel()) {
      addNote(notes, apparel.legendaryName(), withRules(apparel.notes(), apparelRules(apparel)));
    }
    for (CarriedItem item : character.gear().getItems()) {
      addNote(notes, item.displayName(), item.notes());
    }
    String inventory = character.gear().getNotes();
    if (!inventory.isBlank()) {
      notes.add(inventory.strip());
    }
    return notes;
  }

  /** The typed note, then the rules, separated by a dash when both are present. */
  private static @NonNull String withRules(@NonNull String note, @NonNull List<String> rules) {
    String stripped = note.strip();
    String joined = String.join(" ", rules);
    return stripped.isEmpty() || joined.isEmpty() ? stripped + joined : stripped + " – " + joined;
  }

  /**
   * The rules of a piece no sheet column covers, each as a sentence: its legendary property ("Property: rule", WG pp.
   * 104–108; its DR is in the resistance columns), its own special (WG pp. 86–93), then its mods' ("Mod: rule").
   */
  private static @NonNull List<String> apparelRules(@NonNull WornApparel apparel) {
    List<String> rules = new ArrayList<>();
    apparel.legendaryProperty().ifPresent(property -> rules.add(property.name() + ": " + property.rule()));
    apparel.data().map(ApparelData::special).ifPresent(rules::add);
    ModdedApparel.of(apparel).ifPresent(modded -> rules.addAll(modded.specials()));
    return rules.stream().map(SheetContent::sentence).toList();
  }

  private static @NonNull String sentence(@NonNull String rule) {
    return rule.endsWith(".") ? rule : rule + ".";
  }

  private static void addNote(@NonNull List<String> notes, @NonNull String name, @NonNull String note) {
    if (!note.isBlank()) {
      notes.add(name + ": " + note.strip());
    }
  }
}
