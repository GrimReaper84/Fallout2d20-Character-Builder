package de.fallout2d20.charbuilder.model.party;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.derived.CarryLoad;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import de.fallout2d20.charbuilder.persistence.CharacterStore;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PartyMemberTest {

  private static final PartyMember NORA = new PartyMember("Nora", "Anna", 3, 9, 12, 0, 13, "");

  @Test
  void numbersAreNeverNegativeAndHpStayBelowTheRadLoweredMaximum() {
    PartyMember member = new PartyMember("X", "", -1, 20, 12, 3, -2, "");
    assertEquals(0, member.level());
    assertEquals(0, member.initiative());
    assertEquals(9, member.currentHp());
    assertEquals(0, NORA.withCurrentHp(-5).currentHp());
  }

  @Test
  void radsLowerCurrentHpButClearingThemDoesNotHeal() {
    PartyMember irradiated = NORA.withRads(5);
    assertEquals(7, irradiated.currentHp());
    assertEquals(7, irradiated.effectiveMaxHp());
    assertEquals(7, irradiated.withRads(0).currentHp());
    assertEquals(12, irradiated.withRads(0).healed().currentHp());
  }

  @Test
  void aMemberAtFullHpStaysFullWhenTheMaximumChanges() {
    PartyMember full = NORA.healed();
    assertEquals(14, full.withMaxHp(14).currentHp());
    assertEquals(9, NORA.withMaxHp(14).currentHp());
    assertEquals(10, full.withMaxHp(10).currentHp());
  }

  @Test
  void takesTheValuesOfACharacterOnce() throws Exception {
    PlayerCharacter character = new CharacterStore().load(Path.of("src/test/resources/samples/mister-handy.json"))
        .character();
    HitLocation injured = character.hitLocations().getLast();
    character.session().setInjured(injured, true);
    character.session().setRads(2);

    PartyMember member = PartyMember.of(character, "Ben");

    DerivedStats derived = DerivedStats.of(character);
    CarryLoad load = CarryLoad.of(character);
    assertEquals(character.getName(), member.name());
    assertEquals("Ben", member.player());
    assertEquals(character.advancement().getLevel(), member.level());
    assertEquals(derived.maxHp(), member.maxHp());
    assertEquals(2, member.rads());
    assertEquals(derived.maxHp() - 2, member.currentHp());
    assertEquals(load.initiative(derived.initiative()), member.initiative());
    assertEquals(injured.displayName(), member.injuries());
  }
}
