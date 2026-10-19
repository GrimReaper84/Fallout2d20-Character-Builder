package de.fallout2d20.charbuilder.model.character;

import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkChoice;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.catalog.trait.TraitCatalog;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * S.P.E.C.I.A.L. and skills of a character. Part of a {@link PlayerCharacter}
 * ({@link PlayerCharacter#abilities()}).
 *
 * <p>Attributes and skills are stored as their parts (allocation, ranks), not as effective values, so every
 * value can be traced back. A character imported from a legacy save carries a {@link LegacyBaseline} that
 * replaces base and creation allocation; origin and trait effects (attribute modifiers, origin Tag skills,
 * Gifted) apply only to characters without it, because an imported baseline already contains them.
 */
public final class Abilities {

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

  private final @NonNull PlayerCharacter owner;
  private final @NonNull Map<Attribute, Integer> attributeAllocation;
  private final @NonNull Map<Skill, SkillState> skills;

  /** Missing attributes count as allocation 0, missing skills as untrained. */
  Abilities(@NonNull PlayerCharacter owner, @NonNull Map<Attribute, Integer> allocation,
      @NonNull Map<Skill, SkillState> skills) {
    this.owner = owner;
    this.attributeAllocation = new EnumMap<>(Attribute.class);
    for (Attribute attribute : Attribute.values()) {
      this.attributeAllocation.put(attribute, allocation.getOrDefault(attribute, 0));
    }
    this.skills = new EnumMap<>(Skill.class);
    for (Skill skill : Skill.values()) {
      this.skills.put(skill, skills.getOrDefault(skill, SkillState.UNTRAINED));
    }
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
    LegacyBaseline legacyBaseline = owner.getLegacyBaseline();
    if (legacyBaseline != null) {
      return legacyBaseline.attribute(attribute) + attributeAllocation(attribute) + perkBonus(attribute);
    }
    int modifier = owner.getOriginData().map(data -> data.attributeModifier(attribute)).orElse(0);
    return ATTRIBUTE_BASE + modifier + traitBonus(attribute) + attributeAllocation(attribute)
        + perkBonus(attribute);
  }

  /**
   * +1 per new (not imported) rank of an attribute-choice perk that chose the attribute. The new ranks
   * are the last ones, and their choices the last ones of the list (imported ranks may lack choices).
   */
  private int perkBonus(@NonNull Attribute attribute) {
    int bonus = 0;
    for (PerkSelection perk : owner.perkList()) {
      if (choiceOf(perk) == PerkChoice.ATTRIBUTE) {
        List<Attribute> choices = perk.attributeChoices();
        int newRanks = perk.rank() - perk.importedRanks();
        bonus += (int) choices.subList(Math.max(0, choices.size() - newRanks), choices.size())
            .stream().filter(choice -> choice == attribute).count();
      }
    }
    return bonus;
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
    for (TraitSelection trait : owner.traitList()) {
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
    return skillRank(skill, skillState(skill));
  }

  /** Whether a GM correction by {@code delta} changes the effective rank (D2; a Tag! bonus or 0 can hide it). */
  public boolean gmStepChangesRank(@NonNull Skill skill, int delta) {
    SkillState state = skillState(skill);
    return skillRank(skill, state.withGmRanks(state.gmRanks() + delta)) != skillRank(skill, state);
  }

  private int skillRank(@NonNull Skill skill, @NonNull SkillState state) {
    boolean perkTagOnly = isTaggedByPerk(skill) && !state.tagged() && !isTaggedByOrigin(skill);
    LegacyBaseline legacyBaseline = owner.getLegacyBaseline();
    int start = legacyBaseline != null
        ? legacyBaseline.skillRank(skill)
        : (isTagged(skill) && !perkTagOnly ? TAG_SKILL_BONUS : 0) + state.creationRanks();
    int rank = Math.max(0, start + state.advancementRanks() + state.stagedRanks() + state.gmRanks());
    return perkTagOnly ? Math.max(rank, Math.min(rank + TAG_SKILL_BONUS, skillRankCap())) : rank;
  }

  /**
   * Highest rank any skill of this character may have: during creation 3 below level 3, otherwise 6
   * (CRB p. 58), lowered to the origin's skill cap (Super Mutant CRB p. 53, Nightkin WG p. 14).
   */
  public int skillRankCap() {
    int cap = !owner.isCreationComplete() && owner.advancement().getLevel() < FULL_SKILL_RANKS_FROM_LEVEL
        ? CREATION_SKILL_RANK_CAP : MAX_SKILL_RANK;
    Optional<OriginData> data = owner.getOriginData();
    if (data.isEmpty()) {
      return cap;
    }
    Integer originCap = data.get().skillRankCap();
    return originCap == null ? cap : Math.min(cap, originCap);
  }

  /** Whether the origin tags the skill: its fixed Tag skill or the chosen extra Tag skill. */
  public boolean isTaggedByOrigin(@NonNull Skill skill) {
    return owner.getLegacyBaseline() == null && (skill == owner.getOriginChoices().tagSkill()
        || owner.getOriginData().map(data -> data.fixedTagSkill() == skill).orElse(false));
  }

  /** The chosen trait that forbids the skill as a Tag skill (Nomad: Science, WG p. 15), if any. */
  public @NonNull Optional<TraitDefinition> tagForbiddenBy(@NonNull Skill skill) {
    return owner.traitList().stream()
        .map(selection -> TraitCatalog.byId(selection.trait().id()))
        .flatMap(Optional::stream)
        .filter(trait -> trait.forbiddenTagSkills().contains(skill))
        .findFirst();
  }

  /** Whether a perk with a skill choice made the skill a Tag skill (Tag!, CRB p. 73). */
  public boolean isTaggedByPerk(@NonNull Skill skill) {
    return owner.perkList().stream().anyMatch(perk -> choiceOf(perk) == PerkChoice.SKILL
        && perk.skillChoices().contains(skill));
  }

  /** Whether the skill is tagged by its origin or a perk, i.e. not freely (user decision: separate sources). */
  private boolean hasFixedTag(@NonNull Skill skill) {
    return isTaggedByOrigin(skill) || isTaggedByPerk(skill);
  }

  /**
   * Lowers the allocation of every attribute above the origin's maximum (e.g. Super Mutant INT 6,
   * CRB p. 53) down to that maximum; the points return to the S.P.E.C.I.A.L. budget (user decision).
   */
  void adjustAttributesToOrigin() {
    for (Attribute attribute : Attribute.values()) {
      int maximum = owner.getOriginData().map(data -> data.attributeMaximum(attribute))
          .orElse(OriginData.DEFAULT_ATTRIBUTE_MAXIMUM);
      int excess = attributeValue(attribute) - maximum;
      if (excess > 0) {
        attributeAllocation.put(attribute, attributeAllocation(attribute) - excess);
      }
    }
  }

  /**
   * Keeps free Tags and bought ranks consistent after origin, traits, perks or level changed (user decision):
   * a free Tag is removed from a skill the origin or a perk tags (it returns to the free Tag budget;
   * e.g. Ghoul Survival, Tag!) or a trait forbids (Nomad: Science), and bought ranks are lowered as far
   * as needed to stay within {@link #skillRankCap()}, as when tagging a skill.
   */
  void adjustSkillsToRules() {
    for (Skill skill : Skill.values()) {
      SkillState state = skillState(skill);
      boolean tagged = state.tagged() && !hasFixedTag(skill) && tagForbiddenBy(skill).isEmpty();
      skills.put(skill, state.withCreation(tagged, ranksWithinCap(skill, tagged)));
    }
  }

  /** The bought ranks of the skill, lowered so its rank stays within the cap (never below 0). */
  private int ranksWithinCap(@NonNull Skill skill, boolean freeTag) {
    SkillState state = skillState(skill);
    int tagBonus = freeTag || hasFixedTag(skill) ? TAG_SKILL_BONUS : 0;
    int room = skillRankCap() - tagBonus - state.advancementRanks() - state.stagedRanks()
        - Math.max(0, state.gmRanks());
    return Math.max(0, Math.min(state.creationRanks(), room));
  }

  /**
   * Sets the creation allocation of an attribute: points spent on it, or −1 for an attribute lowered
   * below its starting value (CRB p. 58). The range and the budget are checked by the creation
   * rules, not here.
   */
  public void setAttributeAllocation(@NonNull Attribute attribute, int allocation) {
    owner.requireCreation();
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
    owner.requireCreation();
    owner.startingGear().syncing(() -> {
      if (tagged && (hasFixedTag(skill) || tagForbiddenBy(skill).isPresent())) {
        throw new IllegalArgumentException(skill.displayName() + " cannot get a free Tag");
      }
      SkillState state = skillState(skill);
      int creationRanks = tagged ? ranksWithinCap(skill, true) : state.creationRanks();
      skills.put(skill, state.withCreation(tagged, creationRanks));
    });
  }

  /** Sets the ranks bought with skill points during creation; budget and cap are checked by the rules. */
  public void setCreationRanks(@NonNull Skill skill, int creationRanks) {
    owner.requireCreation();
    SkillState state = skillState(skill);
    skills.put(skill, state.withCreation(state.tagged(), creationRanks));
  }

  /**
   * GM correction of a skill rank by {@code delta} (D2), counted at once; works for imported characters
   * too, whose legacy rank is kept as baseline. The rank cap is checked by the rules.
   *
   * @throws IllegalStateException outside GM mode
   */
  public void adjustSkillRankAsGm(@NonNull Skill skill, int delta) {
    owner.requireGmMode();
    SkillState state = skillState(skill);
    skills.put(skill, state.withGmRanks(state.gmRanks() + delta));
  }

  public int attributeAllocation(@NonNull Attribute attribute) {
    return attributeAllocation.getOrDefault(attribute, 0);
  }

  public @NonNull SkillState skillState(@NonNull Skill skill) {
    return skills.getOrDefault(skill, SkillState.UNTRAINED);
  }

  /** Replaces the stored parts of a skill (advancement staging). */
  void setSkillState(@NonNull Skill skill, @NonNull SkillState state) {
    skills.put(skill, state);
  }

  /** Staged advancement ranks over all skills. */
  int stagedRanks() {
    return skills.values().stream().mapToInt(SkillState::stagedRanks).sum();
  }

  /** Makes the staged advancement ranks of every skill permanent. */
  void applyStagedRanks() {
    skills.replaceAll((skill, state) -> state.withAdvancement(state.advancementRanks() + state.stagedRanks(), 0));
  }
}
