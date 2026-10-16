package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import de.fallout2d20.charbuilder.model.character.item.Inventory;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class CharacterTest {

  @Test
  void newCharacterStartsWithBaseValues() {
    Character character = Character.createNew();
    for (Attribute attribute : Attribute.values()) {
      assertEquals(5, character.attributeValue(attribute));
    }
    for (Skill skill : Skill.values()) {
      assertEquals(0, character.skillRank(skill));
    }
    assertFalse(character.isLegacyImport());
    assertFalse(character.isCreationComplete());
  }

  /** The Core Rulebook can never be switched off (user decision). */
  @Test
  void coreRulebookIsAlwaysEnabled() {
    assertEquals(EnumSet.allOf(Book.class), Character.createNew().getEnabledBooks());
    Character character = new Character("Test", null, OriginChoices.NONE, 1, 0, false, EnumSet.of(Book.WOA), Map.of(),
        Map.of(), List.of(), List.of(), Progression.NONE, Vitals.FULL, null, null);
    assertEquals(EnumSet.of(Book.CRB, Book.WOA), character.getEnabledBooks());
  }

  /** CRB p. 58: base 5 plus spent points; an attribute may be lowered to 4. */
  @Test
  void attributeIsBasePlusAllocation() {
    Character character = create(Map.of(Attribute.STRENGTH, 2, Attribute.CHARISMA, -1), Map.of(), null);
    assertEquals(7, character.attributeValue(Attribute.STRENGTH));
    assertEquals(4, character.attributeValue(Attribute.CHARISMA));
  }

  /** CRB p. 58: a Tag skill starts at rank 2. */
  @Test
  void tagSkillAddsTwoRanks() {
    Character character = create(Map.of(),
        Map.of(Skill.REPAIR, new SkillState(true, 1, 0), Skill.SNEAK, new SkillState(false, 2, 1)), null);
    assertEquals(3, character.skillRank(Skill.REPAIR));
    assertEquals(3, character.skillRank(Skill.SNEAK));
  }

  @Test
  void legacyBaselineReplacesBaseAndTagBonus() {
    Map<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
    for (Attribute attribute : Attribute.values()) {
      attributes.put(attribute, 6);
    }
    attributes.put(Attribute.STRENGTH, 4);
    Map<Skill, Integer> ranks = new EnumMap<>(Skill.class);
    for (Skill skill : Skill.values()) {
      ranks.put(skill, 0);
    }
    ranks.put(Skill.LOCKPICK, 3);
    Character character = create(Map.of(),
        Map.of(Skill.LOCKPICK, new SkillState(true, 0, 1)), new LegacyBaseline(attributes, ranks));

    assertTrue(character.isLegacyImport());
    assertEquals(4, character.attributeValue(Attribute.STRENGTH));
    assertEquals(4, character.skillRank(Skill.LOCKPICK));
    assertTrue(character.skillState(Skill.LOCKPICK).tagged());
  }

  /** CRB p. 53: a new Super Mutant starts with STR and END 7; an imported one keeps its baseline. */
  @Test
  void superMutantGetsAttributeModifiers() {
    Character mutant = Character.createNew();
    mutant.chooseOrigin(CatalogRef.of("super-mutant"));
    assertEquals(7, mutant.attributeValue(Attribute.STRENGTH));
    assertEquals(7, mutant.attributeValue(Attribute.ENDURANCE));
    assertEquals(5, mutant.attributeValue(Attribute.INTELLIGENCE));

    Character imported = create(Map.of(), Map.of(), baseline(8));
    assertEquals(8, new Character("Imported", CatalogRef.of("super-mutant"), OriginChoices.NONE, 1, 0,
        true, EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(), Progression.NONE,
        Vitals.FULL, imported.getLegacyBaseline(), null).attributeValue(Attribute.STRENGTH));
  }

  /** CRB p. 52 (user reading): Survival is tagged in addition; a free Survival Tag is removed. */
  @Test
  void ghoulSurvivalIsAnAdditionalTag() {
    Character ghoul = create(Map.of(), Map.of(Skill.SURVIVAL, new SkillState(true, 1, 0)), null);
    ghoul.chooseOrigin(CatalogRef.of("ghoul"));
    assertFalse(ghoul.skillState(Skill.SURVIVAL).tagged());
    assertTrue(ghoul.isTagged(Skill.SURVIVAL));
    assertEquals(3, ghoul.skillRank(Skill.SURVIVAL));
  }

  /** CRB p. 51: the extra Tag skill of the Brotherhood counts as a Tag skill. */
  @Test
  void originTagSkillCountsAsTag() {
    Character initiate = Character.createNew();
    initiate.chooseOrigin(CatalogRef.of("brotherhood-initiate"));
    initiate.chooseOriginTagSkill(Skill.SCIENCE);
    assertTrue(initiate.isTagged(Skill.SCIENCE));
    assertEquals(2, initiate.skillRank(Skill.SCIENCE));
    assertFalse(initiate.isTagged(Skill.REPAIR));
  }

  @Test
  void chooseOriginResetsChoicesAndTraitsButKeepsAllocation() {
    Character character = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 1, 0,
        false, EnumSet.allOf(Book.class), Map.of(Attribute.AGILITY, 2), Map.of(),
        List.of(new TraitSelection(CatalogRef.of("small-frame"), false, List.of())), List.of(),
        Progression.NONE, Vitals.FULL, null, null);
    character.chooseOrigin(CatalogRef.of("protectron"));
    character.setPurpose("  Medical Responder ");
    assertEquals("Medical Responder", character.getOriginChoices().purpose());

    character.chooseOrigin(CatalogRef.of("vault-dweller"));
    assertEquals(OriginChoices.NONE, character.getOriginChoices());
    assertTrue(character.getTraits().isEmpty());
    assertEquals(7, character.attributeValue(Attribute.AGILITY));
  }

  @Test
  void completeCharactersCannotChangeTheirOrigin() {
    Character complete = create(Map.of(), Map.of(), baseline(6));
    assertThrows(IllegalStateException.class, () -> complete.chooseOrigin(CatalogRef.of("ghoul")));
  }

  /** CRB p. 56: Gifted raises two chosen attributes by 1; an imported Gifted adds nothing. */
  @Test
  void giftedRaisesTwoAttributes() {
    Character survivor = Character.createNew();
    survivor.chooseOrigin(CatalogRef.of("survivor"));
    survivor.chooseTrait(CatalogRef.of("gifted"));
    survivor.setTraitAttributeChoices("gifted", List.of(Attribute.STRENGTH, Attribute.AGILITY));
    assertEquals(6, survivor.attributeValue(Attribute.STRENGTH));
    assertEquals(6, survivor.attributeValue(Attribute.AGILITY));
    assertEquals(5, survivor.attributeValue(Attribute.LUCK));

    // Without a baseline, so only the imported flag keeps the choice from counting.
    Character imported = new Character("Imported", CatalogRef.of("survivor"), OriginChoices.NONE, 1, 0,
        true, EnumSet.allOf(Book.class), Map.of(), Map.of(),
        List.of(new TraitSelection(CatalogRef.of("gifted"), true, List.of(Attribute.STRENGTH))),
        List.of(), Progression.NONE, Vitals.FULL, null, null);
    assertEquals(5, imported.attributeValue(Attribute.STRENGTH));
  }

  /** Choices of traits without attribute choices have no effect. */
  @Test
  void onlyTraitsWithAttributeChoicesRaiseAttributes() {
    Character survivor = Character.createNew();
    survivor.chooseOrigin(CatalogRef.of("survivor"));
    survivor.chooseTrait(CatalogRef.of("educated"));
    survivor.setTraitAttributeChoices("educated", List.of(Attribute.STRENGTH));
    assertEquals(5, survivor.attributeValue(Attribute.STRENGTH));
  }

  @Test
  void traitMutatorsWorkOnlyDuringCreation() {
    Character survivor = Character.createNew();
    survivor.chooseOrigin(CatalogRef.of("survivor"));
    survivor.chooseTrait(CatalogRef.of("small-frame"));
    survivor.chooseTrait(CatalogRef.of("small-frame"));
    survivor.chooseTrait(CatalogRef.of("gifted"));
    assertEquals(List.of("small-frame", "gifted"),
        survivor.getTraits().stream().map(trait -> trait.trait().id()).toList());
    survivor.removeTrait("small-frame");
    assertEquals(1, survivor.getTraits().size());

    Character complete = create(Map.of(), Map.of(), baseline(6));
    assertThrows(IllegalStateException.class, () -> complete.chooseTrait(CatalogRef.of("gifted")));
    assertThrows(IllegalStateException.class, () -> complete.removeTrait("gifted"));
    assertThrows(IllegalStateException.class,
        () -> complete.setTraitAttributeChoices("gifted", List.of(Attribute.STRENGTH)));
  }

  @Test
  void creationMutatorsWorkOnlyDuringCreation() {
    Character character = Character.createNew();
    character.setAttributeAllocation(Attribute.LUCK, -1);
    character.setCreationRanks(Skill.SNEAK, 2);
    character.setFreeTag(Skill.REPAIR, true);
    assertEquals(4, character.attributeValue(Attribute.LUCK));
    assertEquals(2, character.skillRank(Skill.SNEAK));
    assertEquals(2, character.skillRank(Skill.REPAIR));

    Character complete = create(Map.of(), Map.of(), baseline(6));
    assertThrows(IllegalStateException.class, () -> complete.setAttributeAllocation(Attribute.LUCK, 1));
    assertThrows(IllegalStateException.class, () -> complete.setCreationRanks(Skill.SNEAK, 1));
    assertThrows(IllegalStateException.class, () -> complete.setFreeTag(Skill.SNEAK, true));
  }

  /** User decision: tagging lowers bought ranks to the creation cap (CRB p. 58: 3 at level 1). */
  @Test
  void tagTrimsBoughtRanksToTheCap() {
    Character character = Character.createNew();
    character.setCreationRanks(Skill.SNEAK, 3);
    character.setFreeTag(Skill.SNEAK, true);
    assertEquals(1, character.skillState(Skill.SNEAK).creationRanks());
    assertEquals(3, character.skillRank(Skill.SNEAK));
  }

  /** User decision: untagging only removes the two free ranks. */
  @Test
  void untagKeepsBoughtRanks() {
    Character character = Character.createNew();
    character.setFreeTag(Skill.SNEAK, true);
    character.setCreationRanks(Skill.SNEAK, 1);
    character.setFreeTag(Skill.SNEAK, false);
    assertEquals(1, character.skillRank(Skill.SNEAK));
  }

  @Test
  void originTagsCannotBeFreeTags() {
    Character ghoul = Character.createNew();
    ghoul.chooseOrigin(CatalogRef.of("ghoul"));
    assertTrue(ghoul.isTaggedByOrigin(Skill.SURVIVAL));
    assertThrows(IllegalArgumentException.class, () -> ghoul.setFreeTag(Skill.SURVIVAL, true));
    ghoul.setFreeTag(Skill.SURVIVAL, false);
  }

  /** Choosing the origin's extra Tag on a freely tagged skill frees that Tag; bought ranks stay. */
  @Test
  void chooseOriginTagSkillClearsAFreeTag() {
    Character initiate = Character.createNew();
    initiate.chooseOrigin(CatalogRef.of("brotherhood-initiate"));
    initiate.setFreeTag(Skill.SCIENCE, true);
    initiate.setCreationRanks(Skill.SCIENCE, 1);
    initiate.chooseOriginTagSkill(Skill.SCIENCE);
    assertFalse(initiate.skillState(Skill.SCIENCE).tagged());
    assertTrue(initiate.isTagged(Skill.SCIENCE));
    assertEquals(3, initiate.skillRank(Skill.SCIENCE));
  }

  /** User decision: choosing Nomad (WG p. 15) removes a free Tag on Science; bought ranks stay. */
  @Test
  void nomadRemovesAFreeScienceTag() {
    Character tribal = Character.createNew();
    tribal.chooseOrigin(CatalogRef.of("tribal"));
    tribal.setFreeTag(Skill.SCIENCE, true);
    tribal.setCreationRanks(Skill.SCIENCE, 1);
    tribal.chooseTrait(CatalogRef.of("nomad"));
    assertFalse(tribal.isTagged(Skill.SCIENCE));
    assertEquals(1, tribal.skillRank(Skill.SCIENCE));
    assertThrows(IllegalArgumentException.class, () -> tribal.setFreeTag(Skill.SCIENCE, true));
  }

  /** User decision: an origin Tag lowers bought ranks to the cap like a free Tag (CRB p. 52, 58). */
  @Test
  void originTagsTrimBoughtRanks() {
    Character ghoul = Character.createNew();
    ghoul.setCreationRanks(Skill.SURVIVAL, 3);
    ghoul.chooseOrigin(CatalogRef.of("ghoul"));
    assertEquals(3, ghoul.skillRank(Skill.SURVIVAL));
    assertEquals(1, ghoul.skillState(Skill.SURVIVAL).creationRanks());

    Character initiate = Character.createNew();
    initiate.chooseOrigin(CatalogRef.of("brotherhood-initiate"));
    initiate.setCreationRanks(Skill.SCIENCE, 3);
    initiate.chooseOriginTagSkill(Skill.SCIENCE);
    assertEquals(3, initiate.skillRank(Skill.SCIENCE));
  }

  /** User decision: a new origin's lower maximum (Super Mutant INT 6, CRB p. 53) returns the excess points. */
  @Test
  void superMutantOriginReturnsExcessAttributePoints() {
    Character character = Character.createNew();
    character.setAttributeAllocation(Attribute.INTELLIGENCE, 3);
    character.setAttributeAllocation(Attribute.STRENGTH, 2);
    character.chooseOrigin(CatalogRef.of("super-mutant"));
    assertEquals(6, character.attributeValue(Attribute.INTELLIGENCE));
    assertEquals(1, character.attributeAllocation(Attribute.INTELLIGENCE));
    assertEquals(9, character.attributeValue(Attribute.STRENGTH));
  }

  /** CRB p. 53: switching to Super Mutant lowers bought ranks to its cap of 4. */
  @Test
  void superMutantOriginTrimsRanksToItsCap() {
    Character character = atLevel(3, "survivor");
    character.setCreationRanks(Skill.SNEAK, 6);
    character.chooseOrigin(CatalogRef.of("super-mutant"));
    assertEquals(4, character.skillRank(Skill.SNEAK));
  }

  /** CRB p. 66: Intense Training raises the chosen attribute; imported ranks are in the baseline. */
  @Test
  void intenseTrainingRaisesAttribute() {
    Character character = atLevel(4, "survivor");
    character.addPerkRank(CatalogRef.of("intense-training"), Attribute.AGILITY, null);
    character.addPerkRank(CatalogRef.of("intense-training"), Attribute.AGILITY, null);
    assertEquals(7, character.attributeValue(Attribute.AGILITY));
    character.removePerkRank("intense-training");
    assertEquals(6, character.attributeValue(Attribute.AGILITY));
    assertEquals(List.of(Attribute.AGILITY), character.getPerks().getFirst().attributeChoices());

    Character imported = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 4, 0, true,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(),
        List.of(new PerkSelection(CatalogRef.of("intense-training"), 1, 1, List.of(Attribute.AGILITY), List.of())),
        Progression.NONE, Vitals.FULL, baseline(6), null);
    assertEquals(6, imported.attributeValue(Attribute.AGILITY));
  }

  /** User decision: Tag! is its own Tag source; a free Tag on the same skill is freed. */
  @Test
  void tagPerkIsSeparateTagSource() {
    Character character = atLevel(5, "survivor");
    character.setFreeTag(Skill.SNEAK, true);
    character.addPerkRank(CatalogRef.of("tag"), null, Skill.SNEAK);
    assertTrue(character.isTaggedByPerk(Skill.SNEAK));
    assertFalse(character.skillState(Skill.SNEAK).tagged());
    assertTrue(character.isTagged(Skill.SNEAK));
    assertThrows(IllegalArgumentException.class, () -> character.setFreeTag(Skill.SNEAK, true));
    character.removePerkRank("tag");
    assertFalse(character.isTagged(Skill.SNEAK));
  }

  /** CRB p. 73: Tag! adds 2 ranks to at most 6; a rank-5 skill ends at 6 (user decision). */
  @Test
  void tagPerkOnRankFiveSkillCapsAtSix() {
    Character character = atLevel(5, "survivor");
    character.setCreationRanks(Skill.SNEAK, 5);
    character.addPerkRank(CatalogRef.of("tag"), null, Skill.SNEAK);
    assertEquals(6, character.skillRank(Skill.SNEAK));
  }

  @Test
  void addAndRemovePerkRanksOnlyInCreation() {
    Character character = Character.createNew();
    character.addPerkRank(CatalogRef.of("jury-rigging"), null, null);
    assertEquals(1, character.getPerks().getFirst().rank());
    character.removePerkRank("jury-rigging");
    assertTrue(character.getPerks().isEmpty());
    character.removePerkRank("jury-rigging");

    Character withImported = new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 1, 0, false,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(),
        List.of(new PerkSelection(CatalogRef.of("jury-rigging"), 1, 1, List.of(), List.of())),
        Progression.NONE, Vitals.FULL, null, null);
    withImported.removePerkRank("jury-rigging");
    assertEquals(1, withImported.getPerks().getFirst().rank());

    Character complete = create(Map.of(), Map.of(), baseline(6));
    complete.addPerkRank(CatalogRef.of("jury-rigging"), null, null);
    assertEquals(1, complete.getPerks().getFirst().stagedRanks(), "after creation a perk rank is staged");
    complete.removePerkRank("jury-rigging");
    assertTrue(complete.getPerks().isEmpty());
  }

  /** CRB p. 49: XP becomes the minimum of the starting level (user decision); no upper limit. */
  @Test
  void setStartingLevelSetsXp() {
    Character character = Character.createNew();
    character.setStartingLevel(5);
    assertEquals(5, character.getLevel());
    assertEquals(1_000, character.getXp());
    character.setStartingLevel(21);
    assertEquals(21_000, character.getXp());
    character.setStartingLevel(25);
    assertEquals(30_000, character.getXp());
  }

  /** User decision: lowering the level trims skill ranks above the new cap (CRB p. 58). */
  @Test
  void loweringTheLevelTrimsSkillRanks() {
    Character character = Character.createNew();
    character.setStartingLevel(3);
    character.setCreationRanks(Skill.SNEAK, 6);
    character.setStartingLevel(2);
    assertEquals(3, character.skillRank(Skill.SNEAK));
  }

  /** Raising the level changes nothing else: extra skill points and perks only show as "left". */
  @Test
  void raisingTheLevelKeepsSkillsAndPerks() {
    Character character = Character.createNew();
    character.setCreationRanks(Skill.SNEAK, 3);
    character.addPerkRank(CatalogRef.of("jury-rigging"), null, null);
    character.setStartingLevel(6);
    assertEquals(3, character.skillRank(Skill.SNEAK));
    assertEquals(1, character.getPerks().size());
    assertEquals(1, character.getPerks().getFirst().rank());
  }

  @Test
  void startingLevelOnlyDuringCreationAndAtLeastOne() {
    assertThrows(IllegalArgumentException.class, () -> Character.createNew().setStartingLevel(0));
    Character huge = Character.createNew();
    assertThrows(IllegalArgumentException.class, () -> huge.setStartingLevel(Integer.MAX_VALUE));
    assertEquals(1, huge.getLevel());
    assertEquals(0, huge.getXp());
    Character complete = create(Map.of(), Map.of(), baseline(6));
    assertThrows(IllegalStateException.class, () -> complete.setStartingLevel(2));
  }

  /** CRB p. 50: creation ends once; afterwards creation choices are fixed. */
  @Test
  void completeCreationEndsCreation() {
    Character character = Character.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.setStartingLevel(3);
    assertEquals(OptionalInt.of(3), character.getStartingLevel(), "during creation the current level");
    character.completeCreation();
    assertTrue(character.isCreationComplete());
    assertEquals(OptionalInt.of(3), character.getStartingLevel());
    assertThrows(IllegalStateException.class, character::completeCreation);
    assertThrows(IllegalStateException.class, () -> character.chooseOrigin(CatalogRef.of("ghoul")));
    assertThrows(IllegalStateException.class, () -> character.setFreeTag(Skill.SNEAK, true));
    assertThrows(IllegalStateException.class, () -> character.chooseEquipmentPack(CatalogRef.of("settler")));
    assertThrows(IllegalStateException.class, () -> character.setStartingLevel(4));
  }

  /** CRB p. 58: the creation rank cap of 3 no longer applies after creation. */
  @Test
  void completionLiftsTheCreationRankCap() {
    Character character = Character.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    assertEquals(3, character.skillRankCap());
    character.completeCreation();
    assertEquals(6, character.skillRankCap());
  }

  /** Imported characters are complete with an unknown starting level. */
  @Test
  void importedCharactersHaveNoKnownStartingLevel() {
    assertEquals(OptionalInt.empty(), create(Map.of(), Map.of(), baseline(6)).getStartingLevel());
  }

  @Test
  void storedStartingLevelMustFitTheCharacter() {
    assertEquals(OptionalInt.of(2), withStartingLevel(true, 2).getStartingLevel());
    assertThrows(IllegalArgumentException.class, () -> withStartingLevel(true, 3), "above the level");
    assertThrows(IllegalArgumentException.class, () -> withStartingLevel(true, 0), "below 1");
    assertThrows(IllegalArgumentException.class, () -> withStartingLevel(false, 2), "only complete characters");
  }

  private static Character withStartingLevel(boolean complete, int startingLevel) {
    return new Character("Test", null, OriginChoices.NONE, 2, 100, complete, EnumSet.allOf(Book.class), Map.of(),
        Map.of(), List.of(), List.of(), Progression.NONE, Vitals.FULL, null, null, StartingEquipment.NONE, "",
        startingLevel, null, List.of(), Map.of(), List.of(), Inventory.EMPTY, null);
  }

  /** CRB p. 58: 3 during creation below level 3, else 6; CRB p. 53: Super Mutant at most 4. */
  @Test
  void skillRankCap() {
    assertEquals(3, Character.createNew().skillRankCap());
    assertEquals(6, atLevel(3, "survivor").skillRankCap());
    assertEquals(4, atLevel(3, "super-mutant").skillRankCap());
    Character mutant = Character.createNew();
    mutant.chooseOrigin(CatalogRef.of("super-mutant"));
    assertEquals(3, mutant.skillRankCap());
    assertEquals(6, create(Map.of(), Map.of(), baseline(6)).skillRankCap());
  }

  private static Character atLevel(int level, String origin) {
    return new Character("Test", CatalogRef.of(origin), OriginChoices.NONE, level, 0, false,
        EnumSet.allOf(Book.class), Map.of(), Map.of(), List.of(), List.of(), Progression.NONE,
        Vitals.FULL, null, null);
  }

  private static LegacyBaseline baseline(int value) {
    Map<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
    for (Attribute attribute : Attribute.values()) {
      attributes.put(attribute, value);
    }
    Map<Skill, Integer> ranks = new EnumMap<>(Skill.class);
    for (Skill skill : Skill.values()) {
      ranks.put(skill, 0);
    }
    return new LegacyBaseline(attributes, ranks);
  }

  private static Character create(Map<Attribute, Integer> allocation, Map<Skill, SkillState> skills,
      LegacyBaseline baseline) {
    return new Character("Test", CatalogRef.of("survivor"), OriginChoices.NONE, 1, 0, baseline != null,
        EnumSet.allOf(Book.class), allocation, skills,
        List.of(), List.of(), Progression.NONE, Vitals.FULL, baseline, null);
  }
}
