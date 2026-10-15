package de.fallout2d20.charbuilder.model.character.equipment;

import de.fallout2d20.charbuilder.catalog.equipment.ArmAttachment;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentCatalog;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentPackData;
import de.fallout2d20.charbuilder.catalog.equipment.PackChoice;
import de.fallout2d20.charbuilder.catalog.equipment.TagSkillItemData;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.StartingEquipmentAllowance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;

/**
 * The starting equipment of a character as text lines, rendered from its {@link StartingEquipment}
 * selection (user decision: text until the inventory of phase 7). Combat Dice amounts stay formulas
 * such as {@code 6 + 3 CD} (user decision: rolled at the table).
 *
 * <p>Lines in this order: the pack's items (CRB pp. 76–80), its choices (an open choice says so), the
 * Mister Handy's arms with 20 shots of ammo for each weapon arm (CRB p. 55; also for a freely chosen
 * weapon, user decision), the personal trinket if the pack has one (CRB p. 80), and the items of every
 * Tag skill (CRB p. 81; all Tag sources count – free, origin and Tag! – user decision). Caps are not a
 * line, see {@link #caps(Character)}.
 *
 * <p>The book grants Tag skill items only for Tags chosen during creation. As long as perks can only be
 * taken during creation (and imported Tag! ranks carry no skill), every current Tag qualifies; once
 * level-up perks exist (D1), a Tag! taken later must not count here.
 */
public final class StartingEquipmentItems {

  private static final @NonNull String NOT_CHOSEN = "not chosen yet";
  private static final int WEAPON_ARM_SHOTS = 20;

  private StartingEquipmentItems() {}

  public static @NonNull List<String> lines(@NonNull Character character) {
    StartingEquipment selection = character.getStartingEquipment();
    List<String> lines = new ArrayList<>();
    Optional<EquipmentPackData> pack = character.getEquipmentPack();
    if (pack.isPresent()) {
      lines.addAll(pack.get().items());
      List<PackChoice> choices = pack.get().choices();
      for (int i = 0; i < choices.size(); i++) {
        // The character keeps one entry per pack choice (StartingEquipment.fittedToCatalog).
        String picked = selection.choices().get(i);
        lines.add(picked != null ? picked : choices.get(i).label() + ": " + NOT_CHOSEN);
      }
      for (int slot = 0; slot < selection.arms().size(); slot++) {
        lines.add("Arm " + (slot + 1) + ": " + arm(selection.arms().get(slot)));
      }
      if (pack.get().trinket()) {
        String trinket = selection.trinket();
        lines.add("Personal trinket: " + (trinket != null ? trinket : "not described yet"));
      }
    }
    taggedSkillItems(character).forEach(items -> tagSkillItems(items, selection).ifPresent(lines::add));
    return lines;
  }

  /**
   * Starting caps: pack caps, the d20 of Tag skill items (Barter) and, during creation, the extra caps
   * of the starting level (CRB p. 81). A complete character's level may have grown since creation, so
   * its level part is left out.
   */
  public static @NonNull StartingCaps caps(@NonNull Character character) {
    int packCaps = character.getEquipmentPack().map(EquipmentPackData::caps).orElse(0);
    long levelCaps = character.isCreationComplete() ? 0 : StartingEquipmentAllowance.caps(character.getLevel());
    int d20 = taggedSkillItems(character).mapToInt(TagSkillItemData::capsD20).sum();
    return new StartingCaps(packCaps, levelCaps, d20);
  }

  /**
   * The Tag skill items rows of every skill that grants starting equipment: all Tag skills (see the
   * class comment); the one place deciding which Tags count.
   */
  public static @NonNull Stream<TagSkillItemData> taggedSkillItems(@NonNull Character character) {
    return Arrays.stream(Skill.values()).filter(character::isTagged).map(EquipmentCatalog::tagSkillItems);
  }

  /** E.g. {@code 10mm Auto Pistol, with 20 shots of 10mm}; {@code not chosen yet} for an open arm. */
  private static @NonNull String arm(@NonNull ArmSelection arm) {
    ArmAttachment attachment = arm.attachment();
    if (attachment != null) {
      return attachment.displayName()
          + attachment.ammo().map(ammo -> ", with " + WEAPON_ARM_SHOTS + " shots of " + ammo).orElse("");
    }
    String weapon = arm.weapon();
    return weapon == null ? NOT_CHOSEN : weapon + ", with " + WEAPON_ARM_SHOTS + " shots of its ammunition";
  }

  /** E.g. {@code Lockpick: 4 + 2 CD bobby pins}; empty for a skill that only grants caps (Barter). */
  private static @NonNull Optional<String> tagSkillItems(@NonNull TagSkillItemData items,
      @NonNull StartingEquipment selection) {
    Skill skill = items.skill();
    if (items.options().isEmpty()) {
      return Optional.empty();
    }
    String prefix = skill.displayName() + ": ";
    if (!items.isChoice()) {
      return Optional.of(prefix + items.options().get(0));
    }
    String picked = selection.tagSkillItems().get(skill);
    return Optional.of(prefix + (picked != null ? picked
        : String.join(" or ", items.options()) + " (" + NOT_CHOSEN + ")"));
  }
}
