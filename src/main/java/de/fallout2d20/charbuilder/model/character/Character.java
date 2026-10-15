package de.fallout2d20.charbuilder.model.character;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.origin.OriginCatalog;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
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
 * choices and traits can only be changed while creation is not complete.
 */
public class Character {

  /** CRB p. 58: every S.P.E.C.I.A.L. attribute starts at 5 before the origin is applied. */
  public static final int ATTRIBUTE_BASE = 5;

  /** CRB p. 58: a Tag skill starts at rank 2. */
  public static final int TAG_SKILL_BONUS = 2;

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
    if (skillState(skill).tagged()) {
      return true;
    }
    if (legacyBaseline != null) {
      return false;
    }
    return skill == originChoices.tagSkill()
        || getOriginData().map(data -> data.fixedTagSkill() == skill).orElse(false);
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
   * the origin (user decision); attribute allocation and free Tag skills are kept. A free Tag on the
   * new origin's fixed Tag skill is removed, since the origin tags that skill anyway.
   */
  public void chooseOrigin(@NonNull CatalogRef newOrigin) {
    requireCreation();
    origin = newOrigin;
    originChoices = OriginChoices.NONE;
    traits.clear();
    getOriginData().map(OriginData::fixedTagSkill).ifPresent(skill -> {
      SkillState state = skillState(skill);
      skills.put(skill, new SkillState(false, state.creationRanks(), state.advancementRanks()));
    });
  }

  /** The additional Tag skill granted by the origin; checked by the creation rules, not here. */
  public void chooseOriginTagSkill(@Nullable Skill skill) {
    requireCreation();
    originChoices = originChoices.withTagSkill(skill);
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

  /** Takes a trait during creation; a trait already chosen is not added twice. */
  public void chooseTrait(@NonNull CatalogRef trait) {
    requireCreation();
    if (traits.stream().noneMatch(selection -> selection.trait().id().equals(trait.id()))) {
      traits.add(new TraitSelection(trait, false, List.of()));
    }
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
