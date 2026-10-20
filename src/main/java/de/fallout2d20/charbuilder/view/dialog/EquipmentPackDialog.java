package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.equipment.ArmAttachment;
import de.fallout2d20.charbuilder.catalog.equipment.ArmSlot;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentCatalog;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentPackData;
import de.fallout2d20.charbuilder.catalog.equipment.PackChoice;
import de.fallout2d20.charbuilder.catalog.equipment.TagSkillItemData;
import de.fallout2d20.charbuilder.catalog.equipment.TrinketData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.StartingGear;
import de.fallout2d20.charbuilder.model.character.equipment.ArmSelection;
import de.fallout2d20.charbuilder.model.character.equipment.PackInventory;
import de.fallout2d20.charbuilder.model.character.equipment.PackItems;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipmentItems;
import de.fallout2d20.charbuilder.model.character.equipment.StartingSource;
import de.fallout2d20.charbuilder.view.DisplayConverter;
import de.fallout2d20.charbuilder.view.Layouts;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The starting equipment (CRB p. 50, step 6) in a dialog (user decision 2026-10-19): the pack offered for the
 * origin (CRB pp. 76–80), its choices, a Mister Handy's arms (CRB p. 77; or freely configurable arms, a
 * house-rule option), the personal trinket with a roll on the trinket table (CRB p. 80) and the Tag skill items
 * that offer a choice (CRB p. 81), with a preview of what gets entered into the inventory. Picks change the
 * character at once and rebuild the controls; Cancel restores the selection from before, OK enters the
 * starting equipment ({@link StartingGear#apply(StartingEquipment)}).
 */
public final class EquipmentPackDialog {

  private static final String FREE_ARMS_TOOLTIP =
      "House rule: any arm attachment, or a weapon of your choice, on each arm.";
  private static final String GM_PERMISSION = " (GM permission)";
  private static final double PREVIEW_WIDTH = 520;

  private final @NonNull PlayerCharacter character;
  private final @NonNull MigPane controls = Layouts.details(2);
  private final @NonNull Label preview = new Label();

  private EquipmentPackDialog(@NonNull PlayerCharacter character) {
    this.character = character;
  }

  /**
   * Shows the dialog; on OK enters the starting equipment, after creation (GM mode) only once the replaced
   * entries are confirmed. A character whose pack items an earlier version added keeps its inventory and only
   * stores the selection.
   *
   * @return whether the character changed
   */
  public static boolean show(@Nullable Window owner, @NonNull PlayerCharacter character) {
    StartingEquipment snapshot = character.startingGear().getSelection();
    EquipmentPackDialog content = new EquipmentPackDialog(character);
    content.rebuild();

    Label info = new Label(info(character));
    info.setWrapText(true);
    info.setMaxWidth(PREVIEW_WIDTH);
    content.preview.setWrapText(true);
    content.preview.setMaxWidth(PREVIEW_WIDTH);
    VBox box = new VBox(12, content.controls, info, new Label("Will be entered:"), content.preview);
    ScrollPane scroll = new ScrollPane(box);
    scroll.setFitToWidth(true);
    scroll.setPrefSize(640, 560);

    Dialog<Boolean> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle("Equipment pack");
    dialog.setHeaderText("Choose the starting equipment (CRB p. 50).");
    dialog.getDialogPane().setContent(scroll);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dialog.setResultConverter(button -> button == ButtonType.OK);
    if (!dialog.showAndWait().orElse(false)) {
      character.startingGear().restore(snapshot);
      return false;
    }
    if (!character.startingGear().canApply()) {
      return true;
    }
    // After creation (GM mode) always ask: entries are replaced, or – for a character whose pack items were typed
    // in by hand before this version – added a second time.
    if (character.isCreationComplete() && !ConfirmDialog.confirm(owner, "Equipment pack",
        "Enter the starting equipment again?", replacement(character), "Enter")) {
      character.startingGear().restore(snapshot);
      return false;
    }
    character.startingGear().apply(snapshot);
    return true;
  }

  /** What a re-entry after creation replaces and adds. */
  private static @NonNull String replacement(@NonNull PlayerCharacter character) {
    List<String> replaced = character.startingGear().entryNames();
    return (replaced.isEmpty() ? "No entries are marked as starting equipment, so nothing is replaced."
        : "These entries are replaced, with their quantities and notes:\n"
            + replaced.stream().map(name -> "• " + name).collect(Collectors.joining("\n")))
        + "\n\nAdded:\n" + preview(character);
  }

  /** What OK does with the inventory. */
  private static @NonNull String info(@NonNull PlayerCharacter character) {
    if (!character.startingGear().canApply()) {
      return "The pack items of this character were added by an earlier version; changes here do not touch the "
          + "inventory.";
    }
    return "OK enters the starting equipment into the weapons, apparel and gear. Entries of an earlier pick are "
        + "replaced (with their quantities and notes); entries you added yourself stay. Amounts to roll become "
        + "lines in the gear notes" + (character.isCreationComplete() ? "." : "; the caps become the fixed "
        + "starting caps.");
  }

  /** Rebuilds the controls (after a pick) and the preview. */
  private void rebuild() {
    controls.getChildren().clear();
    addPackControls();
    addTagSkillItemChoices();
    updatePreview();
  }

  private void addPackControls() {
    CatalogRef origin = character.getOrigin();
    controls.add(new Label("Equipment pack"));
    if (origin == null) {
      controls.add(new Label("Choose an origin first."), Layouts.span(3));
      return;
    }
    List<EquipmentPackData> packs = EquipmentCatalog.packsFor(origin.id(), character.getEnabledBooks());
    Optional<EquipmentPackData> chosen = character.startingGear().getPack();
    if (packs.isEmpty() && chosen.isEmpty()) {
      controls.add(new Label("No equipment packs for this origin yet."), Layouts.span(3));
      return;
    }
    ComboBox<EquipmentPackData> pack = new ComboBox<>();
    pack.getItems().setAll(packs);
    pack.setConverter(new DisplayConverter<>(data -> data.name()
        + (data.needsGmPermission(origin.id()) ? GM_PERMISSION : "")));
    pack.setPromptText("Choose an equipment pack");
    chosen.ifPresent(data -> {
      pack.setValue(data);
      pack.setTooltip(new Tooltip(describe(data, origin.id())));
    });
    pack.valueProperty().addListener((_, _, data) -> {
      if (data != null) {
        character.startingGear().choosePack(CatalogRef.of(data.id()));
        rebuild();
      }
    });
    controls.add(pack, Layouts.span(3));
    chosen.ifPresent(this::addPackChoices);
  }

  private void addPackChoices(@NonNull EquipmentPackData pack) {
    StartingEquipment selection = character.startingGear().getSelection();
    List<PackChoice> choices = pack.choices();
    for (int i = 0; i < choices.size(); i++) {
      int index = i;
      controls.add(new Label(choices.get(i).label()));
      controls.add(choiceBox(choices.get(i).options(), selection.choices().get(i), option -> {
        character.startingGear().chooseOption(index, option);
        rebuild();
      }), Layouts.span(3));
    }
    if (!pack.arms().isEmpty()) {
      addArms(pack);
    }
    if (pack.trinket()) {
      addTrinket();
    }
  }

  private void addArms(@NonNull EquipmentPackData pack) {
    StartingEquipment selection = character.startingGear().getSelection();
    CheckBox free = new CheckBox("Freely configurable arms");
    free.setSelected(selection.freeArms());
    free.setTooltip(new Tooltip(FREE_ARMS_TOOLTIP));
    free.selectedProperty().addListener((_, _, value) -> {
      character.startingGear().setFreeArms(value);
      rebuild();
    });
    controls.add(new Label("Arms"));
    controls.add(free, Layouts.span(3));
    for (int slot = 0; slot < selection.arms().size(); slot++) {
      int index = slot;
      ArmSelection arm = selection.arms().get(slot);
      ArmSlot allowed = pack.arms().get(slot);
      controls.add(new Label("Arm " + (slot + 1)));
      if (selection.freeArms()) {
        addFreeArm(index, arm);
      } else if (allowed.fixed()) {
        controls.add(new Label(allowed.options().getFirst().displayName()), Layouts.span(3));
      } else {
        controls.add(armBox(allowed.options(), arm.attachment(), attachment -> {
          character.startingGear()
              .chooseArm(index, attachment == null ? ArmSelection.OPEN : ArmSelection.of(attachment));
          rebuild();
        }), Layouts.span(3));
      }
    }
  }

  /** Free arm: an attachment from the table, or a weapon typed in (the text replaces the attachment). */
  private void addFreeArm(int slot, @NonNull ArmSelection arm) {
    ComboBox<ArmAttachment> attachment = armBox(Arrays.asList(ArmAttachment.values()), arm.attachment(), value -> {
      if (value != null) {
        character.startingGear().chooseArm(slot, ArmSelection.of(value));
        rebuild();
      }
    });
    TextField weapon = new TextField(arm.weapon() == null ? "" : arm.weapon());
    weapon.setPromptText("or a weapon, e.g. Hunting rifle");
    weapon.textProperty().addListener((_, _, text) -> {
      // Blank text does not replace a chosen attachment.
      if (text.isBlank() && attachment.getValue() != null) {
        return;
      }
      // Typed text goes to the model at once; only the preview follows (a rebuild would steal the focus).
      character.startingGear().chooseArm(slot, ArmSelection.ofWeapon(text));
      attachment.setValue(null);
      updatePreview();
    });
    controls.add(attachment);
    controls.add(weapon, Layouts.span(2));
  }

  private void addTrinket() {
    String current = character.startingGear().getSelection().trinket();
    TextField trinket = new TextField(current == null ? "" : current);
    trinket.setPromptText("Describe your personal trinket");
    trinket.textProperty().addListener((_, _, text) -> {
      character.startingGear().setTrinket(text);
      updatePreview();
    });
    Button roll = new Button();
    roll.setGraphic(new FontIcon("fas-dice-d20"));
    roll.setTooltip(new Tooltip("Roll on the trinket table (CRB p. 80)"));
    roll.setOnAction(_ -> {
      TrinketData rolled = EquipmentCatalog.trinket(ThreadLocalRandom.current().nextInt(1, TrinketData.DIE_SIZE + 1));
      character.startingGear().setTrinket(rolled.name());
      rebuild();
    });
    controls.add(new Label("Personal trinket"));
    controls.add(trinket, Layouts.span(2));
    controls.add(roll, Layouts.cell().alignX("left"));
  }

  /** CRB p. 81: Tag skills whose items offer a choice. */
  private void addTagSkillItemChoices() {
    StartingEquipmentItems.taggedSkillItems(character).filter(TagSkillItemData::isChoice).forEach(items -> {
      controls.add(new Label(items.skill().displayName() + " items"));
      String picked = character.startingGear().getSelection().tagSkillItems().get(items.skill());
      controls.add(choiceBox(items.options(), picked,
          option -> {
            character.startingGear().chooseTagSkillItem(items.skill(), option);
            rebuild();
          }), Layouts.span(3));
    });
  }

  private void updatePreview() {
    preview.setText(preview(character));
  }

  /** The entries OK makes, the amounts to roll and the starting caps. */
  private static @NonNull String preview(@NonNull PlayerCharacter character) {
    List<String> weapons = new ArrayList<>();
    List<String> apparel = new ArrayList<>();
    List<String> items = new ArrayList<>();
    List<String> rolls = new ArrayList<>();
    if (character.startingGear().getSelection().pack() != null) {
      for (StartingSource source : StartingSource.values()) {
        PackItems entries = PackInventory.resolve(character, source);
        entries.weapons().forEach(weapon -> weapons.add(weapon.displayName() + quantity(weapon.quantity())));
        entries.apparel().forEach(item -> apparel.add(item.displayName()));
        entries.items().forEach(item -> items.add(item.displayName() + quantity(item.quantity())));
        rolls.addAll(entries.rolls());
      }
    }
    return String.join("\n", line("Weapons", weapons), line("Apparel", apparel), line("Gear", items),
        line("Gear notes (to roll)", rolls), "Starting caps: " + StartingEquipmentItems.caps(character));
  }

  private static @NonNull String line(@NonNull String name, @NonNull List<String> entries) {
    return name + ": " + (entries.isEmpty() ? "–" : String.join(", ", entries));
  }

  private static @NonNull String quantity(int quantity) {
    return quantity > 1 ? " [" + quantity + "]" : "";
  }

  /** A choice box over item texts; picking calls {@code onPick}. */
  private static @NonNull ComboBox<String> choiceBox(@NonNull List<String> options, @Nullable String value,
      @NonNull Consumer<String> onPick) {
    ComboBox<String> box = new ComboBox<>();
    box.getItems().setAll(options);
    box.setPromptText("Choose");
    box.setValue(value);
    box.valueProperty().addListener((_, _, option) -> {
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
    box.valueProperty().addListener((_, _, attachment) -> onPick.accept(attachment));
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
