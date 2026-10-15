package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.equipment.ArmAttachment;
import de.fallout2d20.charbuilder.catalog.equipment.ArmSlot;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentCatalog;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentPackData;
import de.fallout2d20.charbuilder.catalog.equipment.PackChoice;
import de.fallout2d20.charbuilder.catalog.equipment.TagSkillItemData;
import de.fallout2d20.charbuilder.catalog.equipment.TrinketData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.equipment.ArmSelection;
import de.fallout2d20.charbuilder.model.character.equipment.StartingCaps;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipmentItems;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * Starting equipment (CRB p. 50, step 6) and inventory notes. During creation: the pack offered for the
 * origin (CRB pp. 76–80), its choices, a Mister Handy's arms (CRB p. 77; or freely configurable arms, a
 * house-rule option), the personal trinket with a roll on the trinket table (CRB p. 80) and the Tag
 * skill items that offer a choice (CRB p. 81). In every mode: the resulting starting equipment, the
 * starting caps and the editable inventory notes. Picks rebuild the overview; typed text only marks the
 * character as edited.
 */
final class EquipmentSection {

  /** Consequences of a Mister Handy without a pincer (CRB p. 55). */
  private static final String NO_PINCER_TOOLTIP =
      "No unarmed attacks or object handling; no Lockpick, Repair or Throwing tests (CRB p. 55).";
  private static final String FREE_ARMS_TOOLTIP =
      "House rule: any arm attachment, or a weapon of your choice, on each arm.";
  private static final String GM_PERMISSION = " (GM permission)";

  private EquipmentSection() {}

  /**
   * Adds the equipment rows to a four-column overview grid.
   *
   * @param onEdited called after an edit that needs no rebuild (typed text)
   * @param onChanged called after an edit that changes other values; rebuilds the overview
   */
  static void addTo(@NonNull MigPane pane, @NonNull Character character, @NonNull Runnable onEdited,
      @NonNull Runnable onChanged) {
    if (character.isCreationComplete()) {
      addPackName(pane, character);
    } else {
      addCreationControls(pane, character, onEdited, onChanged);
      addTagSkillItemChoices(pane, character, onChanged);
    }
    if (character.getStartingEquipment().lacksPincer()) {
      Label noPincer = new Label("No pincer");
      noPincer.setTooltip(new Tooltip(NO_PINCER_TOOLTIP));
      pane.add(new Label("Pincer"));
      pane.add(noPincer, "span 3");
    }
    addItemsAndCaps(pane, character);
    TextArea notes = new TextArea(character.getInventoryNotes());
    notes.setPromptText("Gear bought, found or used up, rolled amounts, …");
    notes.setWrapText(true);
    notes.setPrefRowCount(6);
    notes.textProperty().addListener((obs, old, text) -> {
      character.setInventoryNotes(text);
      onEdited.run();
    });
    pane.add(new Label("Inventory notes"), "top");
    pane.add(notes, "span 3, growx");
  }

  private static void addPackName(@NonNull MigPane pane, @NonNull Character character) {
    CatalogRef pack = character.getStartingEquipment().pack();
    if (pack == null) {
      return;
    }
    pane.add(new Label("Equipment pack"));
    pane.add(new Label(character.getEquipmentPack().map(EquipmentPackData::name).orElse(pack.displayName())),
        "span 3");
  }

  private static void addCreationControls(@NonNull MigPane pane, @NonNull Character character,
      @NonNull Runnable onEdited, @NonNull Runnable onChanged) {
    CatalogRef origin = character.getOrigin();
    if (origin == null) {
      pane.add(new Label("Equipment pack"));
      pane.add(new Label("Choose an origin first."), "span 3");
      return;
    }
    List<EquipmentPackData> packs = EquipmentCatalog.packsFor(origin.id(), character.getEnabledBooks());
    Optional<EquipmentPackData> chosen = character.getEquipmentPack();
    pane.add(new Label("Equipment pack"));
    if (packs.isEmpty() && chosen.isEmpty()) {
      pane.add(new Label("No equipment packs for this origin yet."), "span 3");
    } else {
      ComboBox<EquipmentPackData> pack = new ComboBox<>();
      pack.getItems().setAll(packs);
      pack.setConverter(new DisplayConverter<>(data -> data.name()
          + (data.needsGmPermission(origin.id()) ? GM_PERMISSION : "")));
      pack.setPromptText("Choose an equipment pack");
      chosen.ifPresent(data -> {
        pack.setValue(data);
        pack.setTooltip(new Tooltip(describe(data, origin.id())));
      });
      pack.valueProperty().addListener((obs, old, data) -> {
        if (data != null) {
          character.chooseEquipmentPack(CatalogRef.of(data.id()));
          onChanged.run();
        }
      });
      pane.add(pack, "span 3");
    }
    chosen.ifPresent(data -> addPackControls(pane, character, data, onEdited, onChanged));
  }

  private static void addPackControls(@NonNull MigPane pane, @NonNull Character character,
      @NonNull EquipmentPackData pack, @NonNull Runnable onEdited, @NonNull Runnable onChanged) {
    StartingEquipment selection = character.getStartingEquipment();
    List<PackChoice> choices = pack.choices();
    for (int i = 0; i < choices.size(); i++) {
      int index = i;
      pane.add(new Label(choices.get(i).label()));
      pane.add(choiceBox(choices.get(i).options(), selection.choices().get(i), option -> {
        character.chooseEquipmentOption(index, option);
        onChanged.run();
      }), "span 3");
    }
    if (!pack.arms().isEmpty()) {
      addArms(pane, character, pack, onEdited, onChanged);
    }
    if (pack.trinket()) {
      addTrinket(pane, character, onEdited, onChanged);
    }
  }

  private static void addArms(@NonNull MigPane pane, @NonNull Character character,
      @NonNull EquipmentPackData pack, @NonNull Runnable onEdited, @NonNull Runnable onChanged) {
    StartingEquipment selection = character.getStartingEquipment();
    CheckBox free = new CheckBox("Freely configurable arms");
    free.setSelected(selection.freeArms());
    free.setTooltip(new Tooltip(FREE_ARMS_TOOLTIP));
    free.selectedProperty().addListener((obs, old, value) -> {
      character.setFreeArms(value);
      onChanged.run();
    });
    pane.add(new Label("Arms"));
    pane.add(free, "span 3");
    for (int slot = 0; slot < selection.arms().size(); slot++) {
      int index = slot;
      ArmSelection arm = selection.arms().get(slot);
      ArmSlot allowed = pack.arms().get(slot);
      pane.add(new Label("Arm " + (slot + 1)));
      if (selection.freeArms()) {
        addFreeArm(pane, character, index, arm, onEdited, onChanged);
      } else if (allowed.fixed()) {
        pane.add(new Label(allowed.options().get(0).displayName()), "span 3");
      } else {
        pane.add(armBox(allowed.options(), arm.attachment(), attachment -> {
          character.chooseArm(index, attachment == null ? ArmSelection.OPEN : ArmSelection.of(attachment));
          onChanged.run();
        }), "span 3");
      }
    }
  }

  /** Free arm: an attachment from the table, or a weapon typed in (the text replaces the attachment). */
  private static void addFreeArm(@NonNull MigPane pane, @NonNull Character character, int slot,
      @NonNull ArmSelection arm, @NonNull Runnable onEdited, @NonNull Runnable onChanged) {
    ComboBox<ArmAttachment> attachment = armBox(Arrays.asList(ArmAttachment.values()), arm.attachment(), value -> {
      if (value != null) {
        character.chooseArm(slot, ArmSelection.of(value));
        onChanged.run();
      }
    });
    TextField weapon = new TextField(arm.weapon() == null ? "" : arm.weapon());
    weapon.setPromptText("or a weapon, e.g. Hunting rifle");
    weapon.textProperty().addListener((obs, old, text) -> {
      // Blank text does not replace a chosen attachment.
      if (text.isBlank() && attachment.getValue() != null) {
        return;
      }
      // Typed text goes to the model at once (no rebuild, which would steal the focus).
      character.chooseArm(slot, ArmSelection.ofWeapon(text));
      attachment.setValue(null);
      onEdited.run();
    });
    pane.add(attachment);
    pane.add(weapon, "span 2");
  }

  private static void addTrinket(@NonNull MigPane pane, @NonNull Character character,
      @NonNull Runnable onEdited, @NonNull Runnable onChanged) {
    String current = character.getStartingEquipment().trinket();
    TextField trinket = new TextField(current == null ? "" : current);
    trinket.setPromptText("Describe your personal trinket");
    trinket.textProperty().addListener((obs, old, text) -> {
      character.setTrinket(text);
      onEdited.run();
    });
    Button roll = new Button();
    roll.setGraphic(new FontIcon("fas-dice-d20"));
    roll.setTooltip(new Tooltip("Roll on the trinket table (CRB p. 80)"));
    roll.setOnAction(event -> {
      TrinketData rolled = EquipmentCatalog.trinket(ThreadLocalRandom.current().nextInt(1, TrinketData.DIE_SIZE + 1));
      character.setTrinket(rolled.name());
      onChanged.run();
    });
    pane.add(new Label("Personal trinket"));
    pane.add(trinket, "span 2");
    pane.add(roll, "left");
  }

  /** CRB p. 81: Tag skills whose items offer a choice. */
  private static void addTagSkillItemChoices(@NonNull MigPane pane, @NonNull Character character,
      @NonNull Runnable onChanged) {
    StartingEquipmentItems.taggedSkillItems(character).filter(TagSkillItemData::isChoice).forEach(items -> {
      pane.add(new Label(items.skill().displayName() + " items"));
      pane.add(choiceBox(items.options(), character.getStartingEquipment().tagSkillItems().get(items.skill()),
          option -> {
            character.chooseTagSkillItem(items.skill(), option);
            onChanged.run();
          }), "span 3");
    });
  }

  private static void addItemsAndCaps(@NonNull MigPane pane, @NonNull Character character) {
    List<String> lines = StartingEquipmentItems.lines(character);
    if (!lines.isEmpty()) {
      Label items = new Label(lines.stream().map(line -> "• " + line).collect(Collectors.joining("\n")));
      items.setWrapText(true);
      pane.add(new Label("Starting equipment"), "top");
      pane.add(items, "span 3");
    }
    StartingCaps caps = StartingEquipmentItems.caps(character);
    if (character.isCreationComplete() && character.getStartingEquipment().pack() == null && caps.levelCaps() == 0
        && caps.d20() == 0) {
      return;
    }
    Label capsLabel = new Label(caps.toString());
    if (character.getStartingLevel().isPresent()) {
      capsLabel.setTooltip(new Tooltip("Pack: " + caps.packCaps() + ", starting level: " + caps.levelCaps()
          + (caps.d20() == 0 ? "" : ", Tag skill items (Barter): " + caps.d20() + "d20")));
      pane.add(new Label("Starting caps"));
    } else {
      capsLabel.setTooltip(new Tooltip("Caps of the pack and dice of Tag skill items (Barter); extra caps of a "
          + "higher starting level are not included, because the starting level is not known for this character."));
      pane.add(new Label("Starting caps (pack, Tag skills)"));
    }
    pane.add(capsLabel, "span 3");
  }

  /** A choice box over item texts; picking calls {@code onPick}. */
  private static @NonNull ComboBox<String> choiceBox(@NonNull List<String> options, @Nullable String value,
      @NonNull Consumer<String> onPick) {
    ComboBox<String> box = new ComboBox<>();
    box.getItems().setAll(options);
    box.setPromptText("Choose");
    box.setValue(value);
    box.valueProperty().addListener((obs, old, option) -> {
      if (option != null) {
        onPick.accept(option);
      }
    });
    return box;
  }

  private static @NonNull ComboBox<ArmAttachment> armBox(@NonNull List<ArmAttachment> options,
      @Nullable ArmAttachment value, @NonNull Consumer<@Nullable ArmAttachment> onPick) {
    ComboBox<ArmAttachment> box = new ComboBox<>();
    box.getItems().setAll(options);
    box.setConverter(new DisplayConverter<>(ArmAttachment::displayName));
    box.setPromptText("Choose an attachment");
    box.setValue(value);
    box.valueProperty().addListener((obs, old, attachment) -> onPick.accept(attachment));
    return box;
  }

  /** Sources of the pack, e.g. {@code CRB p. 79}, and the GM permission a ghoul vault dweller needs. */
  private static @NonNull String describe(@NonNull EquipmentPackData pack, @NonNull String originId) {
    String sources = pack.sources().stream().map(Source::toString).collect(Collectors.joining(", "));
    return pack.needsGmPermission(originId)
        ? sources + " – with the GM's permission: ghoul vault dwellers may take a Vault Dweller pack (CRB p. 57)"
        : sources;
  }
}
