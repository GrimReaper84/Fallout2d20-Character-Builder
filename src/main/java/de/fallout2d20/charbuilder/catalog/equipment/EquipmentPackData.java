package de.fallout2d20.charbuilder.catalog.equipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * A starting equipment pack (catalog entry).
 *
 * <p>CRB p. 50 (creation step 6): a character takes one starting pack offered for its origin; the
 * packs are listed per origin on CRB pp. 76–80. Item lines are short item names with their quantity;
 * Combat Dice amounts stay formulas such as {@code 6 + 3 CD rounds}. The pack's arms, caps and personal
 * trinket are separate fields, not item lines.
 *
 * @param origins ids of the origins the pack is offered to
 * @param originsWithGmPermission ids of further origins that may take the pack with the GM's permission
 *     (ghoul vault dwellers may take a Vault Dweller pack, CRB p. 57)
 * @param items fixed items of the pack
 * @param choices "pick one of" parts of the pack, in book order
 * @param trinket whether the pack includes a personal trinket (CRB p. 80)
 * @param caps caps the pack contains
 * @param arms the three arms of a Mister Handy pack (CRB p. 77); empty for all other packs
 */
public record EquipmentPackData(
    @JsonProperty(required = true) @NonNull String id,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull List<Source> sources,
    @JsonProperty(required = true) @NonNull List<String> origins,
    @NonNull List<String> originsWithGmPermission,
    @JsonProperty(required = true) @NonNull List<String> items,
    @NonNull List<PackChoice> choices,
    boolean trinket,
    int caps,
    @NonNull List<ArmSlot> arms) {

  /** A Mister Handy has three arms (CRB p. 55). */
  public static final int ARM_COUNT = 3;

  public EquipmentPackData {
    if (id == null || id.isBlank() || name == null || name.isBlank() || sources == null
        || sources.isEmpty() || origins == null || origins.isEmpty() || items == null) {
      throw new IllegalArgumentException("Equipment pack needs id, name, source, origins and items: " + id);
    }
    originsWithGmPermission = originsWithGmPermission == null ? List.of() : List.copyOf(originsWithGmPermission);
    choices = choices == null ? List.of() : List.copyOf(choices);
    arms = arms == null ? List.of() : List.copyOf(arms);
    if (caps < 0) {
      throw new IllegalArgumentException("Negative caps in equipment pack " + id);
    }
    if (!arms.isEmpty() && arms.size() != ARM_COUNT) {
      throw new IllegalArgumentException("Equipment pack " + id + " needs no or exactly three arms");
    }
    if (items.stream().anyMatch(String::isBlank)) {
      throw new IllegalArgumentException("Blank item line in equipment pack " + id);
    }
    if (originsWithGmPermission.stream().anyMatch(origins::contains)) {
      throw new IllegalArgumentException(
          "Origin listed both with and without GM permission in equipment pack " + id);
    }
    sources = List.copyOf(sources);
    origins = List.copyOf(origins);
    items = List.copyOf(items);
  }

  /** Whether a character of the origin may take this pack, with or without the GM's permission. */
  public boolean isOfferedTo(@NonNull String originId) {
    return origins.contains(originId) || originsWithGmPermission.contains(originId);
  }

  /** Whether the origin may take this pack only with the GM's permission (CRB p. 57). */
  public boolean needsGmPermission(@NonNull String originId) {
    return originsWithGmPermission.contains(originId);
  }

  /** Whether one of the books describing this pack is enabled. */
  public boolean isAvailableWith(@NonNull Set<Book> books) {
    return sources.stream().anyMatch(source -> books.contains(source.book()));
  }
}
