package de.fallout2d20.charbuilder.model.character.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.CustomWeapon;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Gear of a switched-off book keeps its place and gets a hint (user decision 2026-11-02). */
class BookRulesTest {

  private static List<String> hints(PlayerCharacter character) {
    return BookRules.problems(character).stream().map(CreationProblem::message).toList();
  }

  @Test
  void weaponOfASwitchedOffBook() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("cryojet")));
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("10mm-pistol")));
    assertEquals(List.of(), hints(character));
    character.setBookEnabled(Book.WOA, false);
    assertEquals(List.of("Cryojet is from Winter of Atom, which is switched off for this character."),
        hints(character));
    assertEquals(HintArea.GEAR, BookRules.problems(character).getFirst().area());
    assertEquals(2, character.gear().getWeapons().size(), "nothing is removed");
    character.setBookEnabled(Book.WOA, true);
    assertEquals(List.of(), hints(character));
  }

  @Test
  void apparelOfASwitchedOffBook() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addApparel(WornApparel.of(CatalogRef.of("beer-hat")));
    character.setBookEnabled(Book.WG, false);
    List<CreationProblem> problems = BookRules.problems(character);
    assertEquals(List.of("Beer Hat is from Wanderer's Guide, which is switched off for this character."),
        problems.stream().map(CreationProblem::message).toList());
    assertEquals(HintArea.ARMOR, problems.getFirst().area());
  }

  @Test
  void customGearBelongsToNoBook() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addWeapon(CarriedWeapon.of(new CustomWeapon("Homebrew", Skill.SMALL_GUNS, 4, "", "Physical", 1,
        WeaponRange.CLOSE, "", null)));
    character.setBookEnabled(Book.WOA, false);
    character.setBookEnabled(Book.WG, false);
    assertEquals(List.of(), hints(character));
  }

  /** Mods, legendary properties, ammunition and items of a switched-off book; a weapon twice gives one hint. */
  @Test
  void otherGearOfASwitchedOffBook() {
    PlayerCharacter character = PlayerCharacter.createNew();
    CarriedWeapon pistol = CarriedWeapon.of(CatalogRef.of("10mm-pistol"))
        .withMod(ModSlot.RECEIVER, CatalogRef.of("small-guns-9mm-receiver"))
        .withLegendary(CatalogRef.of("legendary-weapon-assassins"));
    character.gear().addWeapon(pistol);
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("357-magnum-revolver")));
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("357-magnum-revolver")));
    character.gear().addAmmunition("357-magnum", 10);
    character.gear().addItem(CarriedItem.of(CatalogRef.of("101-heroic-last-stands")));
    character.setBookEnabled(Book.WG, false);
    List<String> hints = hints(character);
    assertEquals(5, hints.size(), hints.toString());
    assertTrue(hints.contains("9mm Receiver (" + pistol.displayName()
        + ") is from Wanderer's Guide, which is switched off for this character."), hints.toString());
    assertTrue(hints.stream().anyMatch(hint -> hint.startsWith("Assassin's (")), hints.toString());
    assertTrue(hints.contains(".357 Magnum is from Wanderer's Guide, which is switched off for this character."));
    assertTrue(hints.contains(
        "101 Heroic Last Stands is from Wanderer's Guide, which is switched off for this character."));
  }

  /** An entry of two books needs one of them; with both off the hint names both. */
  @Test
  void entryOfTwoBooks() {
    PlayerCharacter character = PlayerCharacter.createNew();
    character.gear().addWeapon(CarriedWeapon.of(CatalogRef.of("self-destruct")));
    character.setBookEnabled(Book.WOA, false);
    assertEquals(List.of(), hints(character));
    character.setBookEnabled(Book.WG, false);
    assertEquals(List.of("Self-Destruct is from Winter of Atom and Wanderer's Guide, which are switched off for this "
        + "character."), hints(character));
  }
}
