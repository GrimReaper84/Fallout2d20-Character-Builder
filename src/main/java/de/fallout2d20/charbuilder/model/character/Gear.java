package de.fallout2d20.charbuilder.model.character;

import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import de.fallout2d20.charbuilder.model.character.apparel.ApparelRules;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.equipment.PackItems;
import de.fallout2d20.charbuilder.model.character.equipment.StartingNotes;
import de.fallout2d20.charbuilder.model.character.equipment.StartingSource;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.Holotape;
import de.fallout2d20.charbuilder.model.character.item.Inventory;
import de.fallout2d20.charbuilder.model.character.item.Materials;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.WeaponMods;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * What a character carries: weapons, ammunition, apparel, items, caps, junk/material counters and the gear
 * notes. Part of a {@link PlayerCharacter} ({@link PlayerCharacter#gear()}); editable in every mode, because
 * gear is found, bought and spent at any time.
 */
public final class Gear {

  private final @NonNull PlayerCharacter owner;
  private final @NonNull List<CarriedWeapon> weapons;
  /** Shots per ammunition id (CRB p. 91: counted per type, shared by all weapons using it); only counts above 0. */
  private final @NonNull Map<String, Integer> ammunition;
  private final @NonNull List<WornApparel> apparel;
  private final @NonNull List<CarriedItem> items;
  /** Caps on hand. */
  private long caps;
  private @NonNull Materials materials;
  private @NonNull String notes;

  /** Ammunition counts of 0 are dropped. */
  Gear(@NonNull PlayerCharacter owner, @NonNull List<CarriedWeapon> weapons, @NonNull Map<String, Integer> ammunition,
      @NonNull List<WornApparel> apparel, @NonNull Inventory inventory, @NonNull String notes) {
    this.owner = owner;
    this.weapons = new ArrayList<>(weapons);
    this.ammunition = new LinkedHashMap<>();
    ammunition.forEach((id, shots) -> {
      if (shots > 0) {
        this.ammunition.put(id, shots);
      }
    });
    this.apparel = new ArrayList<>(apparel);
    this.items = new ArrayList<>(inventory.items());
    this.caps = inventory.caps();
    this.materials = inventory.materials();
    this.notes = notes;
  }

  /** Free-text gear notes, e.g. amounts still to roll ({@link StartingNotes}). */
  public @NonNull String getNotes() {
    return notes;
  }

  public void setNotes(@Nullable String notes) {
    this.notes = notes == null ? "" : notes;
  }

  /** Adds a carried weapon. */
  public void addWeapon(@NonNull CarriedWeapon weapon) {
    weapons.add(weapon);
  }

  /**
   * Removes the carried weapon at the index.
   *
   * @throws IndexOutOfBoundsException if there is no weapon at the index
   */
  public void removeWeapon(int index) {
    weapons.remove(index);
  }

  /** Free note of the weapon at the index, e.g. installed mods. */
  public void setWeaponNotes(int index, @Nullable String notes) {
    weapons.set(index, weapons.get(index).withNotes(notes));
  }

  /**
   * How many of the weapon at the index are carried (grenades, mines, thrown weapons).
   *
   * @throws IllegalArgumentException if the quantity is below 1
   */
  public void setWeaponQuantity(int index, int quantity) {
    weapons.set(index, weapons.get(index).withQuantity(quantity));
  }

  /**
   * Chooses the attack skill of the custom weapon at the index (e.g. one imported without skill).
   *
   * @throws IllegalStateException if the weapon at the index is a catalog weapon
   */
  public void setCustomWeaponSkill(int index, @Nullable Skill skill) {
    weapons.set(index, weapons.get(index).withCustomSkill(skill));
  }

  /**
   * Installs a catalog mod in a slot of the weapon at the index, replacing the slot's mod; {@code null}
   * removes it. Mods of slots no longer on offer afterwards are removed ({@link WeaponMods#install}).
   *
   * @throws IllegalArgumentException if the mod does not fit the slot of this weapon
   */
  public void setWeaponMod(int index, @NonNull ModSlot slot, @Nullable WeaponModData mod) {
    weapons.set(index, WeaponMods.install(weapons.get(index), slot, mod));
  }

  /**
   * Chooses the mod slots of the custom weapon at the index (user decision 2026-10-17); mods of slots no
   * longer chosen are removed.
   *
   * @throws IllegalStateException if the weapon at the index is a catalog weapon
   */
  public void setCustomWeaponModSlots(int index, @NonNull Set<ModSlot> slots) {
    weapons.set(index, weapons.get(index).withCustomModSlots(slots));
  }

  public @NonNull List<CarriedWeapon> getWeapons() {
    return List.copyOf(weapons);
  }

  /**
   * Sets the shots of an ammunition type (CRB p. 91: shots are counted per type, all weapons using the
   * type share them; no magazines, user decision against the legacy app). 0 removes the type.
   *
   * @throws IllegalArgumentException if the id is blank or the shots are negative
   */
  public void setAmmunition(@NonNull String ammunitionId, int shots) {
    if (ammunitionId.isBlank() || shots < 0) {
      throw new IllegalArgumentException("Ammunition needs an id and a non-negative number of shots");
    }
    if (shots == 0) {
      ammunition.remove(ammunitionId);
    } else {
      ammunition.put(ammunitionId, shots);
    }
  }

  /** Shots of the ammunition type; 0 if none. */
  public int ammunition(@NonNull String ammunitionId) {
    return ammunition.getOrDefault(ammunitionId, 0);
  }

  /** Shots per ammunition id, only types with shots. */
  public @NonNull Map<String, Integer> getAmmunition() {
    return Collections.unmodifiableMap(ammunition);
  }

  /**
   * Adds a piece of apparel: worn, unless a worn piece of the same layer already covers one of its locations –
   * then carried (user decision 2026-10-16).
   */
  public void addApparel(@NonNull WornApparel item) {
    apparel.add(item.withCarried(ApparelRules.clashesWithWorn(owner, item)));
  }

  /** Marks the piece at the index as only carried (no protection, no hints) or as worn. */
  public void setApparelCarried(int index, boolean carried) {
    apparel.set(index, apparel.get(index).withCarried(carried));
  }

  /** The pieces actually worn: they protect and take part in the apparel hints. */
  public @NonNull List<WornApparel> getWornApparel() {
    return apparel.stream().filter(item -> !item.carried()).toList();
  }

  /**
   * Takes off the piece at the index.
   *
   * @throws IndexOutOfBoundsException if there is no piece at the index
   */
  public void removeApparel(int index) {
    apparel.remove(index);
  }

  /** The limb a single-limb piece is worn on (left/right arm or leg, robot arm 1–3); checked by the rules. */
  public void setApparelLocation(int index, @Nullable HitLocation location) {
    apparel.set(index, apparel.get(index).withLocation(location));
  }

  /** Free note of the piece at the index, e.g. installed mods. */
  public void setApparelNotes(int index, @Nullable String notes) {
    apparel.set(index, apparel.get(index).withNotes(notes));
  }

  public @NonNull List<WornApparel> getApparel() {
    return List.copyOf(apparel);
  }

  /** Adds an item to the inventory. */
  public void addItem(@NonNull CarriedItem item) {
    items.add(item);
  }

  /**
   * Removes the item at the index.
   *
   * @throws IndexOutOfBoundsException if there is no item at the index
   */
  public void removeItem(int index) {
    items.remove(index);
  }

  /** How many of the item at the index the character carries. */
  public void setItemQuantity(int index, int quantity) {
    items.set(index, items.get(index).withQuantity(quantity));
  }

  /** The issue of the magazine at the index (CRB p. 172); {@code null} while not rolled. */
  public void setItemIssue(int index, @Nullable String issue) {
    items.set(index, items.get(index).withIssue(issue));
  }

  /** Free note of the item at the index. */
  public void setItemNotes(int index, @Nullable String notes) {
    items.set(index, items.get(index).withNotes(notes));
  }

  /**
   * Renames or relabels the holotape at the index.
   *
   * @throws IllegalStateException if the item there is no holotape
   */
  public void setItemHolotape(int index, @NonNull Holotape holotape) {
    items.set(index, items.get(index).withHolotape(holotape));
  }

  public @NonNull List<CarriedItem> getItems() {
    return List.copyOf(items);
  }

  public long getCaps() {
    return caps;
  }

  /**
   * Caps on hand.
   *
   * @throws IllegalArgumentException for a negative amount
   */
  public void setCaps(long caps) {
    if (caps < 0) {
      throw new IllegalArgumentException("Caps cannot be negative");
    }
    this.caps = caps;
  }

  public @NonNull Materials getMaterials() {
    return materials;
  }

  public void setMaterials(@NonNull Materials materials) {
    this.materials = materials;
  }

  /**
   * Replaces the entries marked with {@code source} by {@code after} (apparel added as by
   * {@link #addApparel}) and the roll lines of {@code before} in the notes by those of {@code after}.
   */
  void replaceStartingEntries(@NonNull StartingSource source, @NonNull PackItems before, @NonNull PackItems after) {
    weapons.removeIf(weapon -> weapon.startingSource() == source);
    apparel.removeIf(item -> item.startingSource() == source);
    items.removeIf(item -> item.startingSource() == source);
    weapons.addAll(after.weapons());
    after.apparel().forEach(this::addApparel);
    items.addAll(after.items());
    notes = StartingNotes.rewrite(notes, source, before.rolls(), after.rolls());
  }
}
