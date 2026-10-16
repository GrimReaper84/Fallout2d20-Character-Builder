package de.fallout2d20.charbuilder.model.character;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentCatalog;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentPackData;
import de.fallout2d20.charbuilder.catalog.equipment.PackChoice;
import de.fallout2d20.charbuilder.catalog.equipment.TagSkillItemData;
import de.fallout2d20.charbuilder.catalog.origin.OriginCatalog;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkChoice;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.catalog.trait.TraitCatalog;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.model.character.equipment.ArmSelection;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.ToIntFunction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A player character (aggregate root).
 *
 * <p>Attributes and skills are stored as their parts (base, allocation, ranks), not as effective
 * values, so every value can be traced back. A character imported from a legacy save carries a
 * {@link LegacyBaseline} that replaces base and creation allocation.
 *
 * <p>Origin and trait effects (attribute modifiers, origin Tag skills, Gifted) are applied only to
 * characters without a legacy baseline: an imported baseline already contains them. Origin, origin
 * choices, traits, attribute allocation, free Tags, bought skill ranks and the starting equipment can
 * only be changed while creation is not complete; the inventory notes, weapons and ammunition can always
 * be edited.
 *
 * <p>After creation the character advances (CRB p. 49): XP and levels grant skill ranks and perks,
 * which are first staged (they count everywhere but can be taken back) and then applied.
 */
public class Character {

  /** CRB p. 58: every S.P.E.C.I.A.L. attribute starts at 5 before the origin is applied. */
  public static final int ATTRIBUTE_BASE = 5;

  /** CRB p. 58: a Tag skill starts at rank 2. */
  public static final int TAG_SKILL_BONUS = 2;

  /** CRB p. 58: no skill can ever go above rank 6. */
  private static final int MAX_SKILL_RANK = 6;

  /** CRB p. 58: during creation no skill above rank 3, unless the character starts at level 3+. */
  public static final int CREATION_SKILL_RANK_CAP = 3;

  /** CRB p. 58: from this starting level on, the creation cap of rank 3 no longer applies. */
  private static final int FULL_SKILL_RANKS_FROM_LEVEL = 3;

  private String name;
  private @Nullable CatalogRef origin;
  private @NonNull OriginChoices originChoices;
  private int level;
  private long xp;
  private boolean creationComplete;
  private final Set<Book> enabledBooks;
  private final Map<Attribute, Integer> attributeAllocation;
  private final Map<Skill, SkillState> skills;
  private final List<TraitSelection> traits;
  private final List<PerkSelection> perks;
  private @NonNull Progression progression;
  private @NonNull Vitals vitals;
  private final @Nullable LegacyBaseline legacyBaseline;
  private final @Nullable LegacyImport legacyImport;
  private @NonNull StartingEquipment startingEquipment;
  private @NonNull String inventoryNotes;
  /** Level at completion; null while in creation (then the current level) or when unknown (imports). */
  private @Nullable Integer completedStartingLevel;
  /** Tag skills at completion (CRB p. 81 Tag skill items); null while in creation or when not stored. */
  private @Nullable Set<Skill> completedTagSkills;
  private final List<CarriedWeapon> weapons;
  /** Shots per ammunition id (CRB p. 91: counted per type, shared by all weapons using it); only counts above 0. */
  private final Map<String, Integer> ammunition;

  /**
   * Creates a character from stored parts without starting equipment and inventory notes – a test
   * convenience only; the application uses the full constructor. Missing attributes count as allocation
   * 0, missing skills as untrained.
   */
  public Character(
      @NonNull String name,
      @Nullable CatalogRef origin,
      @NonNull OriginChoices originChoices,
      int level,
      long xp,
      boolean creationComplete,
      @NonNull Set<Book> enabledBooks,
      @NonNull Map<Attribute, Integer> attributeAllocation,
      @NonNull Map<Skill, SkillState> skills,
      @NonNull List<TraitSelection> traits,
      @NonNull List<PerkSelection> perks,
      @NonNull Progression progression,
      @NonNull Vitals vitals,
      @Nullable LegacyBaseline legacyBaseline,
      @Nullable LegacyImport legacyImport) {
    this(name, origin, originChoices, level, xp, creationComplete, enabledBooks, attributeAllocation,
        skills, traits, perks, progression, vitals, legacyBaseline, legacyImport, StartingEquipment.NONE, "",
        null, null, List.of(), Map.of());
  }

  /**
   * Creates a character from stored parts. Missing attributes count as allocation 0, missing
   * skills as untrained; the starting equipment is fitted to the catalog (see
   * {@link StartingEquipment#fittedToCatalog(boolean)}; a complete character keeps stale picks).
   *
   * @param startingLevel the level a complete character started at, {@code null} if unknown (imported
   *     characters); always {@code null} for a character in creation (see {@link #getStartingLevel()})
   * @param startingTagSkills the Tag skills of a complete character at the end of creation, {@code null}
   *     if not stored (then its current Tag skills count); always {@code null} for a character in creation
   *     (see {@link #getStartingTagSkills()})
   * @param weapons the carried weapons
   * @param ammunition shots per ammunition id; counts of 0 are dropped
   */
  public Character(
      @NonNull String name,
      @Nullable CatalogRef origin,
      @NonNull OriginChoices originChoices,
      int level,
      long xp,
      boolean creationComplete,
      @NonNull Set<Book> enabledBooks,
      @NonNull Map<Attribute, Integer> attributeAllocation,
      @NonNull Map<Skill, SkillState> skills,
      @NonNull List<TraitSelection> traits,
      @NonNull List<PerkSelection> perks,
      @NonNull Progression progression,
      @NonNull Vitals vitals,
      @Nullable LegacyBaseline legacyBaseline,
      @Nullable LegacyImport legacyImport,
      @NonNull StartingEquipment startingEquipment,
      @NonNull String inventoryNotes,
      @Nullable Integer startingLevel,
      @Nullable Set<Skill> startingTagSkills,
      @NonNull List<CarriedWeapon> weapons,
      @NonNull Map<String, Integer> ammunition) {
    if (level < 1 || xp < 0) {
      throw new IllegalArgumentException("Level must be at least 1 and XP not negative");
    }
    if ((startingLevel != null || startingTagSkills != null) && !creationComplete) {
      throw new IllegalArgumentException("Only a complete character has a stored starting level or Tag skills");
    }
    if (startingLevel != null && (startingLevel < 1 || startingLevel > level)) {
      throw new IllegalArgumentException("Starting level " + startingLevel + " must lie between 1 and the level "
          + level);
    }
    this.name = name;
    this.origin = origin;
    this.originChoices = originChoices;
    this.level = level;
    this.xp = xp;
    this.creationComplete = creationComplete;
    this.enabledBooks = EnumSet.of(Book.CRB);
    this.enabledBooks.addAll(enabledBooks);
    this.attributeAllocation = new EnumMap<>(Attribute.class);
    for (Attribute attribute : Attribute.values()) {
      this.attributeAllocation.put(attribute, attributeAllocation.getOrDefault(attribute, 0));
    }
    this.skills = new EnumMap<>(Skill.class);
    for (Skill skill : Skill.values()) {
      this.skills.put(skill, skills.getOrDefault(skill, SkillState.UNTRAINED));
    }
    this.traits = new ArrayList<>(traits);
    this.perks = new ArrayList<>(perks);
    this.progression = progression;
    this.vitals = vitals;
    this.legacyBaseline = legacyBaseline;
    this.legacyImport = legacyImport;
    this.startingEquipment = startingEquipment.fittedToCatalog(creationComplete);
    this.inventoryNotes = inventoryNotes;
    this.completedStartingLevel = startingLevel;
    this.completedTagSkills = startingTagSkills == null ? null : Set.copyOf(startingTagSkills);
    this.weapons = new ArrayList<>(weapons);
    this.ammunition = new LinkedHashMap<>();
    ammunition.forEach((id, shots) -> {
      if (id.isBlank() || shots < 0) {
        throw new IllegalArgumentException("Ammunition needs an id and a non-negative number of shots");
      }
      if (shots > 0) {
        this.ammunition.put(id, shots);
      }
    });
  }

  /** A new, empty character at the start of character creation. */
  public static Character createNew() {
    return new Character("", null, OriginChoices.NONE, 1, 0, false, EnumSet.allOf(Book.class),
        Map.of(), Map.of(), List.of(), List.of(), Progression.NONE, Vitals.FULL, null, null);
  }

  /**
   * Effective attribute value: the base (5 plus the origin modifier, e.g. Super Mutant STR +2,
   * CRB p. 53; or the legacy baseline value for an imported character) plus the creation allocation.
   *
   * <p>Gifted's chosen attributes add +1 each (CRB p. 56), and so does every rank of a perk with an
   * attribute choice (Intense Training, CRB p. 66). Imported perk ranks are skipped: their effect is
   * already part of the legacy baseline.
   */
  public int attributeValue(@NonNull Attribute attribute) {
    if (legacyBaseline != null) {
      return legacyBaseline.attribute(attribute) + attributeAllocation(attribute) + perkBonus(attribute);
    }
    int modifier = getOriginData().map(data -> data.attributeModifier(attribute)).orElse(0);
    return ATTRIBUTE_BASE + modifier + traitBonus(attribute) + attributeAllocation(attribute)
        + perkBonus(attribute);
  }

  /**
   * +1 per new (not imported) rank of an attribute-choice perk that chose the attribute. The new ranks
   * are the last ones, and their choices the last ones of the list (imported ranks may lack choices).
   */
  private int perkBonus(@NonNull Attribute attribute) {
    int bonus = 0;
    for (PerkSelection perk : perks) {
      if (choiceOf(perk) == PerkChoice.ATTRIBUTE) {
        List<Attribute> choices = perk.attributeChoices();
        int newRanks = perk.rank() - perk.importedRanks();
        bonus += (int) choices.subList(Math.max(0, choices.size() - newRanks), choices.size())
            .stream().filter(choice -> choice == attribute).count();
      }
    }
    return bonus;
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

  private static @Nullable PerkChoice choiceOf(@NonNull PerkSelection perk) {
    return PerkCatalog.byId(perk.perk().id()).map(PerkDefinition::choice).orElse(null);
  }

  /**
   * +1 per choice of the attribute by a trait with attribute choices (Gifted, CRB p. 56). Imported
   * traits are skipped: their effect is part of the legacy baseline.
   */
  private int traitBonus(@NonNull Attribute attribute) {
    int bonus = 0;
    for (TraitSelection trait : traits) {
      if (trait.choosesAttributes()) {
        bonus += (int) trait.attributeChoices().stream().filter(choice -> choice == attribute).count();
      }
    }
    return bonus;
  }

  /**
   * Whether the skill is a Tag skill: one of the freely chosen Tag skills, one granted by the origin –
   * its fixed Tag skill (Ghoul: Survival, in addition to the three free ones, user reading of CRB
   * p. 52) or the chosen extra Tag skill (e.g. Vault Kid, CRB p. 57) – or the skill chosen with Tag!
   * (CRB p. 73).
   */
  public boolean isTagged(@NonNull Skill skill) {
    return skillState(skill).tagged() || hasFixedTag(skill);
  }

  /**
   * Effective skill rank: Tag bonus plus creation ranks (or the legacy baseline rank for an
   * imported character, which already includes the Tag bonus of its imported Tags) plus applied and
   * staged advancement ranks. Rank caps are validated by creation and advancement, not here – except
   * for a skill tagged only by Tag!: its bonus raises the rank at most to {@link #skillRankCap()}
   * (CRB p. 73: +2 ranks, to a maximum of 6), also for an imported character that takes Tag! later.
   */
  public int skillRank(@NonNull Skill skill) {
    SkillState state = skillState(skill);
    boolean perkTagOnly = isTaggedByPerk(skill) && !state.tagged() && !isTaggedByOrigin(skill);
    int start = legacyBaseline != null
        ? legacyBaseline.skillRank(skill)
        : (isTagged(skill) && !perkTagOnly ? TAG_SKILL_BONUS : 0) + state.creationRanks();
    int rank = start + state.advancementRanks() + state.stagedRanks();
    return perkTagOnly ? Math.max(rank, Math.min(rank + TAG_SKILL_BONUS, skillRankCap())) : rank;
  }

  /**
   * Highest rank any skill of this character may have: during creation 3 below level 3, otherwise 6
   * (CRB p. 58), lowered to the origin's skill cap (Super Mutant CRB p. 53, Nightkin WG p. 14).
   */
  public int skillRankCap() {
    int cap = !creationComplete && level < FULL_SKILL_RANKS_FROM_LEVEL
        ? CREATION_SKILL_RANK_CAP : MAX_SKILL_RANK;
    Optional<OriginData> data = getOriginData();
    if (data.isEmpty()) {
      return cap;
    }
    Integer originCap = data.get().skillRankCap();
    return originCap == null ? cap : Math.min(cap, originCap);
  }

  /** Whether the origin tags the skill: its fixed Tag skill or the chosen extra Tag skill. */
  public boolean isTaggedByOrigin(@NonNull Skill skill) {
    return legacyBaseline == null && (skill == originChoices.tagSkill()
        || getOriginData().map(data -> data.fixedTagSkill() == skill).orElse(false));
  }

  /** The chosen trait that forbids the skill as a Tag skill (Nomad: Science, WG p. 15), if any. */
  public @NonNull Optional<TraitDefinition> tagForbiddenBy(@NonNull Skill skill) {
    return traits.stream()
        .map(selection -> TraitCatalog.byId(selection.trait().id()))
        .flatMap(Optional::stream)
        .filter(trait -> trait.forbiddenTagSkills().contains(skill))
        .findFirst();
  }

  /** Whether a perk with a skill choice made the skill a Tag skill (Tag!, CRB p. 73). */
  public boolean isTaggedByPerk(@NonNull Skill skill) {
    return perks.stream().anyMatch(perk -> choiceOf(perk) == PerkChoice.SKILL
        && perk.skillChoices().contains(skill));
  }

  /** Whether the skill is tagged by its origin or a perk, i.e. not freely (user decision: separate sources). */
  private boolean hasFixedTag(@NonNull Skill skill) {
    return isTaggedByOrigin(skill) || isTaggedByPerk(skill);
  }

  public boolean isLegacyImport() {
    return legacyBaseline != null;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
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
   * the new origin (see {@link #adjustAttributesToOrigin()} and {@link #adjustSkillsToRules()}). The
   * starting equipment stays if its pack is offered to the new origin too (e.g. Ghoul and Survivor
   * share the Wastelander packs), otherwise it is cleared.
   */
  public void chooseOrigin(@NonNull CatalogRef newOrigin) {
    requireCreation();
    origin = newOrigin;
    originChoices = OriginChoices.NONE;
    traits.clear();
    if (getEquipmentPack().filter(pack -> pack.isOfferedTo(newOrigin.id())).isEmpty()) {
      startingEquipment = StartingEquipment.NONE;
    }
    adjustAttributesToOrigin();
    adjustSkillsToRules();
  }

  /**
   * Lowers the allocation of every attribute above the new origin's maximum (e.g. Super Mutant INT 6,
   * CRB p. 53) down to that maximum; the points return to the S.P.E.C.I.A.L. budget (user decision).
   */
  private void adjustAttributesToOrigin() {
    for (Attribute attribute : Attribute.values()) {
      int maximum = getOriginData().map(data -> data.attributeMaximum(attribute))
          .orElse(OriginData.DEFAULT_ATTRIBUTE_MAXIMUM);
      int excess = attributeValue(attribute) - maximum;
      if (excess > 0) {
        attributeAllocation.put(attribute, attributeAllocation(attribute) - excess);
      }
    }
  }

  /**
   * The additional Tag skill granted by the origin; checked by the creation rules, not here. The
   * skills are adjusted afterwards (see {@link #adjustSkillsToRules()}).
   */
  public void chooseOriginTagSkill(@Nullable Skill skill) {
    requireCreation();
    originChoices = originChoices.withTagSkill(skill);
    adjustSkillsToRules();
  }

  /**
   * Keeps free Tags and bought ranks consistent after origin, traits or perks changed (user decision):
   * a free Tag is removed from a skill the origin or a perk tags (it returns to the free Tag budget;
   * e.g. Ghoul Survival, Tag!) or a trait forbids (Nomad: Science), and bought ranks are lowered as far
   * as needed to stay within {@link #skillRankCap()}, as when tagging a skill.
   */
  private void adjustSkillsToRules() {
    for (Skill skill : Skill.values()) {
      SkillState state = skillState(skill);
      boolean tagged = state.tagged() && !hasFixedTag(skill) && tagForbiddenBy(skill).isEmpty();
      skills.put(skill, new SkillState(tagged, ranksWithinCap(skill, tagged), state.advancementRanks(),
          state.stagedRanks()));
    }
  }

  /** The bought ranks of the skill, lowered so its rank stays within the cap (never below 0). */
  private int ranksWithinCap(@NonNull Skill skill, boolean freeTag) {
    SkillState state = skillState(skill);
    int tagBonus = freeTag || hasFixedTag(skill) ? TAG_SKILL_BONUS : 0;
    int room = skillRankCap() - tagBonus - state.advancementRanks() - state.stagedRanks();
    return Math.max(0, Math.min(state.creationRanks(), room));
  }

  /**
   * Sets the creation allocation of an attribute: points spent on it, or −1 for an attribute lowered
   * below its starting value (CRB p. 58). The range and the budget are checked by the creation
   * rules, not here.
   */
  public void setAttributeAllocation(@NonNull Attribute attribute, int allocation) {
    requireCreation();
    attributeAllocation.put(attribute, allocation);
  }

  /**
   * Chooses or removes a free Tag skill (CRB p. 58). Tagging adds the two free ranks and lowers the
   * bought ranks as far as needed to stay within {@link #skillRankCap()}; untagging only removes the
   * two free ranks and keeps the bought ones (user decisions). Removing is always possible. The
   * budget is checked by the creation rules.
   *
   * @throws IllegalArgumentException when tagging a skill the origin or a perk already tags, or a trait
   *     forbids
   */
  public void setFreeTag(@NonNull Skill skill, boolean tagged) {
    requireCreation();
    if (tagged && (hasFixedTag(skill) || tagForbiddenBy(skill).isPresent())) {
      throw new IllegalArgumentException(skill.displayName() + " cannot get a free Tag");
    }
    SkillState state = skillState(skill);
    int creationRanks = tagged ? ranksWithinCap(skill, true) : state.creationRanks();
    skills.put(skill, new SkillState(tagged, creationRanks, state.advancementRanks(), state.stagedRanks()));
  }

  /** Sets the ranks bought with skill points during creation; budget and cap are checked by the rules. */
  public void setCreationRanks(@NonNull Skill skill, int creationRanks) {
    requireCreation();
    SkillState state = skillState(skill);
    skills.put(skill, new SkillState(state.tagged(), creationRanks, state.advancementRanks(), state.stagedRanks()));
  }

  /**
   * Sets the starting level during creation. CRB p. 49: characters start at level 1 unless the group
   * agrees on a higher one; the book sets no upper limit (user decision). XP becomes the minimum of
   * that level (user decision; CRB p. 49 table and formula). Skill ranks above the rank cap of the new
   * level are trimmed (user decision, see {@link #adjustSkillsToRules()}); perks and skill points that
   * no longer fit stay and are reported by the creation rules.
   *
   * @throws IllegalArgumentException if the level is below 1, or so high (above roughly 400 million)
   *     that its XP no longer fits into a number – a technical limit, not a rule
   */
  public void setStartingLevel(int startingLevel) {
    requireCreation();
    if (startingLevel < 1) {
      throw new IllegalArgumentException("Level must be at least 1");
    }
    try {
      xp = ExperienceTable.xpForLevel(startingLevel);
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("Level " + startingLevel + " is too high to compute its XP", e);
    }
    level = startingLevel;
    adjustSkillsToRules();
  }

  /** Purpose of a Protectron (WOA p. 21); blank means not chosen. */
  public void setPurpose(@Nullable String purpose) {
    requireCreation();
    originChoices = originChoices.withPurpose(purpose == null || purpose.isBlank() ? null : purpose.strip());
  }

  /**
   * Takes a trait during creation; a trait already chosen is not added twice. A free Tag the trait
   * forbids is removed (see {@link #adjustSkillsToRules()}).
   */
  public void chooseTrait(@NonNull CatalogRef trait) {
    requireCreation();
    if (traits.stream().noneMatch(selection -> selection.trait().id().equals(trait.id()))) {
      traits.add(new TraitSelection(trait, false, List.of()));
    }
    adjustSkillsToRules();
  }

  /** Removes a trait (and its attribute choices) during creation. */
  public void removeTrait(@NonNull String traitId) {
    requireCreation();
    traits.removeIf(selection -> selection.trait().id().equals(traitId));
  }

  /**
   * Takes one more rank of a perk, with the attribute or skill chosen for it (Intense Training, Tag!).
   * Requirements and budget are checked by the creation or advancement rules, not here. During creation
   * a Tag! skill loses a free Tag and keeps its bought ranks within the cap (see
   * {@link #adjustSkillsToRules()}); after creation the rank is staged (CRB p. 49: one perk per new
   * level, see {@link #applyAdvancement()}).
   */
  public void addPerkRank(@NonNull CatalogRef perk, @Nullable Attribute attribute, @Nullable Skill skill) {
    int staged = creationComplete ? 1 : 0;
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
      adjustSkillsToRules();
    }
  }

  /**
   * Gives back the last rank of a perk, with its choice: during creation any rank but the imported
   * ones, after creation only a staged rank (applied ranks are a GM matter, D2). The perk is removed
   * when no rank is left.
   */
  public void removePerkRank(@NonNull String perkId) {
    int index = perkIndex(perkId);
    if (index < 0) {
      return;
    }
    PerkSelection perk = perks.get(index);
    boolean removable = creationComplete ? perk.stagedRanks() > 0 : perk.rank() > perk.importedRanks();
    if (!removable) {
      return;
    }
    if (perk.rank() == 1) {
      perks.remove(index);
      return;
    }
    perks.set(index, new PerkSelection(perk.perk(), perk.rank() - 1, perk.importedRanks(),
        dropLast(perk.attributeChoices(), perk), dropLast(perk.skillChoices(), perk),
        Math.max(0, perk.stagedRanks() - 1)));
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

  /** Sets the attribute choices of a chosen trait (Gifted); checked by the creation rules, not here. */
  public void setTraitAttributeChoices(@NonNull String traitId, @NonNull List<Attribute> choices) {
    requireCreation();
    traits.replaceAll(selection -> selection.trait().id().equals(traitId)
        ? new TraitSelection(selection.trait(), selection.imported(), choices) : selection);
  }

  /**
   * Chooses the starting equipment pack during creation (CRB p. 50, step 6); whether the origin may take
   * it is checked by the creation rules. All pack choices start open; Tag skill item picks and the
   * trinket stay (a trinket only counts for a pack that has one). A Mister Handy pack sets the arms the
   * pack fixes and leaves the choosable ones open (CRB p. 77); free arms are switched off.
   *
   * @throws IllegalArgumentException if the pack is not in the catalog
   */
  public void chooseEquipmentPack(@NonNull CatalogRef pack) {
    requireCreation();
    EquipmentPackData data = EquipmentCatalog.packById(pack.id())
        .orElseThrow(() -> new IllegalArgumentException("Unknown equipment pack: " + pack.id()));
    List<@Nullable String> choices = new ArrayList<>(Collections.nCopies(data.choices().size(), null));
    startingEquipment = new StartingEquipment(pack, choices, startingEquipment.tagSkillItems(),
        startingEquipment.trinket(), false, StartingEquipment.packArms(data, List.of()));
  }

  /**
   * Picks an option of one of the pack's choices during creation; {@code null} opens it again.
   *
   * @throws IllegalArgumentException if the pack has no such choice or the choice no such option
   * @throws IllegalStateException if no equipment pack is chosen
   */
  public void chooseEquipmentOption(int index, @Nullable String option) {
    requireCreation();
    List<PackChoice> choices = requireEquipmentPack().choices();
    if (index < 0 || index >= choices.size()
        || (option != null && !choices.get(index).options().contains(option))) {
      throw new IllegalArgumentException("No pack choice " + index + " with option " + option);
    }
    startingEquipment = startingEquipment.withChoice(index, option);
  }

  /**
   * Picks which of the offered items a Tag skill grants (CRB p. 81) during creation; {@code null}
   * removes the pick.
   *
   * @throws IllegalArgumentException if the skill's items offer no choice or not this option
   */
  public void chooseTagSkillItem(@NonNull Skill skill, @Nullable String option) {
    requireCreation();
    TagSkillItemData items = EquipmentCatalog.tagSkillItems(skill);
    if (!items.isChoice() || (option != null && !items.options().contains(option))) {
      throw new IllegalArgumentException("No Tag skill item choice " + option + " for " + skill);
    }
    startingEquipment = startingEquipment.withTagSkillItem(skill, option);
  }

  /**
   * The personal trinket (CRB p. 80) during creation; blank means none described yet. It is kept across
   * pack changes and only counts for a pack that includes a trinket.
   */
  public void setTrinket(@Nullable String trinket) {
    requireCreation();
    startingEquipment = startingEquipment.withTrinket(trinket);
  }

  /**
   * Switches the Mister Handy's freely configurable arms (house-rule option beyond the book) on or off
   * during creation. Switching on keeps the current arms as a starting point; switching off restores the
   * arms the pack fixes and keeps choosable arms only if the pack allows them.
   *
   * @throws IllegalStateException if no equipment pack is chosen or it has no arms
   */
  public void setFreeArms(boolean free) {
    requireCreation();
    EquipmentPackData pack = requireEquipmentPack();
    if (pack.arms().isEmpty()) {
      throw new IllegalStateException("The equipment pack has no arms: " + pack.id());
    }
    List<ArmSelection> arms = free ? startingEquipment.arms() : StartingEquipment.packArms(pack, startingEquipment.arms());
    startingEquipment = startingEquipment.withArms(free, arms);
  }

  /**
   * Sets one arm (slot 0 to 2) during creation. Whether the pack allows it is checked by the creation
   * rules, not here.
   *
   * @throws IllegalStateException if the starting equipment has no arms
   * @throws IllegalArgumentException if the slot is not 0 to 2
   */
  public void chooseArm(int slot, @NonNull ArmSelection arm) {
    requireCreation();
    if (startingEquipment.arms().isEmpty()) {
      throw new IllegalStateException("The starting equipment has no arms");
    }
    if (slot < 0 || slot >= startingEquipment.arms().size()) {
      throw new IllegalArgumentException("No arm slot " + slot);
    }
    startingEquipment = startingEquipment.withArm(slot, arm);
  }

  /** Free-text inventory notes; editable in every mode (until phase 7 brings a real inventory). */
  public void setInventoryNotes(@Nullable String notes) {
    inventoryNotes = notes == null ? "" : notes;
  }

  /** Adds a carried weapon (any mode: weapons are bought and found at any time). */
  public void addWeapon(@NonNull CarriedWeapon weapon) {
    weapons.add(weapon);
  }

  /**
   * Removes the carried weapon at the index.
   *
   * @throws IndexOutOfBoundsException if there is no weapon at the index
   */
  public void removeWeapon(int index) {
    weapons.remove(index);
  }

  /** Free note of the weapon at the index, e.g. installed mods. */
  public void setWeaponNotes(int index, @Nullable String notes) {
    weapons.set(index, weapons.get(index).withNotes(notes));
  }

  /**
   * How many of the weapon at the index are carried (grenades, mines, thrown weapons).
   *
   * @throws IllegalArgumentException if the quantity is below 1
   */
  public void setWeaponQuantity(int index, int quantity) {
    weapons.set(index, weapons.get(index).withQuantity(quantity));
  }

  /**
   * Chooses the attack skill of the custom weapon at the index (e.g. one imported without skill).
   *
   * @throws IllegalStateException if the weapon at the index is a catalog weapon
   */
  public void setCustomWeaponSkill(int index, @Nullable Skill skill) {
    weapons.set(index, weapons.get(index).withCustomSkill(skill));
  }

  public @NonNull List<CarriedWeapon> getWeapons() {
    return List.copyOf(weapons);
  }

  /**
   * Sets the shots of an ammunition type (CRB p. 91: shots are counted per type, all weapons using the
   * type share them; no magazines, user decision against the legacy app). 0 removes the type.
   *
   * @throws IllegalArgumentException if the id is blank or the shots are negative
   */
  public void setAmmunition(@NonNull String ammunitionId, int shots) {
    if (ammunitionId.isBlank() || shots < 0) {
      throw new IllegalArgumentException("Ammunition needs an id and a non-negative number of shots");
    }
    if (shots == 0) {
      ammunition.remove(ammunitionId);
    } else {
      ammunition.put(ammunitionId, shots);
    }
  }

  /** Shots of the ammunition type; 0 if none. */
  public int ammunition(@NonNull String ammunitionId) {
    return ammunition.getOrDefault(ammunitionId, 0);
  }

  /** Shots per ammunition id, only types with shots. */
  public @NonNull Map<String, Integer> getAmmunition() {
    return Collections.unmodifiableMap(ammunition);
  }

  /** The catalog entry of the chosen equipment pack; empty without pack or for an unknown one. */
  public @NonNull Optional<EquipmentPackData> getEquipmentPack() {
    CatalogRef pack = startingEquipment.pack();
    return pack == null ? Optional.empty() : EquipmentCatalog.packById(pack.id());
  }

  private @NonNull EquipmentPackData requireEquipmentPack() {
    return getEquipmentPack().orElseThrow(() -> new IllegalStateException("No equipment pack chosen"));
  }

  /**
   * Ends character creation (CRB p. 50: creation is done after its six steps, from origin to starting
   * equipment); one-way by user decision, corrections are a GM matter (D2). The creation rules are not
   * checked here – the caller confirms open hints with the player. Afterwards creation edits are refused,
   * the creation rank cap of 3 no longer applies (CRB p. 58; see {@link #skillRankCap()}), and the
   * starting level and the Tag skills are kept for the starting equipment (CRB p. 81).
   *
   * @throws IllegalStateException if creation is already complete
   */
  public void completeCreation() {
    requireCreation();
    completedTagSkills = Set.copyOf(currentTagSkills());
    creationComplete = true;
    completedStartingLevel = level;
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
      if (isTagged(skill)) {
        tagged.add(skill);
      }
    }
    return Collections.unmodifiableSet(tagged);
  }

  /**
   * Adds XP awarded by the GM (CRB p. 49) and gains every level whose threshold the new total crosses
   * (see {@link #gainLevels(int)}). Deliberate deviation from the legacy app, which set the total.
   *
   * @return the levels gained
   * @throws IllegalArgumentException if the amount is not positive or the total gets too high to compute
   * @throws IllegalStateException during creation (the starting level sets the XP there)
   */
  public int addXp(long amount) {
    int gained = levelsGainedBy(amount);
    xp += amount;
    gainLevels(gained);
    return gained;
  }

  /**
   * Levels the character would gain by adding the XP, without changing it (for a confirmation).
   *
   * @throws IllegalArgumentException if the amount is not positive or the total gets too high to compute
   * @throws IllegalStateException during creation
   */
  public int levelsGainedBy(long amount) {
    requireComplete();
    if (amount < 1) {
      throw new IllegalArgumentException("XP to add must be positive");
    }
    try {
      return Math.max(0, ExperienceTable.levelForXp(Math.addExact(xp, amount)) - level);
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("XP total too high to compute", e);
    }
  }

  /**
   * Milestone progression (CRB p. 49): the GM grants a level instead of XP. The XP are raised to the
   * threshold of the new level if below, so XP and level stay consistent (legacy behavior).
   *
   * @throws IllegalArgumentException if the new level is too high to compute its XP (a technical limit)
   * @throws IllegalStateException during creation
   */
  public void gainMilestoneLevel() {
    requireComplete();
    try {
      xp = Math.max(xp, ExperienceTable.xpForLevel(Math.addExact(level, 1)));
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("Level too high to compute its XP", e);
    }
    gainLevels(1);
  }

  /**
   * CRB p. 49: every new level grants one skill rank and one perk to spend and +1 maximum HP (computed
   * from the level). The current HP rise by the same amount, so damage taken stays (user decision, no
   * book rule); "at maximum" stays at maximum.
   */
  private void gainLevels(int levels) {
    if (levels <= 0) {
      return;
    }
    level += levels;
    progression = new Progression(progression.pendingSkillRanks() + levels, progression.pendingPerks() + levels);
    Integer currentHp = vitals.currentHp();
    vitals = new Vitals(currentHp == null ? null : currentHp + levels, vitals.rads(), vitals.radiationPoints());
  }

  /**
   * Stages one advancement rank of the skill more ({@code +1}) or less ({@code -1}). Budget and rank
   * cap are checked by the advancement rules, not here.
   *
   * @throws IllegalArgumentException if fewer than zero staged ranks would remain
   * @throws IllegalStateException during creation
   */
  public void stageSkillRank(@NonNull Skill skill, int delta) {
    requireComplete();
    SkillState state = skillState(skill);
    skills.put(skill, new SkillState(state.tagged(), state.creationRanks(), state.advancementRanks(),
        state.stagedRanks() + delta));
  }

  /**
   * Skill ranks still to stage: the pending ranks plus those of staged Skilled ranks (CRB p. 71: two
   * per rank) minus the staged skill ranks; negative if too many are staged.
   */
  public int advancementSkillRanksLeft() {
    int fromPerks = 0;
    for (PerkSelection perk : perks) {
      if (perk.stagedRanks() > 0) {
        fromPerks += PerkCatalog.byId(perk.perk().id()).map(PerkDefinition::skillPointsPerRank).orElse(0)
            * perk.stagedRanks();
      }
    }
    int staged = skills.values().stream().mapToInt(SkillState::stagedRanks).sum();
    return progression.pendingSkillRanks() + fromPerks - staged;
  }

  /** Perk ranks still to stage: the pending perks minus the staged ranks; negative if too many. */
  public int advancementPerksLeft() {
    return progression.pendingPerks() - perks.stream().mapToInt(PerkSelection::stagedRanks).sum();
  }

  /** Whether any skill or perk rank is staged. */
  public boolean hasStagedAdvancement() {
    return skills.values().stream().anyMatch(state -> state.stagedRanks() > 0)
        || perks.stream().anyMatch(perk -> perk.stagedRanks() > 0);
  }

  /**
   * Makes the staged skill and perk ranks permanent (one-way, user decision; corrections are a GM matter,
   * D2); points left stay pending. Further problems are checked by the caller with the advancement rules.
   *
   * @throws IllegalStateException during creation or if more ranks or perks are staged than available
   */
  public void applyAdvancement() {
    requireComplete();
    int skillRanksLeft = advancementSkillRanksLeft();
    int perksLeft = advancementPerksLeft();
    if (skillRanksLeft < 0 || perksLeft < 0) {
      throw new IllegalStateException("More skill ranks or perks staged than available");
    }
    for (Skill skill : Skill.values()) {
      SkillState state = skillState(skill);
      skills.put(skill, new SkillState(state.tagged(), state.creationRanks(),
          state.advancementRanks() + state.stagedRanks()));
    }
    perks.replaceAll(perk -> new PerkSelection(perk.perk(), perk.rank(), perk.importedRanks(),
        perk.attributeChoices(), perk.skillChoices()));
    progression = new Progression(skillRanksLeft, perksLeft);
  }

  /** Takes back all staged skill and perk ranks with their choices. */
  public void discardAdvancement() {
    requireComplete();
    for (Skill skill : Skill.values()) {
      stageSkillRank(skill, -skillState(skill).stagedRanks());
    }
    for (PerkSelection perk : List.copyOf(perks)) {
      for (int i = 0; i < perk.stagedRanks(); i++) {
        removePerkRank(perk.perk().id());
      }
    }
  }

  /**
   * The level the character starts at: during creation the current level; after completion the level
   * stored at that moment; empty for an imported character, whose starting level is unknown.
   */
  public @NonNull OptionalInt getStartingLevel() {
    if (!creationComplete) {
      return OptionalInt.of(level);
    }
    return completedStartingLevel == null ? OptionalInt.empty() : OptionalInt.of(completedStartingLevel);
  }

  private void requireCreation() {
    if (creationComplete) {
      throw new IllegalStateException("This can only be changed during character creation");
    }
  }

  private void requireComplete() {
    if (!creationComplete) {
      throw new IllegalStateException("This can only be changed after character creation");
    }
  }

  public int getLevel() {
    return level;
  }

  public long getXp() {
    return xp;
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

  public int attributeAllocation(Attribute attribute) {
    return attributeAllocation.getOrDefault(attribute, 0);
  }

  public SkillState skillState(Skill skill) {
    return skills.getOrDefault(skill, SkillState.UNTRAINED);
  }

  public List<TraitSelection> getTraits() {
    return List.copyOf(traits);
  }

  public List<PerkSelection> getPerks() {
    return List.copyOf(perks);
  }

  public @NonNull Progression getProgression() {
    return progression;
  }

  public @NonNull Vitals getVitals() {
    return vitals;
  }

  public @Nullable LegacyBaseline getLegacyBaseline() {
    return legacyBaseline;
  }

  public @Nullable LegacyImport getLegacyImport() {
    return legacyImport;
  }

  public @NonNull StartingEquipment getStartingEquipment() {
    return startingEquipment;
  }

  public @NonNull String getInventoryNotes() {
    return inventoryNotes;
  }
}
