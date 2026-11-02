package de.fallout2d20.charbuilder.model.character.weapon;

import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.perk.FilteredWeapon;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.catalog.perk.PerkWeaponBonus;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * What the character's perks do to one weapon (CRB pp. 59–73, WG pp. 44–53), computed on every call: the changes of
 * the perks whose weapon conditions it meets at their current rank (staged level-up ranks count) and the hints of the
 * situational ones, their rank filled in. Conditions are checked on the weapon after its mods and legendary
 * property; the changes are applied afterwards ({@link ModdedWeapon#withPerks}), so one perk never changes whether
 * another applies.
 */
public final class PerkWeaponBonuses {

  private PerkWeaponBonuses() {}

  /** The bonuses of the character's catalogued perks for the weapon. */
  public static @NonNull PerkWeaponResult of(@NonNull PlayerCharacter character, @NonNull FilteredWeapon weapon) {
    List<PerkWeaponChange> changes = new ArrayList<>();
    List<PerkWeaponHint> hints = new ArrayList<>();
    for (PerkSelection selection : character.getPerks()) {
      Optional<PerkDefinition> definition = PerkCatalog.byId(selection.perk().id());
      if (definition.isEmpty() || definition.get().weaponBonuses().isEmpty()) {
        continue;
      }
      PerkDefinition perk = definition.get();
      int rank = selection.rank();
      String label = perk.ranks() > 1 ? perk.name() + " " + rank : perk.name();
      Source source = perk.sources().getFirst();
      int damage = 0;
      int piercing = 0;
      int fireRate = 0;
      List<String> gained = new ArrayList<>();
      List<String> removed = new ArrayList<>();
      List<String> situational = new ArrayList<>();
      for (PerkWeaponBonus bonus : perk.weaponBonuses()) {
        if (!bonus.appliesAt(rank) || !bonus.weapons().matches(weapon)) {
          continue;
        }
        String hint = bonus.hintAt(rank);
        if (hint != null) {
          situational.add(hint);
          continue;
        }
        damage += bonus.at(bonus.damage(), rank);
        piercing += bonus.at(bonus.piercing(), rank);
        fireRate += bonus.at(bonus.fireRate(), rank);
        gained.addAll(bonus.gainedEffects());
        removed.addAll(bonus.removedQualities());
      }
      if (damage > 0 || piercing > 0 || fireRate > 0 || !gained.isEmpty() || !removed.isEmpty()) {
        changes.add(new PerkWeaponChange(label, damage, piercing, fireRate, gained, removed, source));
      }
      if (!situational.isEmpty()) {
        hints.add(new PerkWeaponHint(label, String.join(" ", situational), source));
      }
    }
    return changes.isEmpty() && hints.isEmpty() ? PerkWeaponResult.NONE : new PerkWeaponResult(changes, hints);
  }
}
