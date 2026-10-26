package de.fallout2d20.charbuilder.model.character;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelModData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModSlot;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModTable;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import de.fallout2d20.charbuilder.model.character.apparel.ApparelMods;
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
   * Adds a weapon picked by hand: a counted catalog weapon (grenades, thrown weapons, mines) the character
   * already carries as a plain stack raises that stack's quantity, anything else is added (user decision
   * 2026-10-20). Starting equipment is never stacked onto, as choosing another pack replaces it.
   *
   * @return the index of the stack or of the new weapon
   */
  public int addWeaponStacked(@NonNull CarriedWeapon weapon) {
    CatalogRef ref = weapon.weapon();
    if (ref != null && isPlainCatalogStack(weapon) && weapon.skill().map(Skill::isCountedWeapon).orElse(false)) {
      for (int i = 0; i < weapons.size(); i++) {
        CarriedWeapon carried = weapons.get(i);
        CatalogRef carriedRef = carried.weapon();
        if (carriedRef != null && isPlainCatalogStack(carried) && carriedRef.id().equals(ref.id())) {
          weapons.set(i, carried.withQuantity(carried.quantity() + weapon.quantity()));
          return i;
        }
      }
    }
    weapons.add(weapon);
    return weapons.size() - 1;
  }

  private static boolean isPlainCatalogStack(@NonNull CarriedWeapon weapon) {
    return weapon.weapon() != null && weapon.notes().isEmpty() && weapon.mods().isEmpty() && !weapon.builtIn()
        && weapon.startingSource() == null && weapon.legendary() == null;
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
   * Chooses the qualities of the custom weapon at the index, as text (e.g. from the quality picker).
   *
   * @throws IllegalStateException if the weapon at the index is a catalog weapon
   */
  public void setCustomWeaponQualities(int index, @NonNull String qualities) {
    weapons.set(index, weapons.get(index).withCustomQualities(qualities));
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

  /**
   * Makes the weapon at the index legendary with this property (WG pp. 96–103), replacing its property; {@code null}
   * makes it an ordinary weapon. Which properties fit is decided by {@code LegendaryRules.options}.
   */
  public void setWeaponLegendary(int index, @Nullable LegendaryWeaponData property) {
    weapons.set(index, weapons.get(index).withLegendary(property == null ? null : CatalogRef.of(property.id())));
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

  /**
   * Adds shots to an ammunition type, e.g. bought or found together with a weapon; 0 changes nothing.
   *
   * @throws IllegalArgumentException if the id is blank or the shots are negative
   */
  public void addAmmunition(@NonNull String ammunitionId, int shots) {
    if (shots < 0) {
      throw new IllegalArgumentException("Shots to add must not be negative");
    }
    setAmmunition(ammunitionId, Math.addExact(ammunition(ammunitionId), shots));
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

  /** Free note of the piece at the index. */
  public void setApparelNotes(int index, @Nullable String notes) {
    apparel.set(index, apparel.get(index).withNotes(notes));
  }

  /**
   * Installs {@code mod} in the slot of the piece at the index; {@code null} removes it.
   *
   * @throws IllegalArgumentException if the mod does not fit the slot of this piece ({@link ApparelMods#fits})
   */
  public void setApparelMod(int index, @NonNull ApparelModSlot slot, @Nullable ApparelModData mod) {
    apparel.set(index, ApparelMods.install(apparel.get(index), slot, mod));
  }

  /**
   * Chooses the mod table of the custom piece at the index (user decision 2026-10-22); installed mods that no
   * longer fit are removed.
   *
   * @throws IllegalStateException if the piece at the index is a catalog piece
   */
  public void setCustomApparelModTable(int index, @Nullable ApparelModTable table) {
    apparel.set(index, apparel.get(index).withCustomModTable(table));
  }

  /**
   * Makes the piece at the index legendary with this property (WG pp. 104–108), replacing its property; {@code null}
   * makes it an ordinary piece. Which properties fit is decided by {@code LegendaryRules.options}.
   */
  public void setApparelLegendary(int index, @Nullable LegendaryArmorData property) {
    apparel.set(index, apparel.get(index).withLegendary(property == null ? null : CatalogRef.of(property.id())));
  }

  public @NonNull List<WornApparel> getApparel() {
    return List.copyOf(apparel);
  }

  /** Adds an item to the inventory. */
  public void addItem(@NonNull CarriedItem item) {
    items.add(item);
  }

  /**
   * Adds an item picked by hand: a catalog item or publication the character already carries as a plain
   * stack (no issue, no note, not starting equipment) raises that stack's quantity, anything else is added
   * (user decision 2026-10-20).
   *
   * @return the index of the stack or of the new item
   */
  public int addItemStacked(@NonNull CarriedItem item) {
    CatalogRef ref = item.item();
    if (ref != null && isPlainCatalogStack(item)) {
      for (int i = 0; i < items.size(); i++) {
        CarriedItem carried = items.get(i);
        CatalogRef carriedRef = carried.item();
        if (carriedRef != null && isPlainCatalogStack(carried) && carriedRef.id().equals(ref.id())) {
          items.set(i, carried.withQuantity(carried.quantity() + item.quantity()));
          return i;
        }
      }
    }
    items.add(item);
    return items.size() - 1;
  }

  private static boolean isPlainCatalogStack(@NonNull CarriedItem item) {
    return item.item() != null && item.issue() == null && item.notes().isEmpty() && item.startingSource() == null;
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

  /**
   * The pieces of junk carried: the junk counter (CRB p. 208) plus the junk items of the WG tables (WG pp. 120–125)
   * with their quantity (user decision 2026-10-22: both count for Junk Shield, WG p. 45).
   */
  public int junkCount() {
    return materials.junk() + items.stream().filter(CarriedItem::isJunk).mapToInt(CarriedItem::quantity).sum();
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
