package de.fallout2d20.charbuilder.persistence.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fallout2d20.charbuilder.catalog.equipment.ArmAttachment;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentCatalog;
import de.fallout2d20.charbuilder.catalog.equipment.EquipmentPackData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.equipment.ArmSelection;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Reads the starting equipment of a legacy save from the kept legacy fields: {@code equipmentPack} (the
 * pack name), {@code trinket}, {@code equipment} (free text, becomes the inventory notes) and the
 * {@code originOptions} of a Mister Handy ({@code attachments}, {@code noPincer}). Every field it maps
 * is taken out of the payload; blank values are dropped, everything else that cannot be mapped stays
 * there and is reported. The pack's choices are only part of the legacy text, so they are not recovered.
 */
final class LegacyEquipmentImport {

  private static final String PACK = "equipmentPack";
  private static final String TRINKET = "trinket";
  private static final String EQUIPMENT = "equipment";
  private static final String ORIGIN_OPTIONS = "originOptions";
  private static final String ATTACHMENTS = "attachments";
  private static final String NO_PINCER = "noPincer";
  private static final String MISTER_HANDY = "mister-handy";
  private static final int[][] SLOT_ORDERS = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};

  private final @NonNull ObjectNode payload;
  private final @NonNull List<ImportWarning> warnings;

  LegacyEquipmentImport(@NonNull ObjectNode payload, @NonNull List<ImportWarning> warnings) {
    this.payload = payload;
    this.warnings = warnings;
  }

  /** The legacy free-text equipment, kept verbatim as inventory notes. */
  @NonNull String inventoryNotes() {
    JsonNode node = payload.get(EQUIPMENT);
    if (node == null || !node.isTextual()) {
      return "";
    }
    payload.remove(EQUIPMENT);
    return node.asText();
  }

  /**
   * Pack (by its book name), trinket and – for a Mister Handy – the arms. The trinket is only taken over
   * with a known pack; otherwise it would be invisible (it only counts for a pack that has one).
   */
  @NonNull StartingEquipment startingEquipment(@NonNull CatalogRef origin) {
    String packName = text(PACK);
    Optional<EquipmentPackData> pack = pack(packName);
    String trinket = text(TRINKET);
    if (pack.isEmpty()) {
      trinket = null;
    } else if (trinket != null) {
      payload.remove(TRINKET);
    }
    List<ArmSelection> arms = new ArrayList<>();
    boolean freeArms = arms(origin, pack, arms);
    CatalogRef packRef = pack.map(data -> new CatalogRef(data.id(), packName)).orElse(null);
    return new StartingEquipment(packRef, List.of(), Map.of(), trinket, freeArms, arms);
  }

  private @NonNull Optional<EquipmentPackData> pack(@Nullable String name) {
    if (name == null) {
      return Optional.empty();
    }
    Optional<EquipmentPackData> pack = EquipmentCatalog.packs().stream()
        .filter(data -> data.name().equalsIgnoreCase(name))
        .findFirst();
    if (pack.isPresent()) {
      payload.remove(PACK);
    } else {
      warn(PACK, "Unknown equipment pack \"" + name + "\"; kept in the legacy data.");
    }
    return pack;
  }

  /**
   * Fills the Mister Handy's arms and returns whether they are freely configurable. Legacy arms that fit
   * the pack – each on an arm that allows it, in any order, missing ones only on choosable arms – keep
   * the pack's arms (CRB p. 77); others become free arms. Without legacy arms the pack's arms are used.
   * "No pincer" is not stored any more; it follows from the arms (CRB p. 55).
   */
  private boolean arms(@NonNull CatalogRef origin, @NonNull Optional<EquipmentPackData> pack,
      @NonNull List<ArmSelection> arms) {
    if (!(payload.get(ORIGIN_OPTIONS) instanceof ObjectNode options)) {
      return false;
    }
    boolean noPincer = takeNoPincer(options);
    List<@Nullable String> names = attachmentNames(options);
    boolean anyArm = names.stream().anyMatch(Objects::nonNull);
    boolean readable = options.path(ATTACHMENTS).isArray() || !options.has(ATTACHMENTS);
    boolean misterHandy = origin.id().equals(MISTER_HANDY);
    boolean freeArms = false;
    if (!misterHandy) {
      if (noPincer || anyArm) {
        info(ORIGIN_OPTIONS, "Pincer and arm attachments only apply to a Mister Handy; ignored.");
      }
    } else if (pack.isEmpty() || pack.get().arms().isEmpty()) {
      if (anyArm) {
        warn(ORIGIN_OPTIONS, "Arm attachments need a Mister Handy equipment pack; kept in the legacy data.");
        readable = false;
      }
    } else {
      if (readable) {
        options.remove(ATTACHMENTS);
        readable = false;
      }
      // fillArms may put the unknown attachments back.
      freeArms = fillArms(pack.get(), names, options, arms);
    }
    if (readable) {
      options.remove(ATTACHMENTS);
    }
    if (noPincer && misterHandy) {
      reportNoPincer(arms);
    }
    if (options.isEmpty()) {
      payload.remove(ORIGIN_OPTIONS);
    }
    return freeArms;
  }

  /** Reads and removes the legacy "no pincer" flag; it follows from the arms now. */
  private static boolean takeNoPincer(@NonNull ObjectNode options) {
    JsonNode noPincerNode = options.get(NO_PINCER);
    if (noPincerNode == null || !noPincerNode.isBoolean()) {
      return false;
    }
    options.remove(NO_PINCER);
    return noPincerNode.asBoolean();
  }

  /** A Mister Handy's legacy "no pincer" against the imported arms: a pincer arm wins, without arms it is lost. */
  private void reportNoPincer(@NonNull List<ArmSelection> arms) {
    if (arms.stream().anyMatch(ArmSelection::isPincer)) {
      info(ORIGIN_OPTIONS, "\"No pincer\" contradicts the pincer arm; the arms count.");
    } else if (arms.isEmpty()) {
      info(ORIGIN_OPTIONS, "\"No pincer\" cannot be kept without arms; ignored.");
    }
  }

  /**
   * The legacy arm names by position; blank entries (an arm not chosen yet) are {@code null}, entries
   * that are no text are kept as their JSON text (and reported as unknown). An {@code attachments} value
   * that is no array gives no names.
   */
  private static @NonNull List<@Nullable String> attachmentNames(@NonNull ObjectNode options) {
    List<@Nullable String> names = new ArrayList<>();
    JsonNode attachments = options.get(ATTACHMENTS);
    if (attachments != null && attachments.isArray()) {
      for (JsonNode node : attachments) {
        if (!node.isTextual()) {
          names.add(node.toString());
        } else {
          names.add(node.asText().isBlank() ? null : node.asText());
        }
      }
    }
    return names;
  }

  private boolean fillArms(@NonNull EquipmentPackData pack, @NonNull List<@Nullable String> names,
      @NonNull ObjectNode options, @NonNull List<ArmSelection> arms) {
    List<@Nullable ArmAttachment> byPosition = new ArrayList<>();
    List<ArmAttachment> known = new ArrayList<>();
    List<String> unknown = new ArrayList<>();
    for (String name : names) {
      ArmAttachment attachment = name == null ? null : ArmAttachment.fromDisplayName(name).orElse(null);
      if (attachment != null) {
        known.add(attachment);
      } else if (name != null) {
        unknown.add(name);
      }
      byPosition.add(attachment);
    }
    if (!unknown.isEmpty()) {
      ArrayNode kept = options.putArray(ATTACHMENTS);
      unknown.forEach(kept::add);
      warn(ORIGIN_OPTIONS, "Unknown arm attachments kept in the legacy data: " + String.join(", ", unknown) + ".");
    }
    List<ArmSelection> fitting = fitPack(pack, known);
    if (fitting != null) {
      arms.addAll(fitting);
      return false;
    }
    // Free arms keep the legacy positions when the file has one entry per arm, else the given order.
    List<@Nullable ArmAttachment> source = byPosition.size() == EquipmentPackData.ARM_COUNT ? byPosition
        : new ArrayList<>(known);
    for (int slot = 0; slot < EquipmentPackData.ARM_COUNT; slot++) {
      ArmAttachment attachment = slot < source.size() ? source.get(slot) : null;
      arms.add(attachment == null ? ArmSelection.OPEN : ArmSelection.of(attachment));
    }
    info(ORIGIN_OPTIONS, "The arms differ from the " + pack.name() + " pack; imported as freely configurable arms.");
    return true;
  }

  /**
   * The pack's arms with the legacy attachments placed on arms that allow them (each on its own arm, the
   * legacy order tried first), or {@code null} if they do not fit. Arms left without a legacy attachment
   * get the pack's fixed attachment or stay open (see {@link StartingEquipment#packArms}).
   */
  private static @Nullable List<ArmSelection> fitPack(@NonNull EquipmentPackData pack,
      @NonNull List<ArmAttachment> attachments) {
    if (attachments.size() > EquipmentPackData.ARM_COUNT) {
      return null;
    }
    for (int[] order : SLOT_ORDERS) {
      List<ArmSelection> placed = new ArrayList<>(Collections.nCopies(EquipmentPackData.ARM_COUNT, ArmSelection.OPEN));
      boolean fits = true;
      for (int i = 0; i < attachments.size() && fits; i++) {
        fits = pack.arms().get(order[i]).allows(attachments.get(i));
        placed.set(order[i], ArmSelection.of(attachments.get(i)));
      }
      if (fits) {
        return StartingEquipment.packArms(pack, placed);
      }
    }
    return null;
  }

  /** Stripped text of a payload field; blank text is removed, other non-text values stay and count as kept. */
  private @Nullable String text(@NonNull String field) {
    JsonNode node = payload.get(field);
    if (node == null || node.isNull() || (node.isTextual() && node.asText().isBlank())) {
      payload.remove(field);
      return null;
    }
    return node.isTextual() ? node.asText().strip() : null;
  }

  private void info(@NonNull String field, @NonNull String message) {
    warnings.add(new ImportWarning(WarningSeverity.INFO, field, message));
  }

  private void warn(@NonNull String field, @NonNull String message) {
    warnings.add(new ImportWarning(WarningSeverity.WARNING, field, message));
  }
}
