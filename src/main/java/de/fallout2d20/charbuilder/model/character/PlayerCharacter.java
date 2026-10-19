package de.fallout2d20.charbuilder.model.character;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.origin.OriginCatalog;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipmentItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToIntFunction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A player character (aggregate root): identity, origin, traits, perks, books and modes, plus its parts
 * {@link #abilities()} (S.P.E.C.I.A.L., skills), {@link #advancement()} (level, XP, level-up),
 * {@link #gear()} (what it carries), {@link #startingGear()} (starting equipment) and {@link #session()}
 * (play-session values). Edits that touch several parts (origin, traits, perks) live here.
 *
 * <p>Origin, origin choices, traits, attribute allocation, free Tags, bought skill ranks and the starting
 * equipment can only be changed while creation is not complete (or in GM mode); gear can always be edited.
 * After creation the character advances (CRB p. 49).
 */
public class PlayerCharacter {

  /** The only origin of the Core Rulebook with its own hit locations (CRB p. 146). */
  private static final @NonNull String MISTER_HANDY = "mister-handy";

  private @NonNull String name;
  private @Nullable CatalogRef origin;
  private @NonNull OriginChoices originChoices;
  private boolean creationComplete;
  private final @NonNull Set<Book> enabledBooks;
  private final @NonNull List<TraitSelection> traits;
  private final @NonNull List<PerkSelection> perks;
  private final @Nullable LegacyBaseline legacyBaseline;
  private final @Nullable LegacyImport legacyImport;
  /** Tag skills at completion (CRB p. 81 Tag skill items); null while in creation or when not stored. */
  private @Nullable Set<Skill> completedTagSkills;
  /** App version that created the character with File → New; null for imports and older files. */
  private final @Nullable String createdWith;
  /** GM free edit (D2): creation edits allowed after completion, no budgets; not saved (user decision). */
  private boolean gmMode;
  private final @NonNull Abilities abilities;
  private final @NonNull Advancement advancement;
  private final @NonNull Gear gear;
  private final @NonNull StartingGear startingGear;
  private final @NonNull Session session;

  /** Takes the parts checked by {@link PlayerCharacterBuilder#build()}. */
  PlayerCharacter(@NonNull PlayerCharacterBuilder parts) {
    this.name = parts.name;
    this.origin = parts.origin;
    this.originChoices = parts.originChoices;
    this.creationComplete = parts.creationComplete;
    this.enabledBooks = EnumSet.of(Book.CRB);
    this.enabledBooks.addAll(parts.enabledBooks);
    this.traits = new ArrayList<>(parts.traits);
    this.perks = new ArrayList<>(parts.perks);
    this.legacyBaseline = parts.legacyBaseline;
    this.legacyImport = parts.legacyImport;
    this.completedTagSkills = parts.startingTagSkills == null ? null : Set.copyOf(parts.startingTagSkills);
    this.createdWith = parts.createdWith;
    this.abilities = new Abilities(this, parts.attributeAllocation, parts.skills);
    this.advancement = new Advancement(this, parts.level, parts.xp, parts.progression, parts.startingLevel);
    this.gear = new Gear(this, parts);
    this.startingGear = new StartingGear(this, parts.startingEquipment.fittedToCatalog(parts.creationComplete),
        parts.inventory.packItemsAdded());
    this.session = new Session(this, parts.vitals);
  }

  /** Builder for a character from stored parts (loading, legacy import, tests); defaults: a new, empty character. */
  public static @NonNull PlayerCharacterBuilder builder() {
    return new PlayerCharacterBuilder();
  }

  /**
   * A new, empty character at the start of character creation, marked as created with {@code appVersion}
   * (kept in the save file for good).
   */
  public static @NonNull PlayerCharacter createNew(@NonNull String appVersion) {
    return builder().createdWith(appVersion).build();
  }

  /** A new, empty character at the start of character creation without an app version (tests). */
  public static @NonNull PlayerCharacter createNew() {
    return builder().build();
  }

  /** S.P.E.C.I.A.L. and skills. */
  public @NonNull Abilities abilities() {
    return abilities;
  }

  /** Level, XP and level-up. */
  public @NonNull Advancement advancement() {
    return advancement;
  }

  /** Weapons, ammunition, apparel, items, caps and gear notes. */
  public @NonNull Gear gear() {
    return gear;
  }

  /** The starting equipment selection and its entries in the gear. */
  public @NonNull StartingGear startingGear() {
    return startingGear;
  }

  /** Current HP, rads, Luck points, injuries and session notes. */
  public @NonNull Session session() {
    return session;
  }

  /**
   * Sum of a catalog effect field times the rank over the character's catalogued perks (Strong Back,
   * Toughness, Skilled, ...). Imported ranks count: unlike Intense Training, whose effect is already
   * part of the legacy baseline attributes, these effects are never stored in a legacy file.
   */
  public int perkEffect(@NonNull ToIntFunction<PerkDefinition> effectPerRank) {
    int total = 0;
    for (PerkSelection perk : perks) {
      Optional<PerkDefinition> definition = PerkCatalog.byId(perk.perk().id());
      if (definition.isPresent()) {
        total += effectPerRank.applyAsInt(definition.get()) * perk.rank();
      }
    }
    return total;
  }

  public boolean isLegacyImport() {
    return legacyBaseline != null;
  }

  public @NonNull String getName() {
    return name;
  }

  public void setName(@NonNull String name) {
    this.name = name;
  }

  public @Nullable CatalogRef getOrigin() {
    return origin;
  }

  /** The catalog entry of the origin; empty without origin or for an origin outside the catalog. */
  public @NonNull Optional<OriginData> getOriginData() {
    return origin == null ? Optional.empty() : OriginCatalog.byId(origin.id());
  }

  public @NonNull OriginChoices getOriginChoices() {
    return originChoices;
  }

  /**
   * Chooses the origin during creation. Origin choices and traits are reset, because they depend on
   * the origin (user decision); attribute allocation and free Tag skills are kept, then adjusted to
   * the new origin (see {@link Abilities#adjustAttributesToOrigin()} and
   * {@link Abilities#adjustSkillsToRules()}). The starting equipment stays if its pack is offered to the new
   * origin too (e.g. Ghoul and Survivor share the Wastelander packs), otherwise it is cleared.
   */
  public void chooseOrigin(@NonNull CatalogRef newOrigin) {
    requireCreation();
    startingGear.syncing(() -> {
      origin = newOrigin;
      originChoices = OriginChoices.NONE;
      traits.clear();
      startingGear.keepOnlyIfOfferedTo(newOrigin);
      abilities.adjustAttributesToOrigin();
      abilities.adjustSkillsToRules();
    });
  }

  /**
   * The additional Tag skill granted by the origin; checked by the creation rules, not here. The
   * skills are adjusted afterwards (see {@link Abilities#adjustSkillsToRules()}).
   */
  public void chooseOriginTagSkill(@Nullable Skill skill) {
    requireCreation();
    startingGear.syncing(() -> {
      originChoices = originChoices.withTagSkill(skill);
      abilities.adjustSkillsToRules();
    });
  }

  /** Purpose of a Protectron (WOA p. 21); blank means not chosen. */
  public void setPurpose(@Nullable String purpose) {
    requireCreation();
    originChoices = originChoices.withPurpose(purpose == null || purpose.isBlank() ? null : purpose.strip());
  }

  /**
   * Takes a trait during creation; a trait already chosen is not added twice. A free Tag the trait
   * forbids is removed (see {@link Abilities#adjustSkillsToRules()}).
   */
  public void chooseTrait(@NonNull CatalogRef trait) {
    requireCreation();
    startingGear.syncing(() -> {
      if (traits.stream().noneMatch(selection -> selection.trait().id().equals(trait.id()))) {
        traits.add(new TraitSelection(trait, false, List.of()));
      }
      abilities.adjustSkillsToRules();
    });
  }

  /** Removes a trait (and its attribute choices) during creation. */
  public void removeTrait(@NonNull String traitId) {
    requireCreation();
    startingGear.syncing(() -> {
      traits.removeIf(selection -> selection.trait().id().equals(traitId));
    });
  }

  /** Sets the attribute choices of a chosen trait (Gifted); checked by the creation rules, not here. */
  public void setTraitAttributeChoices(@NonNull String traitId, @NonNull List<Attribute> choices) {
    requireCreation();
    traits.replaceAll(selection -> selection.trait().id().equals(traitId)
        ? new TraitSelection(selection.trait(), selection.imported(), choices) : selection);
  }

  /**
   * Takes one more rank of a perk, with the attribute or skill chosen for it (Intense Training, Tag!).
   * Requirements and budget are checked by the creation or advancement rules, not here. During creation
   * a Tag! skill loses a free Tag and keeps its bought ranks within the cap (see
   * {@link Abilities#adjustSkillsToRules()}); after creation the rank is staged (CRB p. 49: one perk per new
   * level, see {@link Advancement#apply()}).
   */
  public void addPerkRank(@NonNull CatalogRef perk, @Nullable Attribute attribute, @Nullable Skill skill) {
    startingGear.syncing(() -> {
      int staged = creationComplete && !gmMode ? 1 : 0;
      int index = perkIndex(perk.id());
      PerkSelection current = index < 0 ? null : perks.get(index);
      List<Attribute> attributes = new ArrayList<>(current == null ? List.of() : current.attributeChoices());
      List<Skill> chosenSkills = new ArrayList<>(current == null ? List.of() : current.skillChoices());
      if (attribute != null) {
        attributes.add(attribute);
      }
      if (skill != null) {
        chosenSkills.add(skill);
      }
      if (current == null) {
        perks.add(new PerkSelection(perk, 1, 0, attributes, chosenSkills, staged));
      } else {
        perks.set(index, new PerkSelection(current.perk(), current.rank() + 1, current.importedRanks(),
            attributes, chosenSkills, current.stagedRanks() + staged));
      }
      if (!creationComplete) {
        abilities.adjustSkillsToRules();
      }
    });
  }

  /**
   * Gives back the last rank of a perk, with its choice: during creation any rank but the imported
   * ones, after creation only a staged rank; in GM mode any rank, imported ones too (D2). The perk is
   * removed when no rank is left.
   */
  public void removePerkRank(@NonNull String perkId) {
    startingGear.syncing(() -> {
      int index = perkIndex(perkId);
      if (index < 0) {
        return;
      }
      PerkSelection perk = perks.get(index);
      boolean removable = gmMode || (creationComplete ? perk.stagedRanks() > 0 : perk.rank() > perk.importedRanks());
      if (!removable) {
        return;
      }
      if (perk.rank() == 1) {
        perks.remove(index);
        return;
      }
      perks.set(index, new PerkSelection(perk.perk(), perk.rank() - 1, Math.min(perk.importedRanks(), perk.rank() - 1),
          dropLast(perk.attributeChoices(), perk), dropLast(perk.skillChoices(), perk),
          Math.max(0, perk.stagedRanks() - 1)));
    });
  }

  /**
   * The choices without the one of the last rank, if that rank has one: a staged rank always has its
   * choice as the last entry (imported ranks before it may lack theirs); otherwise the last rank has one
   * if every rank has.
   */
  private static <T> @NonNull List<T> dropLast(@NonNull List<T> choices, @NonNull PerkSelection perk) {
    boolean lastHasChoice = perk.stagedRanks() > 0 ? !choices.isEmpty() : choices.size() >= perk.rank();
    return lastHasChoice ? choices.subList(0, choices.size() - 1) : choices;
  }

  private int perkIndex(@NonNull String perkId) {
    for (int i = 0; i < perks.size(); i++) {
      if (perks.get(i).perk().id().equals(perkId)) {
        return i;
      }
    }
    return -1;
  }

  /** Makes the staged perk ranks permanent ({@link Advancement#apply()}). */
  void applyStagedPerkRanks() {
    perks.replaceAll(perk -> new PerkSelection(perk.perk(), perk.rank(), perk.importedRanks(),
        perk.attributeChoices(), perk.skillChoices()));
  }

  /**
   * The hit locations of this character: those of a Mister Handy (CRB p. 146) for that origin, else the
   * six humanoid ones (CRB p. 28), also for a Protectron (WOA defines none; house rule, robot armor is mapped
   * onto them, see {@code Coverage.locationsOn}).
   */
  public @NonNull List<HitLocation> hitLocations() {
    return isMisterHandy() ? HitLocation.misterHandy() : HitLocation.humanoid();
  }

  /** Whether the origin is the Mister Handy, the one robot with its own body shape (CRB p. 55, 146). */
  public boolean isMisterHandy() {
    return origin != null && origin.id().equals(MISTER_HANDY);
  }

  /**
   * Ends character creation (CRB p. 50: creation is done after its six steps, from origin to starting
   * equipment); one-way by user decision, corrections are a GM matter (D2). The creation rules are not
   * checked here – the caller confirms open hints with the player. Afterwards creation edits are refused,
   * the creation rank cap of 3 no longer applies (CRB p. 58; see {@link Abilities#skillRankCap()}), and the
   * starting level and the Tag skills are kept for the starting equipment (CRB p. 81). Caps still at 0 are
   * set to the fixed starting caps (pack, starting level; user decision); dice caps such as the Barter d20
   * are rolled and added by the player.
   *
   * @throws IllegalStateException if creation is already complete
   */
  public void completeCreation() {
    requireStrictCreation();
    if (gear.getCaps() == 0) {
      gear.setCaps(StartingEquipmentItems.caps(this).fixedTotal());
    }
    completedTagSkills = Set.copyOf(currentTagSkills());
    creationComplete = true;
    advancement.markStartingLevel();
  }

  /**
   * The Tag skills that grant starting equipment (CRB p. 81: the Tags chosen in creation): during
   * creation the current Tag skills, after completion those of that moment, so a Tag! taken later
   * grants no items. For a complete character without stored Tags, the current ones.
   */
  public @NonNull Set<Skill> getStartingTagSkills() {
    if (!creationComplete || completedTagSkills == null) {
      return currentTagSkills();
    }
    return completedTagSkills;
  }

  private @NonNull Set<Skill> currentTagSkills() {
    Set<Skill> tagged = EnumSet.noneOf(Skill.class);
    for (Skill skill : Skill.values()) {
      if (abilities.isTagged(skill)) {
        tagged.add(skill);
      }
    }
    return Collections.unmodifiableSet(tagged);
  }

  /** Whether GM free edit is on (D2): creation edits work after completion and budgets do not apply. */
  public boolean isGmMode() {
    return gmMode;
  }

  /** Switches GM free edit on or off; never saved (user decision 2026-10-16). */
  public void setGmMode(boolean on) {
    gmMode = on;
  }

  /** For creation edits: during creation or in GM mode. */
  void requireCreation() {
    if (creationComplete && !gmMode) {
      throw new IllegalStateException("This can only be changed during character creation");
    }
  }

  /** For what only belongs to creation itself (ending it, the starting level); not even the GM redoes it. */
  void requireStrictCreation() {
    if (creationComplete) {
      throw new IllegalStateException("Character creation is already complete");
    }
  }

  void requireGmMode() {
    if (!gmMode) {
      throw new IllegalStateException("Only the GM can change this (GM mode)");
    }
  }

  void requireComplete() {
    if (!creationComplete) {
      throw new IllegalStateException("This can only be changed after character creation");
    }
  }

  public boolean isCreationComplete() {
    return creationComplete;
  }

  /**
   * Books whose content this character may use. The Core Rulebook is always included, because the
   * basic rules come from it (user decision); only the supplements can be switched off.
   */
  public @NonNull Set<Book> getEnabledBooks() {
    return Collections.unmodifiableSet(enabledBooks);
  }

  public @NonNull List<TraitSelection> getTraits() {
    return List.copyOf(traits);
  }

  public @NonNull List<PerkSelection> getPerks() {
    return List.copyOf(perks);
  }

  /** The traits without a copy, for the parts. */
  @NonNull List<TraitSelection> traitList() {
    return Collections.unmodifiableList(traits);
  }

  /** The perks without a copy, for the parts. */
  @NonNull List<PerkSelection> perkList() {
    return Collections.unmodifiableList(perks);
  }

  public @Nullable LegacyBaseline getLegacyBaseline() {
    return legacyBaseline;
  }

  public @Nullable LegacyImport getLegacyImport() {
    return legacyImport;
  }

  /** App version that created the character, if known. */
  public @NonNull Optional<String> getCreatedWith() {
    return Optional.ofNullable(createdWith);
  }
}
