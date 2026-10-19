package de.fallout2d20.charbuilder.model.character.item;

import de.fallout2d20.charbuilder.catalog.item.ItemCatalog;
import de.fallout2d20.charbuilder.catalog.item.ItemData;
import de.fallout2d20.charbuilder.catalog.item.MagazineData;
import de.fallout2d20.charbuilder.catalog.item.MagazineIssue;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.equipment.StartingSource;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An item in the inventory: a catalog item or magazine, a custom one or a holotape (exactly one of the
 * three), the issue of a magazine (CRB p. 172: one issue per copy), how many of it, a free note, and
 * {@code startingSource} for an item of the starting equipment ({@code null}: added by hand).
 */
public record CarriedItem(
    @Nullable CatalogRef item,
    @Nullable CustomItem custom,
    @Nullable Holotape holotape,
    @Nullable String issue,
    int quantity,
    @NonNull String notes,
    @Nullable StartingSource startingSource) {

  public CarriedItem {
    if (Stream.of(item, custom, holotape).filter(Objects::nonNull).count() != 1) {
      throw new IllegalArgumentException("A carried item is either a catalog item, a custom one or a holotape");
    }
    if (quantity < 1) {
      throw new IllegalArgumentException("Quantity must be at least 1");
    }
    notes = notes == null ? "" : notes;
  }

  /** One catalog item or magazine without issue and note. */
  public static @NonNull CarriedItem of(@NonNull CatalogRef item) {
    return new CarriedItem(item, null, null, null, 1, "", null);
  }

  /** One custom item without note. */
  public static @NonNull CarriedItem of(@NonNull CustomItem custom) {
    return new CarriedItem(null, custom, null, null, 1, "", null);
  }

  /** One holotape without note. */
  public static @NonNull CarriedItem of(@NonNull Holotape holotape) {
    return new CarriedItem(null, null, holotape, null, 1, "", null);
  }

  /** The catalog item; empty for a magazine, a custom item or an id outside the catalog. */
  public @NonNull Optional<ItemData> data() {
    return item == null ? Optional.empty() : ItemCatalog.byId(item.id());
  }

  /** The catalog publication; empty for anything else. */
  public @NonNull Optional<MagazineData> magazine() {
    return item == null ? Optional.empty() : ItemCatalog.magazineById(item.id());
  }

  public @NonNull String displayName() {
    if (custom != null) {
      return custom.name();
    }
    if (holotape != null) {
      return holotape.name();
    }
    return data().map(ItemData::name).or(() -> magazine().map(MagazineData::name))
        .orElseGet(() -> item == null ? "" : item.displayName());
  }

  /** Weight of one piece as printed ({@code <1} for magazines), {@code null} if none or unknown. */
  public @Nullable String printedWeight() {
    if (custom != null) {
      return Integer.toString(custom.weight());
    }
    if (holotape != null) {
      return "0";
    }
    if (magazine().isPresent()) {
      return MagazineData.WEIGHT;
    }
    return data().map(ItemData::weight).orElse(null);
  }

  /**
   * The effect: the issue's for a magazine with a known issue, else the publication's, the item's or the custom
   * text; a holotape's label.
   */
  public @NonNull String effects() {
    if (custom != null) {
      return custom.effects();
    }
    if (holotape != null) {
      return holotape.label();
    }
    Optional<MagazineData> publication = magazine();
    return publication.map(magazineData -> magazineData.issue(issue).map(MagazineIssue::effect).orElse(magazineData.summary()))
        .orElseGet(() -> data().map(ItemData::effects).orElse(""));
  }

  public @NonNull CarriedItem withQuantity(int newQuantity) {
    return new CarriedItem(item, custom, holotape, issue, newQuantity, notes, startingSource);
  }

  public @NonNull CarriedItem withIssue(@Nullable String newIssue) {
    return new CarriedItem(item, custom, holotape, newIssue, quantity, notes, startingSource);
  }

  public @NonNull CarriedItem withNotes(@Nullable String newNotes) {
    return new CarriedItem(item, custom, holotape, issue, quantity, newNotes == null ? "" : newNotes, startingSource);
  }

  /**
   * The holotape renamed or relabelled.
   *
   * @throws IllegalStateException if this is no holotape
   */
  public @NonNull CarriedItem withHolotape(@NonNull Holotape newHolotape) {
    if (holotape == null) {
      throw new IllegalStateException("Only a holotape can be renamed or relabelled");
    }
    return new CarriedItem(null, null, newHolotape, issue, quantity, notes, startingSource);
  }

  /** The item marked as part of the starting equipment; {@code null} for one added by hand. */
  public @NonNull CarriedItem withStartingSource(@Nullable StartingSource source) {
    return new CarriedItem(item, custom, holotape, issue, quantity, notes, source);
  }
}
