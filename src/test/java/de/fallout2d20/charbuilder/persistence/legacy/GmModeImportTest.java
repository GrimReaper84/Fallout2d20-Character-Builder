package de.fallout2d20.charbuilder.persistence.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.creation.SkillRules;
import java.time.Clock;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** GM free edit (D2) on an imported character, whose legacy ranks are its baseline. */
class GmModeImportTest {

  /** User decision 2026-10-16: a GM change after creation counts at once, also below the legacy rank. */
  @Test
  void skillCorrectionsCountAtOnceBelowTheBaseline() throws Exception {
    PlayerCharacter imported = new LegacyCharacterImporter(Clock.systemUTC())
        .importCharacter(LegacyCharacterImporterTest.fixture("test-survivor-v2.json")).character();
    imported.setGmMode(true);
    Skill skill = Arrays.stream(Skill.values()).filter(candidate -> imported.skillRank(candidate) > 0)
        .findFirst().orElseThrow();
    int before = imported.skillRank(skill);
    SkillRules.step(imported, skill, -1);
    assertEquals(before - 1, imported.skillRank(skill));
    assertEquals(-1, imported.skillState(skill).gmRanks());
    assertEquals(0, imported.skillState(skill).stagedRanks(), "not staged");
  }

  /** An imported perk rank can be removed in GM mode; the imported rank count follows. */
  @Test
  void importedPerkRanksCanBeRemoved() throws Exception {
    PlayerCharacter imported = new LegacyCharacterImporter(Clock.systemUTC())
        .importCharacter(LegacyCharacterImporterTest.fixture("test-survivor-v2.json")).character();
    PerkSelection juryRigging = imported.getPerks().stream()
        .filter(perk -> perk.perk().id().equals("jury-rigging")).findFirst().orElseThrow();
    assertTrue(juryRigging.lastRankImported());
    imported.setGmMode(true);
    imported.removePerkRank("jury-rigging");
    assertTrue(imported.getPerks().stream().noneMatch(perk -> perk.perk().id().equals("jury-rigging")));
  }
}
