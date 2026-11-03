package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentCatalog;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentPackData;
import de.fallout2d20.charbuilder.catalog.item.ItemCatalog;
import de.fallout2d20.charbuilder.catalog.origin.ExtraTagSkill;
import de.fallout2d20.charbuilder.catalog.origin.OriginCatalog;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkChoice;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.catalog.perk.PerkRequirements;
import de.fallout2d20.charbuilder.catalog.trait.TraitCatalog;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponQuality;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;

/**
 * The character-rules sections of the GM compendium (H2): origins, traits, perks, skills, equipment packs and weapon
 * qualities, built from the catalogs for the enabled books.
 */
final class RuleEntries {

  private static final @NonNull String RARE_PERKS = "Rare perks";
  private static final @NonNull String PERKS = "Perks";

  private RuleEntries() {}

  static @NonNull SectionContent origins(@NonNull Set<Book> books) {
    List<CompendiumEntry> entries = OriginCatalog.available(books).stream().map(origin -> origin(origin, books))
        .toList();
    return new SectionContent(Section.ORIGINS, List.of(Column.text("Trait"), Column.text("Robot"),
        Column.text("Source")), bookTitles(), entries);
  }

  private static @NonNull CompendiumEntry origin(@NonNull OriginData origin, @NonNull Set<Book> books) {
    String trait = origin.traitName();
    ExtraTagSkill extraTag = origin.extraTagSkill();
    Skill fixedTag = origin.fixedTagSkill();
    List<DetailLine> details = new Details()
        .value(trait == null ? "Trait" : trait, origin.traitSummary())
        .value("Attributes", attributes(origin.attributeModifiers(), true))
        .value("Attribute maximums", attributes(origin.attributeMaximums(), false))
        .value("Skill rank cap", origin.skillRankCap())
        .value("Fixed Tag skill", fixedTag == null ? null : fixedTag.displayName())
        .value("Extra Tag skill", extraTag == null ? null
            : extraTag.options().stream().map(Skill::displayName).collect(Collectors.joining(" or ")))
        .value("Extra perks", origin.extraPerks() == 0 ? null : origin.extraPerks())
        .value("Carry weight", Details.amount(origin.fixedCarryWeight(), " lbs. (fixed)"))
        .value("Radiation DR", origin.baseRadiationDr() == 0 ? null : origin.baseRadiationDr())
        .flag("Robot", origin.robot(), "yes")
        .flag("Radiation", origin.radiationImmune(), "immune")
        .flag("Poison", origin.poisonImmune(), "immune")
        .value("Radiation points", origin.maxRadiationPoints() == 0 ? null : origin.maxRadiationPoints())
        .list("Purpose suggestions", origin.purposeSuggestions())
        .list("Starting rolls", origin.startingRolls())
        .links("Traits", origin.traitLists().stream().flatMap(list -> TraitCatalog.inList(list).stream())
            .filter(entry -> EquipmentEntries.isAvailable(entry.sources(), books))
            .map(entry -> new Reference(Section.TRAITS, entry.id(), entry.name())).toList())
        .links("Built-in weapons", origin.innateWeapons().stream().map(id -> WeaponCatalog.byId(id)
            .map(weapon -> new Reference(Section.WEAPONS, weapon.id(), weapon.name()))).flatMap(Optional::stream)
            .toList())
        .links("Equipment packs", EquipmentCatalog.packsFor(origin.id(), books).stream()
            .map(pack -> new Reference(Section.PACKS, pack.id(), pack.name())).toList())
        .source(origin.sources());
    return new CompendiumEntry(origin.id(), origin.name(), bookTitle(origin.sources()), List.of(
        Cell.text(trait == null ? "–" : trait), Cell.text(origin.robot() ? "yes" : ""),
        Cell.text(EquipmentEntries.firstSource(origin.sources()))), details);
  }

  static @NonNull SectionContent traits(@NonNull Set<Book> books) {
    List<TraitDefinition> traits = TraitCatalog.all().stream()
        .filter(trait -> EquipmentEntries.isAvailable(trait.sources(), books)).toList();
    List<OriginData> origins = OriginCatalog.available(books);
    List<CompendiumEntry> entries = traits.stream().map(trait -> trait(trait, origins)).toList();
    return new SectionContent(Section.TRAITS, List.of(Column.text("List"), Column.text("Source")),
        traits.stream().map(trait -> listName(trait.list())).distinct().toList(), entries);
  }

  private static @NonNull CompendiumEntry trait(@NonNull TraitDefinition trait, @NonNull List<OriginData> origins) {
    List<DetailLine> details = new Details()
        .value("Benefit", trait.benefit())
        .value("Penalty", trait.penalty())
        .value("Extra Tag skills", trait.extraTagSkills() == 0 ? null : trait.extraTagSkills())
        .value("Attribute choices", trait.attributeChoices() == 0 ? null : trait.attributeChoices())
        .list("Not as Tag skill", trait.forbiddenTagSkills().stream().map(Skill::displayName).toList())
        .links("Origins", origins.stream().filter(origin -> origin.traitLists().contains(trait.list()))
            .map(origin -> new Reference(Section.ORIGINS, origin.id(), origin.name())).toList())
        .source(trait.sources());
    return new CompendiumEntry(trait.id(), trait.name(), listName(trait.list()), List.of(
        Cell.text(listName(trait.list())), Cell.text(EquipmentEntries.firstSource(trait.sources()))), details);
  }

  static @NonNull SectionContent perks(@NonNull Set<Book> books) {
    List<CompendiumEntry> entries = PerkCatalog.available(books).stream().map(perk -> perk(perk, books)).toList();
    return new SectionContent(Section.PERKS, List.of(Column.number("Ranks"), Column.number("Level"),
        Column.text("Requires"), Column.text("Source")), List.of(PERKS, RARE_PERKS), entries);
  }

  private static @NonNull CompendiumEntry perk(@NonNull PerkDefinition perk, @NonNull Set<Book> books) {
    PerkRequirements requirements = perk.requirements();
    String requires = requirements(requirements);
    int level = requirements.levelForRank(1);
    Integer increase = requirements.levelIncreasePerRank();
    PerkChoice choice = perk.choice();
    List<DetailLine> details = new Details()
        .value("Effect", perk.summary())
        .value("Ranks", perk.ranks())
        .value("Requires", requires)
        .value("Level per rank", increase == null || perk.ranks() == 1 ? null : "+" + increase)
        .value("Choice", choice == null ? null : choiceText(choice))
        .links("Not with", requirements.excludedPerks().stream().map(id -> PerkCatalog.byId(id)
            .map(other -> new Reference(Section.PERKS, other.id(), other.name()))).flatMap(Optional::stream).toList())
        .links("Not for", origins(requirements.excludedOrigins()))
        .links("No level for the first rank", origins(requirements.firstRankLevelWaivedFor()))
        .links("Rare books", ItemCatalog.rareBooksFor(perk.id()).stream().filter(book -> book.isAvailableWith(books))
            .map(book -> new Reference(Section.ITEMS, book.id(), book.name())).toList())
        .source(perk.sources());
    return new CompendiumEntry(perk.id(), perk.name(), requirements.rare() ? RARE_PERKS : PERKS, List.of(
        Cell.number(perk.ranks()), Cell.number(level), Cell.text(requires.isEmpty() ? "–" : requires),
        Cell.text(EquipmentEntries.firstSource(perk.sources()))), details);
  }

  /** The requirements of the first rank in own short words, e.g. "END 7, level 4, no robot". */
  private static @NonNull String requirements(@NonNull PerkRequirements requirements) {
    List<String> parts = new ArrayList<>();
    if (requirements.rare()) {
      parts.add("rare book");
    }
    parts.add(attributes(requirements.attributes(), false));
    int level = requirements.levelForRank(1);
    if (level > 1) {
      parts.add("level " + level);
    }
    if (requirements.notRobot()) {
      parts.add("no robot");
    }
    if (requirements.notRadiationImmune()) {
      parts.add("not immune to radiation");
    }
    return parts.stream().filter(part -> !part.isEmpty()).collect(Collectors.joining(", "));
  }

  static @NonNull SectionContent skills() {
    List<CompendiumEntry> entries = Arrays.stream(Skill.values()).map(RuleEntries::skill).toList();
    return new SectionContent(Section.SKILLS, List.of(Column.text("Attribute"), Column.text("Weapon skill")),
        Arrays.stream(Attribute.values()).map(Attribute::displayName).toList(), entries);
  }

  private static @NonNull CompendiumEntry skill(@NonNull Skill skill) {
    String attribute = skill.governingAttribute().displayName();
    boolean weapon = Skill.weaponSkills().contains(skill);
    List<DetailLine> details = new Details()
        .value("Attribute", attribute)
        .flag("Weapon skill", weapon, "yes")
        .lines();
    return new CompendiumEntry(skill.id(), skill.displayName(), attribute, List.of(Cell.text(attribute),
        Cell.text(weapon ? "yes" : "")), details);
  }

  static @NonNull SectionContent packs(@NonNull Set<Book> books) {
    List<CompendiumEntry> entries = EquipmentCatalog.packs().stream().filter(pack -> pack.isAvailableWith(books))
        .map(RuleEntries::pack).toList();
    return new SectionContent(Section.PACKS, List.of(Column.text("Origins"), Column.number("Caps"),
        Column.text("Source")), bookTitles(), entries);
  }

  private static @NonNull CompendiumEntry pack(@NonNull EquipmentPackData pack) {
    Details details = new Details()
        .value("Items", String.join("; ", pack.items()));
    pack.choices().forEach(choice -> details.value(choice.label(), String.join(" or ", choice.options())));
    details.flag("Trinket", pack.trinket(), "one roll on the trinket table")
        .value("Caps", pack.caps() == 0 ? null : pack.caps())
        .links("Origins", origins(pack.origins()))
        .links("With GM permission", origins(pack.originsWithGmPermission()));
    String origins = pack.origins().stream().map(id -> OriginCatalog.byId(id).map(OriginData::name).orElse(id))
        .collect(Collectors.joining(", "));
    return new CompendiumEntry(pack.id(), pack.name(), bookTitle(pack.sources()), List.of(Cell.text(origins),
        Cell.number(pack.caps()), Cell.text(EquipmentEntries.firstSource(pack.sources()))),
        details.source(pack.sources()));
  }

  static @NonNull SectionContent qualities(@NonNull Set<Book> books) {
    List<CompendiumEntry> entries = Arrays.stream(WeaponQuality.values())
        .filter(quality -> books.contains(quality.source().book())).map(RuleEntries::quality).toList();
    return new SectionContent(Section.QUALITIES, List.of(Column.text("Source")), bookTitles(), entries);
  }

  private static @NonNull CompendiumEntry quality(@NonNull WeaponQuality quality) {
    List<DetailLine> details = new Details()
        .value("Effect", quality.summary())
        .flag("Rated", quality.isRated(), "yes, with a number (e.g. Piercing 1)")
        .links("Opposite", quality.opposite()
            .map(other -> List.of(new Reference(Section.QUALITIES, other.name(), other.displayName())))
            .orElse(List.of()))
        .source(List.of(quality.source()));
    return new CompendiumEntry(quality.name(), quality.displayName(), quality.source().book().title(),
        List.of(Cell.text(quality.source().toString())), details);
  }

  private static @NonNull List<Reference> origins(@NonNull List<String> ids) {
    return ids.stream().map(id -> OriginCatalog.byId(id)
        .map(origin -> new Reference(Section.ORIGINS, origin.id(), origin.name()))).flatMap(Optional::stream).toList();
  }

  /** Attribute values in S.P.E.C.I.A.L. order, e.g. "STR +2, END −1" (signed) or "STR 12". */
  private static @NonNull String attributes(@NonNull Map<Attribute, Integer> values, boolean signed) {
    List<String> parts = new ArrayList<>();
    for (Attribute attribute : Attribute.values()) {
      Integer value = values.get(attribute);
      if (value != null) {
        parts.add(attribute.abbreviation() + " " + (signed ? signedNumber(value) : String.valueOf(value)));
      }
    }
    return String.join(", ", parts);
  }

  /** "+2", "−1" (minus sign) or "0". */
  private static @NonNull String signedNumber(int value) {
    if (value > 0) {
      return "+" + value;
    }
    return value < 0 ? "−" + -value : "0";
  }

  private static @NonNull String choiceText(@NonNull PerkChoice choice) {
    return switch (choice) {
      case ATTRIBUTE -> "one attribute per rank";
      case SKILL -> "a skill that becomes a Tag skill";
    };
  }

  /** A trait list id as a name, e.g. "survivor" → "Survivor", "brotherhood-outcast" → "Brotherhood outcast". */
  static @NonNull String listName(@NonNull String id) {
    String words = id.replace('-', ' ');
    return words.isEmpty() ? words : words.substring(0, 1).toUpperCase(Locale.ROOT) + words.substring(1);
  }

  private static @NonNull String bookTitle(@NonNull List<Source> sources) {
    return sources.isEmpty() ? Book.CRB.title() : sources.getFirst().book().title();
  }

  private static @NonNull List<String> bookTitles() {
    return Arrays.stream(Book.values()).map(Book::title).toList();
  }
}
