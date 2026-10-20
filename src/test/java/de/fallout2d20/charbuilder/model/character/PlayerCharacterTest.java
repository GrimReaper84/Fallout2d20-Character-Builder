package de.fallout2d20.charbuilder.model.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class PlayerCharacterTest {

  @Test
  void newCharacterStartsWithBaseValues() {
    PlayerCharacter character = PlayerCharacter.createNew();
    for (Attribute attribute : Attribute.values()) {
      assertEquals(5, character.abilities().attributeValue(attribute));
    }
    for (Skill skill : Skill.values()) {
      assertEquals(0, character.abilities().skillRank(skill));
    }
    assertFalse(character.isLegacyImport());
    assertFalse(character.isCreationComplete());
  }

  /** The Core Rulebook can never be switched off (user decision). */
  @Test
  void coreRulebookIsAlwaysEnabled() {
    assertEquals(EnumSet.allOf(Book.class), PlayerCharacter.createNew().getEnabledBooks());
    PlayerCharacter character = PlayerCharacter.builder().name("Test").enabledBooks(EnumSet.of(Book.WOA)).build();
    assertEquals(EnumSet.of(Book.CRB, Book.WOA), character.getEnabledBooks());
  }

  /** CRB p. 58: base 5 plus spent points; an attribute may be lowered to 4. */
  @Test
  void attributeIsBasePlusAllocation() {
    PlayerCharacter character = create(Map.of(Attribute.STRENGTH, 2, Attribute.CHARISMA, -1), Map.of(), null);
    assertEquals(7, character.abilities().attributeValue(Attribute.STRENGTH));
    assertEquals(4, character.abilities().attributeValue(Attribute.CHARISMA));
  }

  /** CRB p. 58: a Tag skill starts at rank 2. */
  @Test
  void tagSkillAddsTwoRanks() {
    PlayerCharacter character = create(Map.of(),
        Map.of(Skill.REPAIR, new SkillState(true, 1, 0), Skill.SNEAK, new SkillState(false, 2, 1)), null);
    assertEquals(3, character.abilities().skillRank(Skill.REPAIR));
    assertEquals(3, character.abilities().skillRank(Skill.SNEAK));
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
    PlayerCharacter character = create(Map.of(),
        Map.of(Skill.LOCKPICK, new SkillState(true, 0, 1)), new LegacyBaseline(attributes, ranks));

    assertTrue(character.isLegacyImport());
    assertEquals(4, character.abilities().attributeValue(Attribute.STRENGTH));
    assertEquals(4, character.abilities().skillRank(Skill.LOCKPICK));
    assertTrue(character.abilities().skillState(Skill.LOCKPICK).tagged());
  }

  /** CRB p. 53: a new Super Mutant starts with STR and END 7; an imported one keeps its baseline. */
  @Test
  void superMutantGetsAttributeModifiers() {
    PlayerCharacter mutant = PlayerCharacter.createNew();
    mutant.chooseOrigin(CatalogRef.of("super-mutant"));
    assertEquals(7, mutant.abilities().attributeValue(Attribute.STRENGTH));
    assertEquals(7, mutant.abilities().attributeValue(Attribute.ENDURANCE));
    assertEquals(5, mutant.abilities().attributeValue(Attribute.INTELLIGENCE));

    PlayerCharacter imported = create(Map.of(), Map.of(), baseline(8));
    assertEquals(8, PlayerCharacter.builder().name("Imported").origin(CatalogRef.of("super-mutant"))
        .creationComplete(true).legacyBaseline(imported.getLegacyBaseline()).build()
        .abilities().attributeValue(Attribute.STRENGTH));
  }

  /** CRB p. 52 (user reading): Survival is tagged in addition; a free Survival Tag is removed. */
  @Test
  void ghoulSurvivalIsAnAdditionalTag() {
    PlayerCharacter ghoul = create(Map.of(), Map.of(Skill.SURVIVAL, new SkillState(true, 1, 0)), null);
    ghoul.chooseOrigin(CatalogRef.of("ghoul"));
    assertFalse(ghoul.abilities().skillState(Skill.SURVIVAL).tagged());
    assertTrue(ghoul.abilities().isTagged(Skill.SURVIVAL));
    assertEquals(3, ghoul.abilities().skillRank(Skill.SURVIVAL));
  }

  /** CRB p. 51: the extra Tag skill of the Brotherhood counts as a Tag skill. */
  @Test
  void originTagSkillCountsAsTag() {
    PlayerCharacter initiate = PlayerCharacter.createNew();
    initiate.chooseOrigin(CatalogRef.of("brotherhood-initiate"));
    initiate.chooseOriginTagSkill(Skill.SCIENCE);
    assertTrue(initiate.abilities().isTagged(Skill.SCIENCE));
    assertEquals(2, initiate.abilities().skillRank(Skill.SCIENCE));
    assertFalse(initiate.abilities().isTagged(Skill.REPAIR));
  }

  @Test
  void chooseOriginResetsChoicesAndTraitsButKeepsAllocation() {
    PlayerCharacter character = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor"))
        .attributeAllocation(Map.of(Attribute.AGILITY, 2))
        .traits(List.of(new TraitSelection(CatalogRef.of("small-frame"), false, List.of()))).build();
    character.chooseOrigin(CatalogRef.of("protectron"));
    character.setPurpose("  Medical Responder ");
    assertEquals("Medical Responder", character.getOriginChoices().purpose());

    character.chooseOrigin(CatalogRef.of("vault-dweller"));
    assertEquals(OriginChoices.NONE, character.getOriginChoices());
    assertTrue(character.getTraits().isEmpty());
    assertEquals(7, character.abilities().attributeValue(Attribute.AGILITY));
  }

  @Test
  void completeCharactersCannotChangeTheirOrigin() {
    PlayerCharacter complete = create(Map.of(), Map.of(), baseline(6));
    CatalogRef ghoul = CatalogRef.of("ghoul");
    assertThrows(IllegalStateException.class, () -> complete.chooseOrigin(ghoul));
  }

  /** CRB p. 56: Gifted raises two chosen attributes by 1; an imported Gifted adds nothing. */
  @Test
  void giftedRaisesTwoAttributes() {
    PlayerCharacter survivor = PlayerCharacter.createNew();
    survivor.chooseOrigin(CatalogRef.of("survivor"));
    survivor.chooseTrait(CatalogRef.of("gifted"));
    survivor.setTraitAttributeChoices("gifted", List.of(Attribute.STRENGTH, Attribute.AGILITY));
    assertEquals(6, survivor.abilities().attributeValue(Attribute.STRENGTH));
    assertEquals(6, survivor.abilities().attributeValue(Attribute.AGILITY));
    assertEquals(5, survivor.abilities().attributeValue(Attribute.LUCK));

    // Without a baseline, so only the imported flag keeps the choice from counting.
    PlayerCharacter imported = PlayerCharacter.builder().name("Imported").origin(CatalogRef.of("survivor"))
        .creationComplete(true)
        .traits(List.of(new TraitSelection(CatalogRef.of("gifted"), true, List.of(Attribute.STRENGTH)))).build();
    assertEquals(5, imported.abilities().attributeValue(Attribute.STRENGTH));
  }

  /** Choices of traits without attribute choices have no effect. */
  @Test
  void onlyTraitsWithAttributeChoicesRaiseAttributes() {
    PlayerCharacter survivor = PlayerCharacter.createNew();
    survivor.chooseOrigin(CatalogRef.of("survivor"));
    survivor.chooseTrait(CatalogRef.of("educated"));
    survivor.setTraitAttributeChoices("educated", List.of(Attribute.STRENGTH));
    assertEquals(5, survivor.abilities().attributeValue(Attribute.STRENGTH));
  }

  @Test
  void traitMutatorsWorkOnlyDuringCreation() {
    PlayerCharacter survivor = PlayerCharacter.createNew();
    survivor.chooseOrigin(CatalogRef.of("survivor"));
    survivor.chooseTrait(CatalogRef.of("small-frame"));
    survivor.chooseTrait(CatalogRef.of("small-frame"));
    survivor.chooseTrait(CatalogRef.of("gifted"));
    assertEquals(List.of("small-frame", "gifted"),
        survivor.getTraits().stream().map(trait -> trait.trait().id()).toList());
    survivor.removeTrait("small-frame");
    assertEquals(1, survivor.getTraits().size());

    PlayerCharacter complete = create(Map.of(), Map.of(), baseline(6));
    CatalogRef gifted = CatalogRef.of("gifted");
    assertThrows(IllegalStateException.class, () -> complete.chooseTrait(gifted));
    assertThrows(IllegalStateException.class, () -> complete.removeTrait("gifted"));
    List<Attribute> strength = List.of(Attribute.STRENGTH);
    assertThrows(IllegalStateException.class, () -> complete.setTraitAttributeChoices("gifted", strength));
  }

  @Test
  void creationMutatorsWorkOnlyDuringCreation() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.abilities().setAttributeAllocation(Attribute.LUCK, -1);
    character.abilities().setCreationRanks(Skill.SNEAK, 2);
    character.abilities().setFreeTag(Skill.REPAIR, true);
    assertEquals(4, character.abilities().attributeValue(Attribute.LUCK));
    assertEquals(2, character.abilities().skillRank(Skill.SNEAK));
    assertEquals(2, character.abilities().skillRank(Skill.REPAIR));

    PlayerCharacter complete = create(Map.of(), Map.of(), baseline(6));
    Abilities completeAbilities = complete.abilities();
    assertThrows(IllegalStateException.class, () -> completeAbilities.setAttributeAllocation(Attribute.LUCK, 1));
    assertThrows(IllegalStateException.class, () -> completeAbilities.setCreationRanks(Skill.SNEAK, 1));
    assertThrows(IllegalStateException.class, () -> completeAbilities.setFreeTag(Skill.SNEAK, true));
  }

  /** User decision: tagging lowers bought ranks to the creation cap (CRB p. 58: 3 at level 1). */
  @Test
  void tagTrimsBoughtRanksToTheCap() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.abilities().setCreationRanks(Skill.SNEAK, 3);
    character.abilities().setFreeTag(Skill.SNEAK, true);
    assertEquals(1, character.abilities().skillState(Skill.SNEAK).creationRanks());
    assertEquals(3, character.abilities().skillRank(Skill.SNEAK));
  }

  /** User decision: untagging only removes the two free ranks. */
  @Test
  void untagKeepsBoughtRanks() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.abilities().setFreeTag(Skill.SNEAK, true);
    character.abilities().setCreationRanks(Skill.SNEAK, 1);
    character.abilities().setFreeTag(Skill.SNEAK, false);
    assertEquals(1, character.abilities().skillRank(Skill.SNEAK));
  }

  @Test
  void originTagsCannotBeFreeTags() {
    PlayerCharacter ghoul = PlayerCharacter.createNew();
    ghoul.chooseOrigin(CatalogRef.of("ghoul"));
    assertTrue(ghoul.abilities().isTaggedByOrigin(Skill.SURVIVAL));
    Abilities ghoulAbilities = ghoul.abilities();
    assertThrows(IllegalArgumentException.class, () -> ghoulAbilities.setFreeTag(Skill.SURVIVAL, true));
    ghoul.abilities().setFreeTag(Skill.SURVIVAL, false);
  }

  /** Choosing the origin's extra Tag on a freely tagged skill frees that Tag; bought ranks stay. */
  @Test
  void chooseOriginTagSkillClearsAFreeTag() {
    PlayerCharacter initiate = PlayerCharacter.createNew();
    initiate.chooseOrigin(CatalogRef.of("brotherhood-initiate"));
    initiate.abilities().setFreeTag(Skill.SCIENCE, true);
    initiate.abilities().setCreationRanks(Skill.SCIENCE, 1);
    initiate.chooseOriginTagSkill(Skill.SCIENCE);
    assertFalse(initiate.abilities().skillState(Skill.SCIENCE).tagged());
    assertTrue(initiate.abilities().isTagged(Skill.SCIENCE));
    assertEquals(3, initiate.abilities().skillRank(Skill.SCIENCE));
  }

  /** User decision: choosing Nomad (WG p. 15) removes a free Tag on Science; bought ranks stay. */
  @Test
  void nomadRemovesAFreeScienceTag() {
    PlayerCharacter tribal = PlayerCharacter.createNew();
    tribal.chooseOrigin(CatalogRef.of("tribal"));
    tribal.abilities().setFreeTag(Skill.SCIENCE, true);
    tribal.abilities().setCreationRanks(Skill.SCIENCE, 1);
    tribal.chooseTrait(CatalogRef.of("nomad"));
    assertFalse(tribal.abilities().isTagged(Skill.SCIENCE));
    assertEquals(1, tribal.abilities().skillRank(Skill.SCIENCE));
    Abilities tribalAbilities = tribal.abilities();
    assertThrows(IllegalArgumentException.class, () -> tribalAbilities.setFreeTag(Skill.SCIENCE, true));
  }

  /** User decision: an origin Tag lowers bought ranks to the cap like a free Tag (CRB p. 52, 58). */
  @Test
  void originTagsTrimBoughtRanks() {
    PlayerCharacter ghoul = PlayerCharacter.createNew();
    ghoul.abilities().setCreationRanks(Skill.SURVIVAL, 3);
    ghoul.chooseOrigin(CatalogRef.of("ghoul"));
    assertEquals(3, ghoul.abilities().skillRank(Skill.SURVIVAL));
    assertEquals(1, ghoul.abilities().skillState(Skill.SURVIVAL).creationRanks());

    PlayerCharacter initiate = PlayerCharacter.createNew();
    initiate.chooseOrigin(CatalogRef.of("brotherhood-initiate"));
    initiate.abilities().setCreationRanks(Skill.SCIENCE, 3);
    initiate.chooseOriginTagSkill(Skill.SCIENCE);
    assertEquals(3, initiate.abilities().skillRank(Skill.SCIENCE));
  }

  /** User decision: a new origin's lower maximum (Super Mutant INT 6, CRB p. 53) returns the excess points. */
  @Test
  void superMutantOriginReturnsExcessAttributePoints() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.abilities().setAttributeAllocation(Attribute.INTELLIGENCE, 3);
    character.abilities().setAttributeAllocation(Attribute.STRENGTH, 2);
    character.chooseOrigin(CatalogRef.of("super-mutant"));
    assertEquals(6, character.abilities().attributeValue(Attribute.INTELLIGENCE));
    assertEquals(1, character.abilities().attributeAllocation(Attribute.INTELLIGENCE));
    assertEquals(9, character.abilities().attributeValue(Attribute.STRENGTH));
  }

  /** CRB p. 53: switching to Super Mutant lowers bought ranks to its cap of 4. */
  @Test
  void superMutantOriginTrimsRanksToItsCap() {
    PlayerCharacter character = atLevel(3, "survivor");
    character.abilities().setCreationRanks(Skill.SNEAK, 6);
    character.chooseOrigin(CatalogRef.of("super-mutant"));
    assertEquals(4, character.abilities().skillRank(Skill.SNEAK));
  }

  /** CRB p. 66: Intense Training raises the chosen attribute; imported ranks are in the baseline. */
  @Test
  void intenseTrainingRaisesAttribute() {
    PlayerCharacter character = atLevel(4, "survivor");
    character.addPerkRank(CatalogRef.of("intense-training"), Attribute.AGILITY, null);
    character.addPerkRank(CatalogRef.of("intense-training"), Attribute.AGILITY, null);
    assertEquals(7, character.abilities().attributeValue(Attribute.AGILITY));
    character.removePerkRank("intense-training");
    assertEquals(6, character.abilities().attributeValue(Attribute.AGILITY));
    assertEquals(List.of(Attribute.AGILITY), character.getPerks().getFirst().attributeChoices());

    PlayerCharacter imported = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor")).level(4)
        .creationComplete(true)
        .perks(List.of(new PerkSelection(CatalogRef.of("intense-training"), 1, 1, List.of(Attribute.AGILITY),
            List.of())))
        .legacyBaseline(baseline(6)).build();
    assertEquals(6, imported.abilities().attributeValue(Attribute.AGILITY));
  }

  /** User decision: Tag! is its own Tag source; a free Tag on the same skill is freed. */
  @Test
  void tagPerkIsSeparateTagSource() {
    PlayerCharacter character = atLevel(5, "survivor");
    character.abilities().setFreeTag(Skill.SNEAK, true);
    character.addPerkRank(CatalogRef.of("tag"), null, Skill.SNEAK);
    assertTrue(character.abilities().isTaggedByPerk(Skill.SNEAK));
    assertFalse(character.abilities().skillState(Skill.SNEAK).tagged());
    assertTrue(character.abilities().isTagged(Skill.SNEAK));
    Abilities abilities = character.abilities();
    assertThrows(IllegalArgumentException.class, () -> abilities.setFreeTag(Skill.SNEAK, true));
    character.removePerkRank("tag");
    assertFalse(character.abilities().isTagged(Skill.SNEAK));
  }

  /** CRB p. 73: Tag! adds 2 ranks to at most 6; a rank-5 skill ends at 6 (user decision). */
  @Test
  void tagPerkOnRankFiveSkillCapsAtSix() {
    PlayerCharacter character = atLevel(5, "survivor");
    character.abilities().setCreationRanks(Skill.SNEAK, 5);
    character.addPerkRank(CatalogRef.of("tag"), null, Skill.SNEAK);
    assertEquals(6, character.abilities().skillRank(Skill.SNEAK));
  }

  @Test
  void addAndRemovePerkRanksOnlyInCreation() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.addPerkRank(CatalogRef.of("jury-rigging"), null, null);
    assertEquals(1, character.getPerks().getFirst().rank());
    character.removePerkRank("jury-rigging");
    assertTrue(character.getPerks().isEmpty());
    character.removePerkRank("jury-rigging");

    PlayerCharacter withImported = PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor"))
        .perks(List.of(new PerkSelection(CatalogRef.of("jury-rigging"), 1, 1, List.of(), List.of()))).build();
    withImported.removePerkRank("jury-rigging");
    assertEquals(1, withImported.getPerks().getFirst().rank());

    PlayerCharacter complete = create(Map.of(), Map.of(), baseline(6));
    complete.addPerkRank(CatalogRef.of("jury-rigging"), null, null);
    assertEquals(1, complete.getPerks().getFirst().stagedRanks(), "after creation a perk rank is staged");
    complete.removePerkRank("jury-rigging");
    assertTrue(complete.getPerks().isEmpty());
  }

  /** CRB p. 49: XP becomes the minimum of the starting level (user decision); no upper limit. */
  @Test
  void setStartingLevelSetsXp() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.advancement().setStartingLevel(5);
    assertEquals(5, character.advancement().getLevel());
    assertEquals(1_000, character.advancement().getXp());
    character.advancement().setStartingLevel(21);
    assertEquals(21_000, character.advancement().getXp());
    character.advancement().setStartingLevel(25);
    assertEquals(30_000, character.advancement().getXp());
  }

  /** User decision: lowering the level trims skill ranks above the new cap (CRB p. 58). */
  @Test
  void loweringTheLevelTrimsSkillRanks() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.advancement().setStartingLevel(3);
    character.abilities().setCreationRanks(Skill.SNEAK, 6);
    character.advancement().setStartingLevel(2);
    assertEquals(3, character.abilities().skillRank(Skill.SNEAK));
  }

  /** Raising the level changes nothing else: extra skill points and perks only show as "left". */
  @Test
  void raisingTheLevelKeepsSkillsAndPerks() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.abilities().setCreationRanks(Skill.SNEAK, 3);
    character.addPerkRank(CatalogRef.of("jury-rigging"), null, null);
    character.advancement().setStartingLevel(6);
    assertEquals(3, character.abilities().skillRank(Skill.SNEAK));
    assertEquals(1, character.getPerks().size());
    assertEquals(1, character.getPerks().getFirst().rank());
  }

  @Test
  void startingLevelOnlyDuringCreationAndAtLeastOne() {
    Advancement advancement = PlayerCharacter.createNew().advancement();
    assertThrows(IllegalArgumentException.class, () -> advancement.setStartingLevel(0));
    PlayerCharacter huge = PlayerCharacter.createNew();
    Advancement hugeAdvancement = huge.advancement();
    assertThrows(IllegalArgumentException.class, () -> hugeAdvancement.setStartingLevel(Integer.MAX_VALUE));
    assertEquals(1, huge.advancement().getLevel());
    assertEquals(0, huge.advancement().getXp());
    PlayerCharacter complete = create(Map.of(), Map.of(), baseline(6));
    Advancement completeAdvancement = complete.advancement();
    assertThrows(IllegalStateException.class, () -> completeAdvancement.setStartingLevel(2));
  }

  /** CRB p. 50: creation ends once; afterwards creation choices are fixed. */
  @Test
  void completeCreationEndsCreation() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    character.advancement().setStartingLevel(3);
    assertEquals(OptionalInt.of(3), character.advancement().getStartingLevel(), "during creation the current level");
    character.completeCreation();
    assertTrue(character.isCreationComplete());
    assertEquals(OptionalInt.of(3), character.advancement().getStartingLevel());
    assertThrows(IllegalStateException.class, character::completeCreation);
    CatalogRef ghoul = CatalogRef.of("ghoul");
    assertThrows(IllegalStateException.class, () -> character.chooseOrigin(ghoul));
    Abilities abilities = character.abilities();
    assertThrows(IllegalStateException.class, () -> abilities.setFreeTag(Skill.SNEAK, true));
    StartingGear startingGear = character.startingGear();
    CatalogRef settler = CatalogRef.of("settler");
    assertThrows(IllegalStateException.class, () -> startingGear.choosePack(settler));
    Advancement advancement = character.advancement();
    assertThrows(IllegalStateException.class, () -> advancement.setStartingLevel(4));
  }

  /** CRB p. 58: the creation rank cap of 3 no longer applies after creation. */
  @Test
  void completionLiftsTheCreationRankCap() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of("survivor"));
    assertEquals(3, character.abilities().skillRankCap());
    character.completeCreation();
    assertEquals(6, character.abilities().skillRankCap());
  }

  /** Imported characters are complete with an unknown starting level. */
  @Test
  void importedCharactersHaveNoKnownStartingLevel() {
    assertEquals(OptionalInt.empty(), create(Map.of(), Map.of(), baseline(6)).advancement().getStartingLevel());
  }

  @Test
  void storedStartingLevelMustFitTheCharacter() {
    assertEquals(OptionalInt.of(2), withStartingLevel(true, 2).advancement().getStartingLevel());
    assertThrows(IllegalArgumentException.class, () -> withStartingLevel(true, 3), "above the level");
    assertThrows(IllegalArgumentException.class, () -> withStartingLevel(true, 0), "below 1");
    assertThrows(IllegalArgumentException.class, () -> withStartingLevel(false, 2), "only complete characters");
  }

  private static PlayerCharacter withStartingLevel(boolean complete, int startingLevel) {
    return PlayerCharacter.builder().name("Test").level(2).xp(100).creationComplete(complete)
        .startingLevel(startingLevel).build();
  }

  /** CRB p. 58: 3 during creation below level 3, else 6; CRB p. 53: Super Mutant at most 4. */
  @Test
  void skillRankCap() {
    assertEquals(3, PlayerCharacter.createNew().abilities().skillRankCap());
    assertEquals(6, atLevel(3, "survivor").abilities().skillRankCap());
    assertEquals(4, atLevel(3, "super-mutant").abilities().skillRankCap());
    PlayerCharacter mutant = PlayerCharacter.createNew();
    mutant.chooseOrigin(CatalogRef.of("super-mutant"));
    assertEquals(3, mutant.abilities().skillRankCap());
    assertEquals(6, create(Map.of(), Map.of(), baseline(6)).abilities().skillRankCap());
  }

  private static PlayerCharacter atLevel(int level, String origin) {
    return PlayerCharacter.builder().name("Test").origin(CatalogRef.of(origin)).level(level).build();
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

  private static PlayerCharacter create(Map<Attribute, Integer> allocation, Map<Skill, SkillState> skills,
      LegacyBaseline baseline) {
    return PlayerCharacter.builder().name("Test").origin(CatalogRef.of("survivor")).creationComplete(baseline != null)
        .attributeAllocation(allocation).skills(skills).legacyBaseline(baseline).build();
  }
}
