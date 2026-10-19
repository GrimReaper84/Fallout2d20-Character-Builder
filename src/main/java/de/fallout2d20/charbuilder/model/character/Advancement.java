package de.fallout2d20.charbuilder.model.character;

import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import java.util.List;
import java.util.OptionalInt;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Level, XP and advancement of a character (CRB p. 49). Part of a {@link PlayerCharacter}
 * ({@link PlayerCharacter#advancement()}).
 *
 * <p>After creation, XP and levels grant skill ranks and perks, which are first staged (they count everywhere
 * but can be taken back) and then applied.
 */
public final class Advancement {

  private final @NonNull PlayerCharacter owner;
  private int level;
  private long xp;
  private @NonNull Progression progression;
  /** Level at completion; null while in creation (then the current level) or when unknown (imports). */
  private @Nullable Integer completedStartingLevel;

  Advancement(@NonNull PlayerCharacter owner, int level, long xp, @NonNull Progression progression,
      @Nullable Integer completedStartingLevel) {
    this.owner = owner;
    this.level = level;
    this.xp = xp;
    this.progression = progression;
    this.completedStartingLevel = completedStartingLevel;
  }

  public int getLevel() {
    return level;
  }

  public long getXp() {
    return xp;
  }

  public @NonNull Progression getProgression() {
    return progression;
  }

  /**
   * The level the character starts at: during creation the current level; after completion the level
   * stored at that moment; empty for an imported character, whose starting level is unknown.
   */
  public @NonNull OptionalInt getStartingLevel() {
    if (!owner.isCreationComplete()) {
      return OptionalInt.of(level);
    }
    return completedStartingLevel == null ? OptionalInt.empty() : OptionalInt.of(completedStartingLevel);
  }

  /**
   * Sets the starting level during creation. CRB p. 49: characters start at level 1 unless the group
   * agrees on a higher one; the book sets no upper limit (user decision). XP becomes the minimum of
   * that level (user decision; CRB p. 49 table and formula). Skill ranks above the rank cap of the new
   * level are trimmed (user decision, see {@link Abilities#adjustSkillsToRules()}); perks and skill points
   * that no longer fit stay and are reported by the creation rules.
   *
   * @throws IllegalArgumentException if the level is below 1, or so high (above roughly 400 million)
   *     that its XP no longer fits into a number – a technical limit, not a rule
   */
  public void setStartingLevel(int startingLevel) {
    owner.requireStrictCreation();
    if (startingLevel < 1) {
      throw new IllegalArgumentException("Level must be at least 1");
    }
    try {
      xp = ExperienceTable.xpForLevel(startingLevel);
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("Level " + startingLevel + " is too high to compute its XP", e);
    }
    level = startingLevel;
    owner.abilities().adjustSkillsToRules();
    owner.startingGear().startingLevelChanged();
  }

  /** Keeps the current level as the starting level when creation is completed (CRB p. 81). */
  void markStartingLevel() {
    completedStartingLevel = level;
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
    owner.requireComplete();
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
    owner.requireComplete();
    try {
      xp = Math.max(xp, ExperienceTable.xpForLevel(Math.addExact(level, 1)));
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("Level too high to compute its XP", e);
    }
    gainLevels(1);
  }

  /**
   * CRB p. 49: every new level grants one skill rank and one perk to spend and +1 maximum HP (computed
   * from the level); the current HP rise with it ({@link Session#raiseCurrentHp(int)}).
   */
  private void gainLevels(int levels) {
    if (levels <= 0) {
      return;
    }
    level += levels;
    progression = new Progression(progression.pendingSkillRanks() + levels, progression.pendingPerks() + levels);
    owner.session().raiseCurrentHp(levels);
  }

  /**
   * Stages one advancement rank of the skill more ({@code +1}) or less ({@code -1}). Budget and rank
   * cap are checked by the advancement rules, not here.
   *
   * @throws IllegalArgumentException if fewer than zero staged ranks would remain
   * @throws IllegalStateException during creation
   */
  public void stageSkillRank(@NonNull Skill skill, int delta) {
    owner.requireComplete();
    Abilities abilities = owner.abilities();
    SkillState state = abilities.skillState(skill);
    abilities.setSkillState(skill, state.withAdvancement(state.advancementRanks(), state.stagedRanks() + delta));
  }

  /**
   * Skill ranks still to stage: the pending ranks plus those of staged Skilled ranks (CRB p. 71: two
   * per rank) minus the staged skill ranks; negative if too many are staged.
   */
  public int skillRanksLeft() {
    int fromPerks = 0;
    for (PerkSelection perk : owner.perkList()) {
      if (perk.stagedRanks() > 0) {
        fromPerks += PerkCatalog.byId(perk.perk().id()).map(PerkDefinition::skillPointsPerRank).orElse(0)
            * perk.stagedRanks();
      }
    }
    return progression.pendingSkillRanks() + fromPerks - owner.abilities().stagedRanks();
  }

  /** Perk ranks still to stage: the pending perks minus the staged ranks; negative if too many. */
  public int perksLeft() {
    return progression.pendingPerks() - owner.perkList().stream().mapToInt(PerkSelection::stagedRanks).sum();
  }

  /** Whether any skill or perk rank is staged. */
  public boolean hasStaged() {
    return owner.abilities().stagedRanks() > 0
        || owner.perkList().stream().anyMatch(perk -> perk.stagedRanks() > 0);
  }

  /**
   * Makes the staged skill and perk ranks permanent (one-way, user decision; corrections are a GM matter,
   * D2); points left stay pending. Further problems are checked by the caller with the advancement rules.
   *
   * @throws IllegalStateException during creation or if more ranks or perks are staged than available
   */
  public void apply() {
    owner.requireComplete();
    int skillRanksLeft = skillRanksLeft();
    int perksLeft = perksLeft();
    if (skillRanksLeft < 0 || perksLeft < 0) {
      throw new IllegalStateException("More skill ranks or perks staged than available");
    }
    owner.abilities().applyStagedRanks();
    owner.applyStagedPerkRanks();
    progression = new Progression(skillRanksLeft, perksLeft);
  }

  /** Takes back all staged skill and perk ranks with their choices. */
  public void discard() {
    owner.requireComplete();
    for (Skill skill : Skill.values()) {
      stageSkillRank(skill, -owner.abilities().skillState(skill).stagedRanks());
    }
    for (PerkSelection perk : List.copyOf(owner.perkList())) {
      for (int i = 0; i < perk.stagedRanks(); i++) {
        owner.removePerkRank(perk.perk().id());
      }
    }
  }

  /**
   * Sets level and XP freely (D2), also downwards. Lowering the level lowers a stored starting level above
   * it, takes back staged advancement, cuts the pending level-up points by the levels lost and caps the
   * current HP at the new maximum; raising grants no points (user decision 2026-10-16).
   *
   * @throws IllegalStateException outside GM mode
   * @throws IllegalArgumentException for a level below 1 or negative XP
   */
  public void setLevelAsGm(int newLevel, long newXp) {
    owner.requireGmMode();
    if (newLevel < 1 || newXp < 0) {
      throw new IllegalArgumentException("Level must be at least 1 and XP not negative");
    }
    int lost = level - newLevel;
    level = newLevel;
    xp = newXp;
    Integer starting = completedStartingLevel;
    if (starting != null && starting > newLevel) {
      completedStartingLevel = newLevel;
    }
    if (lost > 0) {
      if (owner.isCreationComplete()) {
        discard();
      }
      progression = new Progression(Math.max(0, progression.pendingSkillRanks() - lost),
          Math.max(0, progression.pendingPerks() - lost));
      owner.session().capCurrentHp();
    }
  }
}
