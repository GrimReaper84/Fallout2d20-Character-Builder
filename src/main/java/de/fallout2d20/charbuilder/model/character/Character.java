package de.fallout2d20.charbuilder.model.character;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.origin.OriginCatalog;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.trait.TraitCatalog;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
 * choices, traits, attribute allocation, free Tags and bought skill ranks can only be changed while
 * creation is not complete.
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
  private final int level;
  private final long xp;
  private final boolean creationComplete;
  private final Set<Book> enabledBooks;
  private final Map<Attribute, Integer> attributeAllocation;
  private final Map<Skill, SkillState> skills;
  private final List<TraitSelection> traits;
  private final List<PerkSelection> perks;
  private final Progression progression;
  private final Vitals vitals;
  private final @Nullable LegacyBaseline legacyBaseline;
  private final @Nullable LegacyImport legacyImport;

  /**
   * Creates a character from stored parts. Missing attributes count as allocation 0, missing
   * skills as untrained.
   */
  public Character(
      String name,
      @Nullable CatalogRef origin,
      @NonNull OriginChoices originChoices,
      int level,
      long xp,
      boolean creationComplete,
      @NonNull Set<Book> enabledBooks,
      Map<Attribute, Integer> attributeAllocation,
      Map<Skill, SkillState> skills,
      List<TraitSelection> traits,
      List<PerkSelection> perks,
      Progression progression,
      Vitals vitals,
      @Nullable LegacyBaseline legacyBaseline,
      @Nullable LegacyImport legacyImport) {
    if (level < 1 || xp < 0) {
      throw new IllegalArgumentException("Level must be at least 1 and XP not negative");
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
   * <p>Gifted's chosen attributes add +1 each (CRB p. 56). Perks such as Intense Training are added
   * here by their feature; they must skip the imported ranks of a perk, whose effects are already
   * part of the legacy baseline.
   */
  public int attributeValue(@NonNull Attribute attribute) {
    if (legacyBaseline != null) {
      return legacyBaseline.attribute(attribute) + attributeAllocation(attribute);
    }
    int modifier = getOriginData().map(data -> data.attributeModifier(attribute)).orElse(0);
    return ATTRIBUTE_BASE + modifier + traitBonus(attribute) + attributeAllocation(attribute);
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
   * Whether the skill is a Tag skill: one of the freely chosen Tag skills, or one granted by the
   * origin – its fixed Tag skill (Ghoul: Survival, in addition to the three free ones, user reading
   * of CRB p. 52) or the chosen extra Tag skill (e.g. Vault Kid, CRB p. 57).
   */
  public boolean isTagged(@NonNull Skill skill) {
    return skillState(skill).tagged() || isTaggedByOrigin(skill);
  }

  /**
   * Effective skill rank: Tag bonus plus creation ranks (or the legacy baseline rank for an
   * imported character, which already includes the Tag bonus) plus advancement ranks. Rank caps
   * are validated by creation and advancement, not here.
   */
  public int skillRank(@NonNull Skill skill) {
    SkillState state = skillState(skill);
    int start = legacyBaseline != null
        ? legacyBaseline.skillRank(skill)
        : (isTagged(skill) ? TAG_SKILL_BONUS : 0) + state.creationRanks();
    return start + state.advancementRanks();
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
   * the new origin (see {@link #adjustAttributesToOrigin()} and {@link #adjustSkillsToRules()}).
   */
  public void chooseOrigin(@NonNull CatalogRef newOrigin) {
    requireCreation();
    origin = newOrigin;
    originChoices = OriginChoices.NONE;
    traits.clear();
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
   * Keeps free Tags and bought ranks consistent after origin or traits changed (user decision): a free
   * Tag is removed from a skill the origin tags (it returns to the free Tag budget; e.g. Ghoul
   * Survival) or a trait forbids (Nomad: Science), and bought ranks are lowered as far as needed to
   * stay within {@link #skillRankCap()}, as when tagging a skill.
   */
  private void adjustSkillsToRules() {
    for (Skill skill : Skill.values()) {
      SkillState state = skillState(skill);
      boolean tagged = state.tagged() && !isTaggedByOrigin(skill) && tagForbiddenBy(skill).isEmpty();
      skills.put(skill, new SkillState(tagged, ranksWithinCap(skill, tagged), state.advancementRanks()));
    }
  }

  /** The bought ranks of the skill, lowered so its rank stays within the cap (never below 0). */
  private int ranksWithinCap(@NonNull Skill skill, boolean freeTag) {
    SkillState state = skillState(skill);
    int tagBonus = freeTag || isTaggedByOrigin(skill) ? TAG_SKILL_BONUS : 0;
    int room = skillRankCap() - tagBonus - state.advancementRanks();
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
   * @throws IllegalArgumentException when tagging a skill the origin already tags or a trait forbids
   */
  public void setFreeTag(@NonNull Skill skill, boolean tagged) {
    requireCreation();
    if (tagged && (isTaggedByOrigin(skill) || tagForbiddenBy(skill).isPresent())) {
      throw new IllegalArgumentException(skill.displayName() + " cannot get a free Tag");
    }
    SkillState state = skillState(skill);
    int creationRanks = tagged ? ranksWithinCap(skill, true) : state.creationRanks();
    skills.put(skill, new SkillState(tagged, creationRanks, state.advancementRanks()));
  }

  /** Sets the ranks bought with skill points during creation; budget and cap are checked by the rules. */
  public void setCreationRanks(@NonNull Skill skill, int creationRanks) {
    requireCreation();
    SkillState state = skillState(skill);
    skills.put(skill, new SkillState(state.tagged(), creationRanks, state.advancementRanks()));
  }

  /** Mister Handy without a pincer (CRB p. 55). */
  public void setNoPincer(boolean noPincer) {
    requireCreation();
    originChoices = originChoices.withNoPincer(noPincer);
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

  /** Sets the attribute choices of a chosen trait (Gifted); checked by the creation rules, not here. */
  public void setTraitAttributeChoices(@NonNull String traitId, @NonNull List<Attribute> choices) {
    requireCreation();
    traits.replaceAll(selection -> selection.trait().id().equals(traitId)
        ? new TraitSelection(selection.trait(), selection.imported(), choices) : selection);
  }

  private void requireCreation() {
    if (creationComplete) {
      throw new IllegalStateException("This can only be changed during character creation");
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

  public Progression getProgression() {
    return progression;
  }

  public Vitals getVitals() {
    return vitals;
  }

  public @Nullable LegacyBaseline getLegacyBaseline() {
    return legacyBaseline;
  }

  public @Nullable LegacyImport getLegacyImport() {
    return legacyImport;
  }
}
