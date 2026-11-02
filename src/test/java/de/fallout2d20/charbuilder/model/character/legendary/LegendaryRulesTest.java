package de.fallout2d20.charbuilder.model.character.legendary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorLocation;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryCatalog;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponType;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.apparel.CustomApparel;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.CustomWeapon;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Which items can get which legendary properties (WG pp. 96–104, user decisions 2026-10-23). */
class LegendaryRulesTest {

  private final PlayerCharacter character = PlayerCharacter.builder().enabledBooks(Set.of(Book.CRB, Book.WG)).build();

  /** A Hunting Rifle gets exactly the Small Guns column of both tables: 17 + 17 properties. */
  @Test
  void smallGun() {
    List<LegendaryWeaponData> options = LegendaryRules.options(character, weapon("hunting-rifle"));
    assertEquals(34, options.size());
    assertTrue(options.stream().allMatch(property -> property.fits(LegendaryWeaponType.SMALL_GUNS)));
    assertTrue(names(options).contains("Explosive"));
    assertFalse(names(options).contains("Mighty"));
  }

  /** A Machete gets the Melee column (20 + 11), with Mighty instead of Powerful. */
  @Test
  void melee() {
    List<String> names = names(LegendaryRules.options(character, weapon("machete")));
    assertEquals(31, names.size());
    assertTrue(names.contains("Mighty"));
    assertFalse(names.contains("Powerful"));
  }

  /** WG p. 96: the Minigun counts as a Small Gun, the Blast 25mm Grenade APW as a Big Gun. */
  @Test
  void unusualWeapons() {
    assertTrue(names(LegendaryRules.options(character, weapon("minigun"))).contains("Violent"));
    assertFalse(names(LegendaryRules.options(character, weapon("25mm-grenade-apw"))).contains("Violent"));
  }

  /** WG p. 96: bows, explosives and throwing weapons cannot be legendary; a custom weapon goes by its skill. */
  @Test
  void excludedWeapons() {
    assertTrue(LegendaryRules.options(character, weapon("bow")).isEmpty());
    assertTrue(LegendaryRules.options(character, weapon("frag-grenade")).isEmpty());
    assertTrue(LegendaryRules.options(character, weapon("javelin")).isEmpty());
    CarriedWeapon club = CarriedWeapon.of(new CustomWeapon("Club", Skill.MELEE_WEAPONS, 3, "", "Physical", null, null,
        "", null));
    assertEquals(31, LegendaryRules.options(character, club).size());
  }

  /** WG p. 96: a custom Small Gun with Blast rolls as a Big Gun, like a catalog one. */
  @Test
  void customBlastSmallGun() {
    CarriedWeapon launcher = CarriedWeapon.of(new CustomWeapon("Launcher", Skill.SMALL_GUNS, 4, "", "Physical", 1, null,
        "Blast, Two-Handed", null));
    assertEquals(LegendaryWeaponType.BIG_GUNS, launcher.legendaryType().orElseThrow());
  }

  /** The properties need the Wanderer's Guide; GM free edit offers all of them for any item. */
  @Test
  void bookAndGmMode() {
    PlayerCharacter core = PlayerCharacter.builder().enabledBooks(Set.of(Book.CRB)).build();
    assertTrue(LegendaryRules.options(core, weapon("hunting-rifle")).isEmpty());
    core.setGmMode(true);
    assertEquals(44, LegendaryRules.options(core, weapon("bow")).size());
    assertEquals(28, LegendaryRules.options(core, WornApparel.of(CatalogRef.of("vault-jumpsuit"))).size());
  }

  /** WG p. 104: an arm piece gets the Arm column only; clothing none; robot armor by its mapped location. */
  @Test
  void armor() {
    List<LegendaryArmorData> arm = LegendaryRules.options(character, WornApparel.of(CatalogRef.of("combat-arm")));
    assertEquals(20, arm.size());
    assertTrue(arm.stream().allMatch(property -> property.fits(LegendaryArmorLocation.ARM)));
    assertTrue(LegendaryRules.options(character, WornApparel.of(CatalogRef.of("vault-jumpsuit"))).isEmpty());
    assertEquals(EnumSet.of(LegendaryArmorLocation.HEAD),
        WornApparel.of(CatalogRef.of("factory-armor-optics")).legendaryLocations());
    assertEquals(18, LegendaryRules.options(character, WornApparel.of(CatalogRef.of("factory-armor-optics"))).size());
  }

  /** A piece covering several locations offers the properties of every covered column. */
  @Test
  void severalLocations() {
    WornApparel suit = WornApparel.of(new CustomApparel("Plate", ApparelKind.ARMOR,
        Set.of(HitLocation.TORSO, HitLocation.LEFT_LEG), 3, 3, 0, false, 0, null));
    List<String> names = names(LegendaryRules.options(character, suit));
    assertTrue(names.contains("Unyielding"));
    assertTrue(names.contains("Acrobat's"));
    assertFalse(names.contains("Safecracker's"));
  }

  @Test
  void name() {
    assertEquals("★ Two-Shot Hunting Rifle", LegendaryRules.name("Two-Shot", "Hunting Rifle"));
    WornApparel piece = WornApparel.of(CatalogRef.of("combat-arm")).withLegendary(CatalogRef.of(
        LegendaryCatalog.armorPropertyById("legendary-armor-sentinels").orElseThrow().id()));
    assertEquals("★ Sentinel's Combat Arm", piece.legendaryName());
  }

  private static CarriedWeapon weapon(String id) {
    return CarriedWeapon.of(CatalogRef.of(id));
  }

  private static <T> List<String> names(List<T> properties) {
    return properties.stream().map(property -> property instanceof LegendaryWeaponData weapon ? weapon.name()
        : ((LegendaryArmorData) property).name()).toList();
  }
}
