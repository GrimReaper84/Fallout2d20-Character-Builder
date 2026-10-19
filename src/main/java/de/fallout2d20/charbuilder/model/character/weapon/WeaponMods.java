package de.fallout2d20.charbuilder.model.character.weapon;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.catalog.weapon.ModEffect;
import de.fallout2d20.charbuilder.catalog.weapon.ModEffectType;
import de.fallout2d20.charbuilder.catalog.weapon.ModPerk;
import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Which mods fit a carried weapon (CRB p. 87: the mods its description lists, one per slot). A catalog
 * weapon offers the slots of the mods it accepts; a slot that a mod of its own unlocks (Bracketed barrels:
 * Muzzle, CRB p. 104–105) only while such a mod is installed. A custom weapon offers the slots the player
 * chose and every catalog mod of them, those of the mod table of its weapon type first (user decision
 * 2026-10-17). Where the book allows a Grip or a Stock but not both (CRB p. 98), installing one removes the
 * other.
 */
public final class WeaponMods {

  private static final @NonNull Map<Skill, String> TABLE_OF_SKILL =
      Map.of(Skill.SMALL_GUNS, "small-guns", Skill.ENERGY_WEAPONS, "energy-weapons");

  private WeaponMods() {}

  /** The slots on offer, in slot order; none for a weapon without mods or an id outside the catalog. */
  public static @NonNull List<ModSlot> slots(@NonNull CarriedWeapon weapon) {
    CustomWeapon custom = weapon.custom();
    if (custom != null) {
      return List.copyOf(custom.modSlots());
    }
    List<WeaponModData> accepted = accepted(weapon);
    Set<ModSlot> unlockable = unlockedSlots(accepted);
    Set<ModSlot> unlocked = unlockedSlots(installedMods(weapon));
    Set<ModSlot> slots = EnumSet.noneOf(ModSlot.class);
    for (WeaponModData mod : accepted) {
      if (!unlockable.contains(mod.slot()) || unlocked.contains(mod.slot())) {
        slots.add(mod.slot());
      }
    }
    return List.copyOf(slots);
  }

  /** The mods that can go into a slot of the weapon, of the enabled books. */
  public static @NonNull List<WeaponModData> options(@NonNull CarriedWeapon weapon, @NonNull ModSlot slot,
      @NonNull Set<Book> books) {
    CustomWeapon custom = weapon.custom();
    if (custom == null) {
      return accepted(weapon).stream().filter(mod -> mod.slot() == slot && mod.isAvailableWith(books)).toList();
    }
    Skill skill = custom.skill();
    String preferred = skill == null ? null : TABLE_OF_SKILL.get(skill);
    return WeaponCatalog.mods().stream()
        .filter(mod -> mod.slot() == slot && mod.isAvailableWith(books))
        .sorted(Comparator.comparing(mod -> !mod.table().equals(preferred)))
        .toList();
  }

  /** Whether the mod can go into the slot: the slot is on offer and the weapon accepts the mod there. */
  public static boolean canInstall(@NonNull CarriedWeapon weapon, @NonNull ModSlot slot, @NonNull WeaponModData mod) {
    return mod.slot() == slot && slots(weapon).contains(slot)
        && (weapon.custom() != null || weapon.data().map(data -> data.mods().contains(mod.id())).orElse(false));
  }

  /**
   * The weapon with the mod installed in the slot ({@code null} removes the slot's mod). Installing a Grip
   * removes the Stock and the other way round where the weapon takes only one of them (CRB p. 98). Mods of
   * slots no longer on offer afterwards (a Muzzle mod after its Bracketed barrel is removed) are removed too.
   *
   * @throws IllegalArgumentException if the mod does not fit the slot of this weapon
   */
  public static @NonNull CarriedWeapon install(@NonNull CarriedWeapon weapon, @NonNull ModSlot slot,
      @Nullable WeaponModData mod) {
    if (mod != null && !canInstall(weapon, slot, mod)) {
      throw new IllegalArgumentException(mod.id() + " does not fit the " + slot + " of " + weapon.displayName());
    }
    CarriedWeapon changed = weapon.withMod(slot, mod == null ? null : CatalogRef.of(mod.id()));
    if (mod != null && (slot == ModSlot.GRIP || slot == ModSlot.STOCK)
        && weapon.data().map(WeaponData::gripOrStock).orElse(false)) {
      changed = changed.withMod(slot == ModSlot.GRIP ? ModSlot.STOCK : ModSlot.GRIP, null);
    }
    List<ModSlot> offered = slots(changed);
    for (ModSlot installed : Set.copyOf(changed.mods().keySet())) {
      if (!offered.contains(installed)) {
        changed = changed.withMod(installed, null);
      }
    }
    return changed;
  }

  /**
   * The installed mods the catalog knows, in slot order. Unknown ids and mods stored under another slot than
   * their own (a hand-edited file, a later catalog fix) are left out, so a slot never counts twice (CRB p. 87).
   */
  public static @NonNull List<WeaponModData> installedMods(@NonNull CarriedWeapon weapon) {
    return weapon.mods().entrySet().stream()
        .flatMap(entry -> WeaponCatalog.modById(entry.getValue().id()).filter(mod -> mod.slot() == entry.getKey())
            .stream())
        .toList();
  }

  /**
   * The perk ranks needed for the mod that the character does not have (CRB p. 87); information only, never
   * a block (user decision 2026-10-17).
   */
  public static @NonNull List<ModPerk> missingPerks(@NonNull PlayerCharacter character, @NonNull WeaponModData mod) {
    return mod.perks().stream()
        .filter(needed -> character.getPerks().stream()
            .noneMatch(perk -> perk.perk().id().equals(needed.perk()) && perk.rank() >= needed.rank()))
        .toList();
  }

  /** A needed perk rank as shown to the player, e.g. "Gun Nut 2". */
  public static @NonNull String perkText(@NonNull ModPerk perk) {
    return PerkCatalog.byId(perk.perk()).map(PerkDefinition::name).orElse(perk.perk()) + " " + perk.rank();
  }

  private static @NonNull List<WeaponModData> accepted(@NonNull CarriedWeapon weapon) {
    return weapon.data().map(WeaponData::mods).orElse(List.of()).stream()
        .map(WeaponCatalog::modById)
        .flatMap(Optional::stream)
        .toList();
  }

  private static @NonNull Set<ModSlot> unlockedSlots(@NonNull List<WeaponModData> mods) {
    Set<ModSlot> slots = EnumSet.noneOf(ModSlot.class);
    for (WeaponModData mod : mods) {
      for (ModEffect effect : mod.effects()) {
        if (effect.type() == ModEffectType.ALLOWS_SLOT) {
          slots.add(ModSlot.valueOf(effect.text()));
        }
      }
    }
    return slots;
  }
}
