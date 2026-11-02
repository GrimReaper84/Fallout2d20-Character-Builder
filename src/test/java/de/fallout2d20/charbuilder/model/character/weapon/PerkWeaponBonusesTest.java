package de.fallout2d20.charbuilder.model.character.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.perk.FilteredWeapon;
import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponKind;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Perk bonuses in the weapon values (plan C8; CRB pp. 59–73, WG pp. 44–53). */
class PerkWeaponBonusesTest {

  /** A character with the perks, each given as id and rank. */
  private static PlayerCharacter with(Object... idsAndRanks) {
    List<PerkSelection> perks = new ArrayList<>();
    for (int i = 0; i < idsAndRanks.length; i += 2) {
      perks.add(new PerkSelection(CatalogRef.of((String) idsAndRanks[i]), (Integer) idsAndRanks[i + 1], 0,
          List.of(), List.of()));
    }
    return PlayerCharacter.builder().perks(perks).build();
  }

  private static List<String> changes(WeaponAttack attack) {
    return attack.perkChanges().stream().map(PerkWeaponChange::text).toList();
  }

  private static List<String> hints(WeaponAttack attack) {
    return attack.perkHints().stream().map(PerkWeaponHint::text).toList();
  }

  private static WeaponAttack attack(PlayerCharacter character, String weaponId) {
    return WeaponAttack.of(character, CarriedWeapon.of(CatalogRef.of(weaponId)));
  }

  /** CRB p. 62: Fire Rate 3 or more, not heavy weapons. */
  @Test
  void commandoOnSubmachineGunNotOnMinigun() {
    PlayerCharacter character = with("commando", 2);
    WeaponAttack smg = attack(character, "submachine-gun");
    assertEquals(5, smg.damage());
    assertEquals(List.of("Commando 2: +2 CD"), changes(smg));
    assertEquals(3, attack(character, "minigun").damage());
    assertEquals(3, attack(character, "pipe-gun").damage(), "Fire Rate 2");
  }

  /** CRB p. 65; one-handed = no Two-Handed quality (user decision 2026-10-31). */
  @Test
  void gunslingerOnLeverActionRifleNotOnAssaultRifle() {
    PlayerCharacter character = with("gunslinger", 1);
    assertEquals(8, attack(character, "lever-action-rifle").damage());
    assertEquals(5, attack(character, "assault-rifle").damage());
  }

  /** CRB p. 70: rank 2 adds +1 to the Piercing rating. */
  @Test
  void riflemanRank2OnHuntingRifle() {
    WeaponAttack rifle = attack(with("rifleman", 2), "hunting-rifle");
    assertEquals(8, rifle.damage());
    assertEquals("Piercing 2", rifle.effects());
    assertEquals(List.of("Rifleman 2: +2 CD, Piercing +1"), changes(rifle));
  }

  /** A stock makes the pipe rifle two-handed (CRB p. 98), so Rifleman applies instead of Gunslinger. */
  @Test
  void riflemanOnPipeBoltActionWithStock() {
    PlayerCharacter character = with("rifleman", 1, "gunslinger", 1);
    CarriedWeapon pistol = CarriedWeapon.of(CatalogRef.of("pipe-bolt-action"));
    assertEquals(List.of("Gunslinger 1: +1 CD"), changes(WeaponAttack.of(character, pistol)));
    CarriedWeapon rifle = pistol.withMod(ModSlot.STOCK, CatalogRef.of("small-guns-marksmans-stock"));
    assertEquals(List.of("Rifleman 1: +1 CD"), changes(WeaponAttack.of(character, rifle)));
  }

  /** CRB p. 66: +1 CD; rank 2 also Vicious, not gained twice. */
  @Test
  void ironFistRank2OnUnarmedStrike() {
    PlayerCharacter character = with("iron-fist", 2);
    WeaponAttack strike = WeaponAttack.of(character, WeaponCatalog.byId("unarmed-strike").orElseThrow());
    assertEquals(3, strike.damage());
    assertEquals("Vicious", strike.effects());
    assertEquals("Vicious", attack(character, "handy-rock").effects());
    WeaponAttack rankOne = attack(with("iron-fist", 1), "unarmed-strike");
    assertEquals(3, rankOne.damage());
    assertEquals("", rankOne.effects());
  }

  /** CRB p. 61: two-handed melee weapons; the Sledgehammer is not Two-Handed in the table. */
  @Test
  void bigLeaguesSuperSledgeNotSledgehammer() {
    PlayerCharacter character = with("big-leagues", 1);
    assertEquals("Breaking, Vicious", attack(character, "super-sledge").effects());
    assertEquals("", attack(character, "sledgehammer").effects());
  }

  /** WG p. 44: Piercing 2 at rank 2, or +2 on an existing rating. */
  @Test
  void incisorRank2() {
    PlayerCharacter character = with("incisor", 2);
    assertEquals("Piercing 3", attack(character, "sword").effects());
    assertEquals("Piercing 2", attack(character, "baseball-bat").effects());
  }

  /** CRB p. 70: unarmed and bladed melee weapons; a Bladed mod makes a blunt weapon bladed. */
  @Test
  void piercingStrikeOnBladedAndUnarmed() {
    PlayerCharacter character = with("piercing-strike", 1);
    assertEquals("Piercing 2", attack(character, "machete").effects());
    assertEquals("", attack(character, "lead-pipe").effects());
    assertEquals("Piercing 1", attack(character, "knuckles").effects());
    CarriedWeapon bladedBat = CarriedWeapon.of(CatalogRef.of("baseball-bat"))
        .withMod(ModSlot.UPGRADE, CatalogRef.of("baseball-bat-bladed"));
    assertTrue(WeaponAttack.of(character, bladedBat).effects().contains("Piercing 1"));
  }

  /** CRB p. 71, WG p. 50. */
  @Test
  void shotgunSurgeonAndLicensedPlumber() {
    assertEquals("Spread, Piercing 1", attack(with("shotgun-surgeon", 1), "combat-shotgun").effects());
    WeaponAttack pipe = attack(with("licensed-plumber", 1), "pipe-gun");
    assertEquals("Close Quarters", pipe.qualities());
    assertEquals(List.of("Licensed Plumber: not Unreliable"), changes(pipe));
    assertEquals(List.of("Unreliable (removed by Licensed Plumber)"),
        ModdedWeapon.of(CarriedWeapon.of(CatalogRef.of("pipe-gun"))).orElseThrow()
            .withPerks(PerkWeaponBonuses.of(with("licensed-plumber", 1), new FilteredWeapon("pipe-gun",
                Skill.SMALL_GUNS, 2, List.of("Unreliable"), List.of(), "Physical", Set.of(WeaponKind.PIPE)))
                .changes()).removedQualities());
  }

  /** CRB p. 70: fire-based weapons, also the Chainsaw with its Flamer (WG p. 78) and the Burning Auto-Axe. */
  @Test
  void pyromaniacFlamerAndChainsawWithFlamer() {
    PlayerCharacter character = with("pyromaniac", 3);
    assertEquals(attack(PlayerCharacter.createNew(), "flamer").damage() + 3, attack(character, "flamer").damage());
    CarriedWeapon chainsaw = CarriedWeapon.of(CatalogRef.of("chainsaw"));
    int plain = WeaponAttack.of(character, chainsaw).damage();
    assertEquals(6, plain);
    CarriedWeapon flaming = chainsaw.withMod(ModSlot.ADD_ON, CatalogRef.of("chainsaw-flamer"));
    assertEquals(7 + 3, WeaponAttack.of(character, flaming).damage());
    CarriedWeapon autoAxe = CarriedWeapon.of(CatalogRef.of("auto-axe"));
    int axe = WeaponAttack.of(character, autoAxe).damage();
    CarriedWeapon burning = autoAxe.withMod(ModSlot.UPGRADE, CatalogRef.of("auto-axe-burning"));
    assertEquals(axe + 3, WeaponAttack.of(character, burning).damage(), "WG p. 76; user decision 2026-11-02");
  }

  /** WG p. 44: Big Guns with Fire Rate 2 or more; the perk's fire rate may exceed 6 (assumption). */
  @Test
  void lockAndLoadOnGatlingLaserNotOnFatMan() {
    PlayerCharacter character = with("lock-and-load", 2);
    assertEquals(8, attack(character, "gatling-laser").fireRate());
    assertEquals(0, attack(character, "fat-man").fireRate());
  }

  /** CRB p. 66, 72: the weapon type decides, so the Gatling Laser is a heavy weapon, not an energy weapon. */
  @Test
  void laserCommanderAndSizeMatters() {
    PlayerCharacter character = with("laser-commander", 1, "size-matters", 2);
    assertEquals(5, attack(character, "laser-gun").damage());
    WeaponAttack gatling = attack(character, "gatling-laser");
    assertEquals(5, gatling.damage());
    assertEquals(List.of("Size Matters 2: +2 CD"), changes(gatling));
  }

  /** WG p. 45: bows and crossbows, also a custom Athletics weapon. */
  @Test
  void archerAndBowBeforeMe() {
    PlayerCharacter character = with("archer", 2, "bow-before-me", 1);
    WeaponAttack bow = attack(character, "bow");
    assertEquals(5, bow.damage());
    assertEquals("Piercing 1", bow.effects());
    CustomWeapon sling = new CustomWeapon("Longbow", Skill.ATHLETICS, 4, "", "Physical", 1, WeaponRange.MEDIUM,
        "Two-Handed", null);
    assertEquals(6, WeaponAttack.of(character, CarriedWeapon.of(sling)).damage());
  }

  /** CRB p. 63: weapons with Blast. */
  @Test
  void demolitionExpertOnFragGrenade() {
    assertEquals("Vicious", attack(with("demolition-expert", 1), "frag-grenade").effects());
  }

  /** User decision 2026-10-31: custom weapons can be given kinds. */
  @Test
  void customWeaponWithShotgunKind() {
    CustomWeapon scattergun = new CustomWeapon("Scattergun", Skill.SMALL_GUNS, 5, "Spread", "Physical", 1,
        WeaponRange.CLOSE, "Two-Handed", null, Set.of(), Set.of(WeaponKind.SHOTGUN));
    assertEquals("Spread, Piercing 1",
        WeaponAttack.of(with("shotgun-surgeon", 1), CarriedWeapon.of(scattergun)).effects());
    assertEquals("Spread", WeaponAttack.of(with("shotgun-surgeon", 1),
        CarriedWeapon.of(scattergun.withKinds(Set.of()))).effects());
  }

  /** Situational perks are listed on the weapons they can apply to, never computed, with their rank filled in. */
  @Test
  void hintsOnlyOnMatchingWeapons() {
    PlayerCharacter character = with("ninja", 1, "mister-sandman", 1, "nerd-rage", 2);
    WeaponAttack sword = attack(character, "sword");
    assertEquals(4, sword.damage());
    assertEquals(List.of("Ninja: +2 CD on sneak attacks; not in Power Armor.",
        "Nerd Rage! 2: Below a quarter of your maximum HP: +2 CD."), hints(sword));
    assertEquals(List.of("Nerd Rage! 2: Below a quarter of your maximum HP: +2 CD."),
        hints(attack(character, "laser-gun")));
    assertTrue(hints(attack(character, "bow")).getFirst().startsWith("Mister Sandman"), "the Bow is Suppressed");
    assertEquals(Book.CRB, sword.perkHints().getFirst().source().book());
  }

  /** WG p. 44: +2 DR at rank 1, one more per rank. */
  @Test
  void hintWithRankOffset() {
    assertEquals(List.of("Bullet Shield 3: While you wield it: +4 DR."),
        hints(attack(with("bullet-shield", 3), "minigun")));
  }

  /** WG p. 46: rank 2 adds Accurate to the same line. */
  @Test
  void hintEntriesOfOnePerkShareALine() {
    assertEquals(List.of("Crack Shot 1: After aiming: range one step further."),
        hints(attack(with("crack-shot", 1), "10mm-pistol")));
    assertEquals(List.of("Crack Shot 2: After aiming: range one step further. Also Accurate."),
        hints(attack(with("crack-shot", 2), "10mm-pistol")));
  }

  /** Blitz helps only from rank 2 (CRB p. 61). */
  @Test
  void hintFromRank() {
    assertEquals(List.of(), attack(with("blitz", 1), "sword").perkHints());
    assertEquals(1, attack(with("blitz", 2), "sword").perkHints().size());
  }

  /** Staged level-up ranks count everywhere (project rules § 7). */
  @Test
  void stagedPerkRankCounts() {
    PlayerCharacter character = PlayerCharacter.builder().perks(List.of(new PerkSelection(
        CatalogRef.of("commando"), 2, 0, List.of(), List.of(), 1))).build();
    assertEquals(5, attack(character, "submachine-gun").damage());
  }

  @Test
  void noPerksNoChanges() {
    WeaponAttack smg = attack(PlayerCharacter.createNew(), "submachine-gun");
    assertEquals(List.of(), changes(smg));
    assertEquals(List.of(), smg.perkHints());
  }
}
