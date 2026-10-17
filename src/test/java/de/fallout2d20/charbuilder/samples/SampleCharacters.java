package de.fallout2d20.charbuilder.samples;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.equipment.ArmSlot;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentCatalog;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentPackData;
import de.fallout2d20.charbuilder.catalog.equipment.PackChoice;
import de.fallout2d20.charbuilder.catalog.equipment.TagSkillItemData;
import de.fallout2d20.charbuilder.catalog.item.ItemCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.ExperienceTable;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.model.character.creation.CreationRules;
import de.fallout2d20.charbuilder.model.character.creation.PerkRules;
import de.fallout2d20.charbuilder.model.character.creation.SkillRules;
import de.fallout2d20.charbuilder.model.character.equipment.ArmSelection;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipmentItems;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Test characters for the Beta menu (E9) and for manual UI and PDF checks. Each one is built through the
 * public model API and the creation rules, the way a player would build it in the app, so the samples
 * follow the rules and are always written in the current save format (see {@link GenerateSamples}).
 * Names are made up.
 */
final class SampleCharacters {

  /**
   * Written as {@code createdWith}/{@code savedWith} instead of the app version, so a new release does not
   * change the sample files; only real content changes show up in their diffs.
   */
  static final String VERSION = "samples";

  private SampleCharacters() {}

  /** The generated samples by file name, in menu order. {@code nora-mayfield.json} is hand-made and kept. */
  static Map<String, Supplier<Character>> all() {
    Map<String, Supplier<Character>> samples = new LinkedHashMap<>();
    samples.put("brotherhood-initiate.json", SampleCharacters::brotherhoodInitiate);
    samples.put("mister-handy.json", SampleCharacters::misterHandy);
    samples.put("super-mutant.json", SampleCharacters::superMutant);
    samples.put("ghoul.json", SampleCharacters::ghoul);
    samples.put("gen-3-synth.json", SampleCharacters::gen3Synth);
    samples.put("protectron.json", SampleCharacters::protectron);
    samples.put("child-of-atom.json", SampleCharacters::childOfAtom);
    samples.put("tribal-gifted.json", SampleCharacters::tribalGifted);
    samples.put("in-creation.json", SampleCharacters::inCreation);
    samples.put("level-up-pending.json", SampleCharacters::levelUpPending);
    samples.put("veteran-overflow.json", SampleCharacters::veteranOverflow);
    return samples;
  }

  static Character brotherhoodInitiate() {
    Character character = start("Daria Voss", "brotherhood-initiate");
    character.chooseOriginTagSkill(Skill.ENERGY_WEAPONS);
    special(character, 0, 1, 1, 2, 0, 1, 0);
    tags(character, Skill.SCIENCE, Skill.REPAIR, Skill.MEDICINE);
    finish(character, List.of(Skill.ENERGY_WEAPONS, Skill.SCIENCE, Skill.REPAIR, Skill.SNEAK),
        List.of("black-widow-lady-killer"));
    return complete(character);
  }

  static Character misterHandy() {
    Character character = start("Mister Pennywhistle", "mister-handy");
    special(character, 0, 2, 1, 2, 0, 0, 0);
    tags(character, Skill.REPAIR, Skill.MEDICINE, Skill.SCIENCE);
    finish(character, List.of(Skill.REPAIR, Skill.MEDICINE, Skill.SCIENCE, Skill.ENERGY_WEAPONS),
        List.of("jury-rigging"));
    return complete(character);
  }

  static Character superMutant() {
    Character character = start("Grak", "super-mutant");
    special(character, 1, 1, 1, -1, -1, 2, 2);
    tags(character, Skill.MELEE_WEAPONS, Skill.BIG_GUNS, Skill.ATHLETICS);
    finish(character, List.of(Skill.MELEE_WEAPONS, Skill.BIG_GUNS, Skill.ATHLETICS, Skill.SURVIVAL),
        List.of("toughness"));
    return complete(character);
  }

  static Character ghoul() {
    Character character = start("Eli Marrow", "ghoul");
    special(character, 0, 2, 1, 0, 1, 1, 0);
    tags(character, Skill.SMALL_GUNS, Skill.SNEAK, Skill.LOCKPICK);
    finish(character, List.of(Skill.SMALL_GUNS, Skill.SNEAK, Skill.LOCKPICK, Skill.SURVIVAL),
        List.of("moving-target"));
    return complete(character);
  }

  static Character gen3Synth() {
    Character character = start("Ada Sterling", "generation-3-synth");
    character.chooseOriginTagSkill(Skill.SPEECH);
    special(character, 0, 1, 0, 1, 1, 1, 1);
    tags(character, Skill.SMALL_GUNS, Skill.SCIENCE, Skill.SNEAK);
    finish(character, List.of(Skill.SMALL_GUNS, Skill.SPEECH, Skill.SCIENCE, Skill.SNEAK), List.of());
    return complete(character);
  }

  static Character protectron() {
    Character character = start("Unit PT-7", "protectron");
    character.setPurpose(character.getOriginData().orElseThrow().purposeSuggestions().getFirst());
    special(character, 1, 1, 1, 1, 0, 0, 1);
    tags(character, Skill.ENERGY_WEAPONS, Skill.UNARMED, Skill.REPAIR);
    finish(character, List.of(Skill.ENERGY_WEAPONS, Skill.UNARMED, Skill.REPAIR, Skill.ATHLETICS), List.of());
    return complete(character);
  }

  static Character childOfAtom() {
    Character character = start("Brother Kael", "child-of-atom");
    special(character, 1, 1, 1, 0, 1, 0, 1);
    tags(character, Skill.MELEE_WEAPONS, Skill.SURVIVAL, Skill.SPEECH);
    finish(character, List.of(Skill.MELEE_WEAPONS, Skill.SURVIVAL, Skill.SPEECH, Skill.MEDICINE),
        List.of("hunter", "can-do"));
    return complete(character);
  }

  static Character tribalGifted() {
    Character character = start("Runs-With-Storms", "tribal");
    character.chooseTrait(CatalogRef.of("gifted"));
    character.setTraitAttributeChoices("gifted", List.of(Attribute.AGILITY, Attribute.PERCEPTION));
    character.chooseTrait(CatalogRef.of("heavy-handed"));
    special(character, 1, 0, 1, 1, 0, 1, 1);
    tags(character, Skill.MELEE_WEAPONS, Skill.SURVIVAL, Skill.THROWING);
    finish(character, List.of(Skill.MELEE_WEAPONS, Skill.SURVIVAL, Skill.THROWING, Skill.SNEAK), List.of());
    return complete(character);
  }

  /** A Survivor halfway through creation: origin, one trait, part of S.P.E.C.I.A.L. and Tags; open hints. */
  static Character inCreation() {
    Character character = start("Half Done", "survivor");
    character.chooseTrait(CatalogRef.of("educated"));
    special(character, 1, 1, 0, 1, 0, 0, 0);
    tags(character, Skill.SMALL_GUNS, Skill.LOCKPICK);
    return character;
  }

  /** Complete at level 3, then XP for level 5: two skill ranks and two perks wait to be spent. */
  static Character levelUpPending() {
    Character character = start("Mara Quill", "vault-dweller");
    character.chooseOriginTagSkill(Skill.SCIENCE);
    character.setStartingLevel(3);
    special(character, 0, 1, 1, 1, 1, 1, 0);
    tags(character, Skill.SMALL_GUNS, Skill.MEDICINE, Skill.SPEECH);
    finish(character, List.of(Skill.SMALL_GUNS, Skill.MEDICINE, Skill.SPEECH, Skill.SCIENCE), List.of());
    complete(character);
    character.addXp(xpUntil(character, 5));
    return character;
  }

  /** A level 12 Survivor with many perks and a full inventory, for long tables and PDF continuation pages. */
  static Character veteranOverflow() {
    Character character = start("Old Jeb Harrow", "survivor");
    character.chooseTrait(CatalogRef.of("educated"));
    character.chooseTrait(CatalogRef.of("fast-shot"));
    character.setStartingLevel(12);
    special(character, 1, 1, 1, 0, 1, 1, 0);
    tags(character, Skill.SMALL_GUNS, Skill.LOCKPICK, Skill.SNEAK, Skill.REPAIR);
    finish(character, List.of(Skill.SMALL_GUNS, Skill.LOCKPICK, Skill.SNEAK, Skill.REPAIR, Skill.SCIENCE,
        Skill.MEDICINE), List.of());
    complete(character);
    WeaponCatalog.offered("survivor", character.getEnabledBooks()).stream().limit(15)
        .forEach(weapon -> character.addWeapon(CarriedWeapon.of(CatalogRef.of(weapon.id()))));
    int worn = character.getApparel().size();
    ApparelCatalog.available(character.getEnabledBooks()).stream().limit(10)
        .forEach(apparel -> character.addApparel(WornApparel.of(CatalogRef.of(apparel.id()))));
    // Only carried, so the extra apparel causes no clash hints.
    for (int i = worn; i < character.getApparel().size(); i++) {
      character.setApparelCarried(i, true);
    }
    ItemCatalog.all().stream().limit(40)
        .forEach(item -> character.addItem(CarriedItem.of(CatalogRef.of(item.id()))));
    return character;
  }

  private static Character start(String name, String originId) {
    Character character = Character.createNew(VERSION);
    character.setName(name);
    character.chooseOrigin(CatalogRef.of(originId));
    return character;
  }

  /** S.P.E.C.I.A.L. allocations in attribute order (STR … LCK); together they spend the five points. */
  private static void special(Character character, int... allocations) {
    Attribute[] attributes = Attribute.values();
    for (int i = 0; i < attributes.length; i++) {
      character.setAttributeAllocation(attributes[i], allocations[i]);
    }
  }

  private static void tags(Character character, Skill... skills) {
    for (Skill skill : skills) {
      character.setFreeTag(skill, true);
    }
  }

  /**
   * Spends the rest of the creation budgets: the wished perks (they must be allowed), then other perks without a
   * choice in a fixed random order; the skill points round-robin over the wished skills, then over all; the first pack the origin
   * may take (if any) with its first options, arms and Tag skill items, and a trinket.
   */
  private static void finish(Character character, List<Skill> skills, List<String> perkIds) {
    for (String id : perkIds) {
      PerkDefinition perk = PerkCatalog.byId(id).orElseThrow();
      if (!PerkRules.canAdd(character, perk)) {
        throw new IllegalStateException("Sample " + character.getName() + " cannot take " + perk.name() + ": "
            + PerkRules.blockers(character, perk));
      }
      character.addPerkRank(CatalogRef.of(perk.id()), null, null);
    }
    // Shuffled with the name as seed: varied, but the same perks on every run.
    List<PerkDefinition> perks = new ArrayList<>(PerkCatalog.available(character.getEnabledBooks()));
    Collections.shuffle(perks, new Random(character.getName().hashCode()));
    for (PerkDefinition perk : perks) {
      while (PerkRules.perksLeft(character) > 0 && perk.choice() == null && PerkRules.canAdd(character, perk)) {
        character.addPerkRank(CatalogRef.of(perk.id()), null, null);
      }
    }
    spendSkillPoints(character, skills);
    spendSkillPoints(character, List.of(Skill.values()));
    choosePack(character);
  }

  private static void spendSkillPoints(Character character, List<Skill> skills) {
    boolean spent = true;
    while (spent && SkillRules.skillPointsLeft(character) > 0) {
      spent = false;
      for (Skill skill : skills) {
        if (SkillRules.skillPointsLeft(character) > 0 && SkillRules.canRaise(character, skill)) {
          SkillRules.step(character, skill, 1);
          spent = true;
        }
      }
    }
  }

  private static void choosePack(Character character) {
    String originId = character.getOriginData().orElseThrow().id();
    Optional<EquipmentPackData> offered = EquipmentCatalog.packsFor(originId, character.getEnabledBooks())
        .stream().filter(candidate -> !candidate.needsGmPermission(originId)).findFirst();
    if (offered.isEmpty()) {
      return; // e.g. Tribal: the WG packs are not in the catalog yet (F4)
    }
    EquipmentPackData pack = offered.get();
    character.chooseEquipmentPack(CatalogRef.of(pack.id()));
    List<PackChoice> choices = pack.choices();
    for (int i = 0; i < choices.size(); i++) {
      character.chooseEquipmentOption(i, choices.get(i).options().getFirst());
    }
    List<ArmSlot> arms = pack.arms();
    for (int slot = 0; slot < arms.size(); slot++) {
      character.chooseArm(slot, ArmSelection.of(arms.get(slot).options().getFirst()));
    }
    StartingEquipmentItems.taggedSkillItems(character).filter(TagSkillItemData::isChoice)
        .forEach(items -> character.chooseTagSkillItem(items.skill(), items.options().getFirst()));
    if (pack.trinket()) {
      character.setTrinket("A dented harmonica");
    }
  }

  /**
   * Ends creation after checking that the sample follows the rules, then adds the pack items to the
   * inventory, as a player would with the button in the Gear tab.
   *
   * @throws IllegalStateException with the open hints if the sample does not follow the rules
   */
  private static Character complete(Character character) {
    List<CreationProblem> problems = CreationRules.problems(character);
    if (!problems.isEmpty()) {
      throw new IllegalStateException("Sample " + character.getName() + " has open hints: "
          + problems.stream().map(CreationProblem::message).collect(Collectors.joining("; ")));
    }
    character.completeCreation();
    if (character.canAddPackItems()) {
      character.addPackItemsToInventory();
    }
    return character;
  }

  /** XP still needed to reach the level. */
  private static long xpUntil(Character character, int level) {
    return ExperienceTable.xpForLevel(level) - character.getXp();
  }
}
