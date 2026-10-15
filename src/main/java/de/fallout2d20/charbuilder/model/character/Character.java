package de.fallout2d20.charbuilder.model.character;

import de.fallout2d20.charbuilder.catalog.Book;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A player character (aggregate root).
 *
 * <p>Attributes and skills are stored as their parts (base, allocation, ranks), not as effective
 * values, so every value can be traced back. A character imported from a legacy save carries a
 * {@link LegacyBaseline} that replaces base and creation allocation.
 */
public class Character {

  /** CRB p. 58: every S.P.E.C.I.A.L. attribute starts at 5 before the origin is applied. */
  public static final int ATTRIBUTE_BASE = 5;

  /** CRB p. 58: a Tag skill starts at rank 2. */
  public static final int TAG_SKILL_BONUS = 2;

  private String name;
  private @Nullable CatalogRef origin;
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
    return new Character("", null, 1, 0, false, EnumSet.allOf(Book.class), Map.of(), Map.of(),
        List.of(), List.of(),
        Progression.NONE, Vitals.FULL, null, null);
  }

  /**
   * Effective attribute value: the base (5, or the legacy baseline value for an imported
   * character) plus the creation allocation.
   *
   * <p>Origin modifiers, traits (Gifted) and perks (Intense Training) are added here by their
   * features; they must skip imported traits and the imported ranks of a perk, whose effects are
   * already part of the legacy baseline.
   */
  public int attributeValue(Attribute attribute) {
    int base = legacyBaseline != null ? legacyBaseline.attribute(attribute) : ATTRIBUTE_BASE;
    return base + attributeAllocation(attribute);
  }

  /**
   * Effective skill rank: Tag bonus plus creation ranks (or the legacy baseline rank for an
   * imported character, which already includes the Tag bonus) plus advancement ranks. Rank caps
   * are validated by creation and advancement, not here.
   */
  public int skillRank(Skill skill) {
    SkillState state = skillState(skill);
    int start = legacyBaseline != null
        ? legacyBaseline.skillRank(skill)
        : (state.tagged() ? TAG_SKILL_BONUS : 0) + state.creationRanks();
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
