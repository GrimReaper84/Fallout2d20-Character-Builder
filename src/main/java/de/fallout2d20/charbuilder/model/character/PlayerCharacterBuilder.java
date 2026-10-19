package de.fallout2d20.charbuilder.model.character;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import de.fallout2d20.charbuilder.model.character.item.Inventory;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Collects the stored parts of a {@link PlayerCharacter} (see {@link PlayerCharacter#builder()}). Every part
 * has the value of a new, empty character in creation: no name and origin, level 1, 0 XP, all books, no
 * allocation (attributes at the base), untrained skills, no traits, perks, gear or starting equipment, full
 * vitals. {@link #build()} checks the parts.
 */
public final class PlayerCharacterBuilder {
  @NonNull String name = "";
  @Nullable CatalogRef origin;
  @NonNull OriginChoices originChoices = OriginChoices.NONE;
  int level = 1;
  long xp;
  boolean creationComplete;
  @NonNull Set<Book> enabledBooks = EnumSet.allOf(Book.class);
  @NonNull Map<Attribute, Integer> attributeAllocation = Map.of();
  @NonNull Map<Skill, SkillState> skills = Map.of();
  @NonNull List<TraitSelection> traits = List.of();
  @NonNull List<PerkSelection> perks = List.of();
  @NonNull Progression progression = Progression.NONE;
  @NonNull Vitals vitals = Vitals.FULL;
  @Nullable LegacyBaseline legacyBaseline;
  @Nullable LegacyImport legacyImport;
  @NonNull StartingEquipment startingEquipment = StartingEquipment.NONE;
  @NonNull String inventoryNotes = "";
  @Nullable Integer startingLevel;
  @Nullable Set<Skill> startingTagSkills;
  @NonNull List<CarriedWeapon> weapons = List.of();
  @NonNull Map<String, Integer> ammunition = Map.of();
  @NonNull List<WornApparel> apparel = List.of();
  @NonNull Inventory inventory = Inventory.EMPTY;
  @Nullable String createdWith;

  PlayerCharacterBuilder() {
  }

  public @NonNull PlayerCharacterBuilder name(@NonNull String name) {
    this.name = name;
    return this;
  }

  public @NonNull PlayerCharacterBuilder origin(@Nullable CatalogRef origin) {
    this.origin = origin;
    return this;
  }

  public @NonNull PlayerCharacterBuilder originChoices(@NonNull OriginChoices originChoices) {
    this.originChoices = originChoices;
    return this;
  }

  /** At least 1. */
  public @NonNull PlayerCharacterBuilder level(int level) {
    this.level = level;
    return this;
  }

  /** Not negative. */
  public @NonNull PlayerCharacterBuilder xp(long xp) {
    this.xp = xp;
    return this;
  }

  public @NonNull PlayerCharacterBuilder creationComplete(boolean creationComplete) {
    this.creationComplete = creationComplete;
    return this;
  }

  /** The Core Rulebook is always added. */
  public @NonNull PlayerCharacterBuilder enabledBooks(@NonNull Set<Book> enabledBooks) {
    this.enabledBooks = enabledBooks;
    return this;
  }

  /** Missing attributes count as allocation 0. */
  public @NonNull PlayerCharacterBuilder attributeAllocation(@NonNull Map<Attribute, Integer> allocation) {
    this.attributeAllocation = allocation;
    return this;
  }

  /** Missing skills count as untrained. */
  public @NonNull PlayerCharacterBuilder skills(@NonNull Map<Skill, SkillState> skills) {
    this.skills = skills;
    return this;
  }

  public @NonNull PlayerCharacterBuilder traits(@NonNull List<TraitSelection> traits) {
    this.traits = traits;
    return this;
  }

  public @NonNull PlayerCharacterBuilder perks(@NonNull List<PerkSelection> perks) {
    this.perks = perks;
    return this;
  }

  public @NonNull PlayerCharacterBuilder progression(@NonNull Progression progression) {
    this.progression = progression;
    return this;
  }

  public @NonNull PlayerCharacterBuilder vitals(@NonNull Vitals vitals) {
    this.vitals = vitals;
    return this;
  }

  public @NonNull PlayerCharacterBuilder legacyBaseline(@Nullable LegacyBaseline legacyBaseline) {
    this.legacyBaseline = legacyBaseline;
    return this;
  }

  public @NonNull PlayerCharacterBuilder legacyImport(@Nullable LegacyImport legacyImport) {
    this.legacyImport = legacyImport;
    return this;
  }

  /**
   * Fitted to the catalog on {@link #build()} (see {@link StartingEquipment#fittedToCatalog(boolean)}; a complete
   * character keeps stale picks).
   */
  public @NonNull PlayerCharacterBuilder startingEquipment(@NonNull StartingEquipment startingEquipment) {
    this.startingEquipment = startingEquipment;
    return this;
  }

  public @NonNull PlayerCharacterBuilder inventoryNotes(@NonNull String inventoryNotes) {
    this.inventoryNotes = inventoryNotes;
    return this;
  }

  /**
   * The level a complete character started at, between 1 and its level; {@code null} if unknown (imported
   * characters) and always for a character in creation (see {@link Advancement#getStartingLevel()}).
   */
  public @NonNull PlayerCharacterBuilder startingLevel(@Nullable Integer startingLevel) {
    this.startingLevel = startingLevel;
    return this;
  }

  /**
   * The Tag skills of a complete character at the end of creation, {@code null} if not stored (then its current
   * Tag skills count) and always for a character in creation (see {@link PlayerCharacter#getStartingTagSkills()}).
   */
  public @NonNull PlayerCharacterBuilder startingTagSkills(@Nullable Set<Skill> startingTagSkills) {
    this.startingTagSkills = startingTagSkills;
    return this;
  }

  public @NonNull PlayerCharacterBuilder weapons(@NonNull List<CarriedWeapon> weapons) {
    this.weapons = weapons;
    return this;
  }

  /** Shots per ammunition id, none negative; counts of 0 are dropped. */
  public @NonNull PlayerCharacterBuilder ammunition(@NonNull Map<String, Integer> ammunition) {
    this.ammunition = ammunition;
    return this;
  }

  /** The worn and carried clothing, armor and robot armor. */
  public @NonNull PlayerCharacterBuilder apparel(@NonNull List<WornApparel> apparel) {
    this.apparel = apparel;
    return this;
  }

  /** The carried items, caps and junk/material counters. */
  public @NonNull PlayerCharacterBuilder inventory(@NonNull Inventory inventory) {
    this.inventory = inventory;
    return this;
  }

  /**
   * The app version that created the character, {@code null} if unknown (legacy imports, files written before it
   * was stored).
   */
  public @NonNull PlayerCharacterBuilder createdWith(@Nullable String createdWith) {
    this.createdWith = createdWith;
    return this;
  }

  /**
   * The character from the collected parts.
   *
   * @throws IllegalArgumentException if the level is below 1, the XP negative, a starting level or Tag skills
   *     are given for a character in creation, the starting level lies outside 1 to the level, or an ammunition
   *     entry has a blank id or negative shots
   */
  public @NonNull PlayerCharacter build() {
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
    ammunition.forEach((id, shots) -> {
      if (id.isBlank() || shots < 0) {
        throw new IllegalArgumentException("Ammunition needs an id and a non-negative number of shots");
      }
    });
    return new PlayerCharacter(this);
  }
}
