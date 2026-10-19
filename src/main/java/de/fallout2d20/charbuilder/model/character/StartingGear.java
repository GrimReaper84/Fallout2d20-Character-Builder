package de.fallout2d20.charbuilder.model.character;

import de.fallout2d20.charbuilder.catalog.equipment.EquipmentCatalog;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentPackData;
import de.fallout2d20.charbuilder.catalog.equipment.PackChoice;
import de.fallout2d20.charbuilder.catalog.equipment.TagSkillItemData;
import de.fallout2d20.charbuilder.model.character.equipment.ArmSelection;
import de.fallout2d20.charbuilder.model.character.equipment.PackInventory;
import de.fallout2d20.charbuilder.model.character.equipment.PackItems;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipmentItems;
import de.fallout2d20.charbuilder.model.character.equipment.StartingNotes;
import de.fallout2d20.charbuilder.model.character.equipment.StartingSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The starting equipment of a character (CRB p. 50, step 6; pp. 76–81): the chosen pack with its choices,
 * Tag skill item picks, trinket and Mister Handy arms, and its entries in the {@link Gear}. Part of a
 * {@link PlayerCharacter} ({@link PlayerCharacter#startingGear()}); chosen during creation.
 */
public final class StartingGear {

  private final @NonNull PlayerCharacter owner;
  private @NonNull StartingEquipment selection;
  private final boolean packItemsAdded;
  private int syncs;

  /**
   * @param selection the selection, already fitted to the catalog
   * @param packItemsAdded whether an earlier version added the pack items once by a button
   */
  StartingGear(@NonNull PlayerCharacter owner, @NonNull StartingEquipment selection, boolean packItemsAdded) {
    this.owner = owner;
    this.selection = selection;
    this.packItemsAdded = packItemsAdded;
  }

  public @NonNull StartingEquipment getSelection() {
    return selection;
  }

  /** The catalog entry of the chosen equipment pack; empty without pack or for an unknown one. */
  public @NonNull Optional<EquipmentPackData> getPack() {
    CatalogRef pack = selection.pack();
    return pack == null ? Optional.empty() : EquipmentCatalog.packById(pack.id());
  }

  private @NonNull EquipmentPackData requirePack() {
    return getPack().orElseThrow(() -> new IllegalStateException("No equipment pack chosen"));
  }

  /**
   * Chooses the starting equipment pack during creation; whether the origin may take it is checked by the
   * creation rules. All pack choices start open; Tag skill item picks and the trinket stay (a trinket only
   * counts for a pack that has one). A Mister Handy pack sets the arms the pack fixes and leaves the choosable
   * ones open (CRB p. 77); free arms are switched off.
   *
   * @throws IllegalArgumentException if the pack is not in the catalog
   */
  public void choosePack(@NonNull CatalogRef pack) {
    owner.requireCreation();
    EquipmentPackData data = EquipmentCatalog.packById(pack.id())
        .orElseThrow(() -> new IllegalArgumentException("Unknown equipment pack: " + pack.id()));
    List<@Nullable String> choices = new ArrayList<>(Collections.nCopies(data.choices().size(), null));
    selection = new StartingEquipment(pack, choices, selection.tagSkillItems(), selection.trinket(), false,
        StartingEquipment.packArms(data, List.of()));
  }

  /**
   * Picks an option of one of the pack's choices during creation; {@code null} opens it again.
   *
   * @throws IllegalArgumentException if the pack has no such choice or the choice no such option
   * @throws IllegalStateException if no equipment pack is chosen
   */
  public void chooseOption(int index, @Nullable String option) {
    owner.requireCreation();
    List<PackChoice> choices = requirePack().choices();
    if (index < 0 || index >= choices.size()
        || (option != null && !choices.get(index).options().contains(option))) {
      throw new IllegalArgumentException("No pack choice " + index + " with option " + option);
    }
    selection = selection.withChoice(index, option);
  }

  /**
   * Picks which of the offered items a Tag skill grants (CRB p. 81) during creation; {@code null}
   * removes the pick.
   *
   * @throws IllegalArgumentException if the skill's items offer no choice or not this option
   */
  public void chooseTagSkillItem(@NonNull Skill skill, @Nullable String option) {
    owner.requireCreation();
    TagSkillItemData items = EquipmentCatalog.tagSkillItems(skill);
    if (!items.isChoice() || (option != null && !items.options().contains(option))) {
      throw new IllegalArgumentException("No Tag skill item choice " + option + " for " + skill);
    }
    selection = selection.withTagSkillItem(skill, option);
  }

  /**
   * The personal trinket (CRB p. 80) during creation; blank means none described yet. It is kept across
   * pack changes and only counts for a pack that includes a trinket.
   */
  public void setTrinket(@Nullable String trinket) {
    owner.requireCreation();
    selection = selection.withTrinket(trinket);
  }

  /**
   * Switches the Mister Handy's freely configurable arms (house-rule option beyond the book) on or off
   * during creation. Switching on keeps the current arms as a starting point; switching off restores the
   * arms the pack fixes and keeps choosable arms only if the pack allows them.
   *
   * @throws IllegalStateException if no equipment pack is chosen or it has no arms
   */
  public void setFreeArms(boolean free) {
    owner.requireCreation();
    EquipmentPackData pack = requirePack();
    if (pack.arms().isEmpty()) {
      throw new IllegalStateException("The equipment pack has no arms: " + pack.id());
    }
    List<ArmSelection> arms = free ? selection.arms() : StartingEquipment.packArms(pack, selection.arms());
    selection = selection.withArms(free, arms);
  }

  /**
   * Sets one arm (slot 0 to 2) during creation. Whether the pack allows it is checked by the creation
   * rules, not here.
   *
   * @throws IllegalStateException if the starting equipment has no arms
   * @throws IllegalArgumentException if the slot is not 0 to 2
   */
  public void chooseArm(int slot, @NonNull ArmSelection arm) {
    owner.requireCreation();
    if (selection.arms().isEmpty()) {
      throw new IllegalStateException("The starting equipment has no arms");
    }
    if (slot < 0 || slot >= selection.arms().size()) {
      throw new IllegalArgumentException("No arm slot " + slot);
    }
    selection = selection.withArm(slot, arm);
  }

  /**
   * Whether an earlier version added the pack items once by a button: those entries are not marked, so the
   * starting equipment is no longer synced into the inventory (nothing is doubled).
   */
  public boolean isPackItemsAdded() {
    return packItemsAdded;
  }

  /**
   * Whether the starting equipment can be (re-)entered into the inventory: during creation, after it in GM
   * mode, never for a character whose pack items an earlier version already added.
   */
  public boolean canApply() {
    return !packItemsAdded && (!owner.isCreationComplete() || owner.isGmMode());
  }

  /**
   * Enters the starting equipment (pack, choices, trinket, Tag skill items) into the {@link Gear} (user
   * decision 2026-10-19): the entries of an earlier pick are replaced, entries added by hand stay. Weapons,
   * worn apparel and items are marked with their {@link StartingSource}; amounts still to roll become lines in
   * the gear notes, replacing the untouched lines of {@code previous} ({@link StartingNotes}); during creation
   * the caps become the fixed starting caps (pack and starting level, CRB p. 81). Without a pack nothing is
   * entered and earlier entries are removed.
   *
   * @param previous the selection the current entries were made from (the pack dialog's snapshot); the current
   *     selection on the first entry
   * @throws IllegalStateException if {@link #canApply()} is false
   */
  public void apply(@NonNull StartingEquipment previous) {
    if (!canApply()) {
      throw new IllegalStateException("The starting equipment cannot be entered into the inventory");
    }
    StartingEquipment current = selection;
    selection = previous;
    Map<StartingSource, PackItems> before = entries();
    selection = current;
    for (StartingSource source : StartingSource.values()) {
      sync(source, before.getOrDefault(source, PackItems.NONE), entries(source));
    }
    setStartingCaps();
  }

  /**
   * Restores a snapshot of the selection (a cancelled pack dialog); the inventory is not touched.
   */
  public void restore(@NonNull StartingEquipment snapshot) {
    owner.requireCreation();
    selection = snapshot;
  }

  /** Names of the weapons, apparel and items marked as starting equipment, which a re-sync replaces. */
  public @NonNull List<String> entryNames() {
    return owner.gear().startingEntryNames();
  }

  /**
   * How often the starting equipment was synced into the inventory in this session (not saved): a sync moves
   * entries, so a view holding a selected list index clears it when this changes.
   */
  public int syncs() {
    return syncs;
  }

  /** The entries of every part of the starting equipment for the current selection. */
  private @NonNull Map<StartingSource, PackItems> entries() {
    Map<StartingSource, PackItems> entries = new EnumMap<>(StartingSource.class);
    for (StartingSource source : StartingSource.values()) {
      entries.put(source, entries(source));
    }
    return entries;
  }

  /** The entries of one part; none before a pack is chosen (the starting equipment step has not begun). */
  private @NonNull PackItems entries(@NonNull StartingSource source) {
    return selection.pack() == null ? PackItems.NONE : PackInventory.resolve(owner, source);
  }

  private void sync(@NonNull StartingSource source, @NonNull PackItems before, @NonNull PackItems after) {
    owner.gear().replaceStartingEntries(source, before, after);
    syncs++;
  }

  /**
   * Runs a creation edit that may change the Tag skills (CRB p. 81: their items, Barter's d20 caps) or drop the
   * pack (another origin), then re-syncs each part of the starting equipment whose entries changed (user
   * decision 2026-10-19), so edits of unchanged entries survive. A dropped pack also resets the caps.
   */
  void syncing(@NonNull Runnable edit) {
    if (owner.isCreationComplete() || packItemsAdded) {
      edit.run();
      return;
    }
    Map<StartingSource, PackItems> before = entries();
    edit.run();
    for (StartingSource source : StartingSource.values()) {
      PackItems earlier = before.getOrDefault(source, PackItems.NONE);
      PackItems after = entries(source);
      if (!after.equals(earlier)) {
        sync(source, earlier, after);
        if (source == StartingSource.PACK) {
          setStartingCaps();
        }
      }
    }
  }

  /** Drops the selection unless its pack is offered to the origin too (e.g. Ghoul and Survivor packs). */
  void keepOnlyIfOfferedTo(@NonNull CatalogRef origin) {
    if (getPack().filter(pack -> pack.isOfferedTo(origin.id())).isEmpty()) {
      selection = StartingEquipment.NONE;
    }
  }

  /** A new starting level changes the starting caps of a chosen pack (CRB p. 81). */
  void startingLevelChanged() {
    if (selection.pack() != null) {
      setStartingCaps();
    }
  }

  /** During creation: the caps become the fixed starting caps (pack and starting level, CRB p. 81). */
  private void setStartingCaps() {
    if (!owner.isCreationComplete() && !packItemsAdded) {
      owner.gear().setCaps(StartingEquipmentItems.caps(owner).fixedTotal());
    }
  }
}
