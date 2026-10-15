package de.fallout2d20.charbuilder.model.character.equipment;

import de.fallout2d20.charbuilder.catalog.equipment.ArmSlot;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentCatalog;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentPackData;
import de.fallout2d20.charbuilder.catalog.equipment.TagSkillItemData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The starting equipment chosen during creation (CRB p. 50, step 6): a pack and what the player picks
 * within it. The selection is stored, not the resulting items; they are rendered from it.
 *
 * @param pack the chosen equipment pack, or {@code null}
 * @param choices the option picked for each of the pack's choices, index-aligned; {@code null} = open
 * @param tagSkillItems picked option per Tag skill whose items offer a choice (CRB p. 81); picks of
 *     skills that are no longer tagged are kept but ignored
 * @param trinket the personal trinket (CRB p. 80), or {@code null}; kept across pack changes and only
 *     counted for a pack that includes one
 * @param freeArms whether the Mister Handy's arms are freely configurable (house-rule option) instead
 *     of following the pack
 * @param arms the three arms of a Mister Handy pack (CRB p. 55); empty for other packs
 */
public record StartingEquipment(
    @Nullable CatalogRef pack,
    @NonNull List<@Nullable String> choices,
    @NonNull Map<Skill, String> tagSkillItems,
    @Nullable String trinket,
    boolean freeArms,
    @NonNull List<ArmSelection> arms) {

  public static final @NonNull StartingEquipment NONE =
      new StartingEquipment(null, List.of(), Map.of(), null, false, List.of());

  public StartingEquipment {
    if (!arms.isEmpty() && arms.size() != EquipmentPackData.ARM_COUNT) {
      throw new IllegalArgumentException("A Mister Handy has three arms");
    }
    choices = Collections.unmodifiableList(new ArrayList<>(choices));
    tagSkillItems = tagSkillItems.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(tagSkillItems));
    trinket = trinket == null || trinket.isBlank() ? null : trinket.strip();
    arms = List.copyOf(arms);
  }

  public @NonNull StartingEquipment withChoice(int index, @Nullable String option) {
    List<@Nullable String> changed = new ArrayList<>(choices);
    changed.set(index, option);
    return new StartingEquipment(pack, changed, tagSkillItems, trinket, freeArms, arms);
  }

  public @NonNull StartingEquipment withTagSkillItem(@NonNull Skill skill, @Nullable String option) {
    Map<Skill, String> changed = new EnumMap<>(Skill.class);
    changed.putAll(tagSkillItems);
    if (option == null) {
      changed.remove(skill);
    } else {
      changed.put(skill, option);
    }
    return new StartingEquipment(pack, choices, changed, trinket, freeArms, arms);
  }

  public @NonNull StartingEquipment withTrinket(@Nullable String value) {
    return new StartingEquipment(pack, choices, tagSkillItems, value, freeArms, arms);
  }

  public @NonNull StartingEquipment withArms(boolean free, @NonNull List<ArmSelection> value) {
    return new StartingEquipment(pack, choices, tagSkillItems, trinket, free, value);
  }

  public @NonNull StartingEquipment withArm(int slot, @NonNull ArmSelection arm) {
    List<ArmSelection> changed = new ArrayList<>(arms);
    changed.set(slot, arm);
    return withArms(freeArms, changed);
  }

  /**
   * This selection made consistent with the catalog, for data from a file. For a known pack the structure
   * always fits afterwards – exactly one entry per pack choice, and the pack's arms when their number does
   * not fit (three for a Mister Handy pack, none otherwise; free arms only for a pack with arms) – which
   * the rendering and the creation rules rely on. With {@code keepStalePicks} false (characters in
   * creation) picks that are no longer an option of their choice or Tag skill row become open again and
   * show up as creation hints, so a save stays loadable when a catalog text is corrected later; true
   * (complete characters, who cannot pick again) keeps their text. A pack outside the catalog keeps its
   * choices and arms.
   */
  public @NonNull StartingEquipment fittedToCatalog(boolean keepStalePicks) {
    Map<Skill, String> picks = new EnumMap<>(Skill.class);
    tagSkillItems.forEach((skill, option) -> {
      TagSkillItemData row = EquipmentCatalog.tagSkillItems(skill);
      if (keepStalePicks || (row.isChoice() && row.options().contains(option))) {
        picks.put(skill, option);
      }
    });
    Optional<EquipmentPackData> data = pack == null ? Optional.empty() : EquipmentCatalog.packById(pack.id());
    if (data.isEmpty()) {
      return new StartingEquipment(pack, choices, picks, trinket, freeArms, arms);
    }
    List<@Nullable String> fitted = new ArrayList<>();
    for (int i = 0; i < data.get().choices().size(); i++) {
      String option = i < choices.size() ? choices.get(i) : null;
      boolean valid = option != null && (keepStalePicks || data.get().choices().get(i).options().contains(option));
      fitted.add(valid ? option : null);
    }
    List<ArmSelection> fittedArms = arms.size() == data.get().arms().size() ? arms : packArms(data.get(), List.of());
    return new StartingEquipment(pack, fitted, picks, trinket, freeArms && !fittedArms.isEmpty(), fittedArms);
  }

  /**
   * The arms a pack gives (CRB p. 77): fixed arms get their attachment, choosable arms keep the current
   * arm if the pack allows it, else they are open. Empty for a pack without arms.
   */
  public static @NonNull List<ArmSelection> packArms(@NonNull EquipmentPackData pack,
      @NonNull List<ArmSelection> current) {
    List<ArmSelection> arms = new ArrayList<>();
    for (int slot = 0; slot < pack.arms().size(); slot++) {
      ArmSlot allowed = pack.arms().get(slot);
      if (allowed.fixed()) {
        arms.add(ArmSelection.of(allowed.options().get(0)));
      } else {
        ArmSelection kept = slot < current.size() ? current.get(slot) : ArmSelection.OPEN;
        arms.add(allowed.allows(kept.attachment()) ? kept : ArmSelection.OPEN);
      }
    }
    return arms;
  }

  /** Whether one of the arms is a pincer. */
  public boolean hasPincer() {
    return arms.stream().anyMatch(ArmSelection::isPincer);
  }

  /**
   * Whether a Mister Handy goes without a pincer (CRB p. 55): it has arms, and none of them is a pincer.
   * Without arms (other packs, or none chosen yet) nothing is known, so this is {@code false}.
   */
  public boolean lacksPincer() {
    return !arms.isEmpty() && !hasPincer();
  }
}
