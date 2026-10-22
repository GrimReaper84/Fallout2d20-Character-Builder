package de.fallout2d20.charbuilder.model.character.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The weapons of the Assaultron's trait (WG p. 11; user decision 2026-10-22: always there, never carried). */
class InnateWeaponsTest {

  private static PlayerCharacter character(String origin) {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.chooseOrigin(CatalogRef.of(origin));
    return character;
  }

  @Test
  void assaultronHasItsTraitWeapons() {
    PlayerCharacter assaultron = character("assaultron");
    assertEquals(List.of("assaultron-claws", "assaultron-head-laser", "self-destruct"),
        InnateWeapons.of(assaultron).stream().map(WeaponData::id).toList());
    assertTrue(InnateWeapons.of(character("protectron")).isEmpty(), "Protectron weapons depend on its model");
    assertTrue(InnateWeapons.of(character("survivor")).isEmpty());
  }

  /** The trait's weapons follow the origin, never only some of them by book. */
  @Test
  void followTheOriginNotTheBooks() {
    PlayerCharacter assaultron = PlayerCharacter.builder().origin(CatalogRef.of("assaultron"))
        .enabledBooks(EnumSet.of(Book.CRB, Book.WOA)).build();
    assertEquals(3, InnateWeapons.of(assaultron).size());
  }

  /** Carried weapons' ammunition first, then that of the innate weapons. */
  @Test
  void ammunitionUsed() {
    PlayerCharacter assaultron = character("assaultron");
    assaultron.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    assertEquals(List.of("10mm", "fusion-cell"), List.copyOf(InnateWeapons.ammunitionUsed(assaultron)));
  }

  /** CRB p. 27: TN = attribute + skill; the Claws are an unarmed attack of 4 CD (trait bonus included, WG p. 11). */
  @Test
  void clawsAttackLikeUnarmedWeapons() {
    PlayerCharacter assaultron = character("assaultron");
    WeaponAttack claws = WeaponAttack.of(assaultron, WeaponCatalog.byId("assaultron-claws").orElseThrow());
    assertEquals(Skill.UNARMED, claws.skill());
    assertEquals(assaultron.abilities().attributeValue(Attribute.STRENGTH)
        + assaultron.abilities().skillRank(Skill.UNARMED), claws.targetNumber());
    assertEquals("4 CD", claws.damageText(), "no melee bonus at the starting STR");
  }
}
