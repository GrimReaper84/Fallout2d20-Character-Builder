package de.fallout2d20.charbuilder.model.character.creation;

import de.fallout2d20.charbuilder.catalog.equipment.EquipmentCatalog;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentPackData;
import de.fallout2d20.charbuilder.catalog.equipment.PackChoice;
import de.fallout2d20.charbuilder.catalog.equipment.TagSkillItemData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.equipment.ArmSelection;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipmentItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Checks the starting equipment of a character in creation (CRB p. 50, step 6): a pack offered for the
 * origin (CRB pp. 76–80, Vault Dweller packs for ghouls with GM permission CRB p. 57), every pack choice
 * made, a Mister Handy's arms set as the pack allows (CRB p. 77) unless they are freely configurable, a
 * personal trinket described (CRB p. 80) and a pick for Tag skill items that offer a choice (CRB p. 81).
 * Only characters in creation with an origin are checked; the origin itself is checked by
 * {@link OriginRules}.
 */
public final class EquipmentRules {

  private static final String EQUIPMENT = "Equipment";

  private EquipmentRules() {}

  /** All equipment problems of a character in creation; empty for complete characters. */
  public static @NonNull List<CreationProblem> problems(@NonNull PlayerCharacter character) {
    List<CreationProblem> problems = new ArrayList<>();
    CatalogRef origin = character.getOrigin();
    if (!CreationRules.isChecked(character) || origin == null) {
      return problems;
    }
    StartingEquipment selection = character.getStartingEquipment();
    CatalogRef packRef = selection.pack();
    if (packRef == null) {
      if (!EquipmentCatalog.packsFor(origin.id(), character.getEnabledBooks()).isEmpty()) {
        add(problems, "Choose an equipment pack.");
      }
      return problems;
    }
    Optional<EquipmentPackData> data = character.getEquipmentPack();
    if (data.isEmpty()) {
      add(problems, "\"" + packRef.displayName() + "\" is not an equipment pack of the supported books.");
      return problems;
    }
    EquipmentPackData pack = data.get();
    if (!pack.isOfferedTo(origin.id())) {
      add(problems, pack.name() + " is not an equipment pack for this origin.");
    }
    if (!pack.isAvailableWith(character.getEnabledBooks())) {
      add(problems, pack.name() + " needs a book that is not enabled.");
    }
    checkChoices(pack, selection, problems);
    checkArms(pack, selection, problems);
    if (pack.trinket() && selection.trinket() == null) {
      add(problems, "Describe or roll the personal trinket.");
    }
    StartingEquipmentItems.taggedSkillItems(character)
        .filter(TagSkillItemData::isChoice)
        .filter(items -> selection.tagSkillItems().get(items.skill()) == null)
        .forEach(items -> add(problems, "Choose the Tag skill items for " + items.skill().displayName() + "."));
    return problems;
  }

  private static void checkChoices(@NonNull EquipmentPackData pack, @NonNull StartingEquipment selection,
      @NonNull List<CreationProblem> problems) {
    List<PackChoice> choices = pack.choices();
    for (int i = 0; i < choices.size(); i++) {
      // The character keeps one entry per pack choice (StartingEquipment.fittedToCatalog).
      if (selection.choices().get(i) == null) {
        add(problems, "Choose: " + choices.get(i).label() + " (" + pack.name() + " pack).");
      }
    }
  }

  /** Every arm set; without free arms only the attachments the pack allows on that arm (CRB p. 77). */
  private static void checkArms(@NonNull EquipmentPackData pack, @NonNull StartingEquipment selection,
      @NonNull List<CreationProblem> problems) {
    List<ArmSelection> arms = selection.arms();
    for (int slot = 0; slot < arms.size(); slot++) {
      ArmSelection arm = arms.get(slot);
      String name = "arm " + (slot + 1);
      if (arm.isOpen()) {
        add(problems, "Choose " + name + ".");
      } else if (!selection.freeArms() && !pack.arms().get(slot).allows(arm.attachment())) {
        add(problems, "The " + pack.name() + " pack does not allow this attachment on " + name
            + "; switch on freely configurable arms to keep it.");
      }
    }
  }

  private static void add(@NonNull List<CreationProblem> problems, @NonNull String message) {
    problems.add(new CreationProblem(HintArea.GEAR, EQUIPMENT, message));
  }
}
