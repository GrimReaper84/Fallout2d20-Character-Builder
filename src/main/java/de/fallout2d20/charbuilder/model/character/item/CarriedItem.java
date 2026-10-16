package de.fallout2d20.charbuilder.model.character.item;

import de.fallout2d20.charbuilder.catalog.item.ItemCatalog;
import de.fallout2d20.charbuilder.catalog.item.ItemData;
import de.fallout2d20.charbuilder.catalog.item.MagazineData;
import de.fallout2d20.charbuilder.catalog.item.MagazineIssue;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An item in the inventory: a catalog item or magazine, or a custom one (exactly one of both), the issue
 * of a magazine (CRB p. 172: one issue per copy), how many of it and a free note.
 */
public record CarriedItem(
    @Nullable CatalogRef item,
    @Nullable CustomItem custom,
    @Nullable String issue,
    int quantity,
    @NonNull String notes) {

  public CarriedItem {
    if ((item == null) == (custom == null)) {
      throw new IllegalArgumentException("A carried item is either a catalog item or a custom one");
    }
    if (quantity < 1) {
      throw new IllegalArgumentException("Quantity must be at least 1");
    }
    notes = notes == null ? "" : notes;
  }

  /** One catalog item or magazine without issue and note. */
  public static @NonNull CarriedItem of(@NonNull CatalogRef item) {
    return new CarriedItem(item, null, null, 1, "");
  }

  /** One custom item without note. */
  public static @NonNull CarriedItem of(@NonNull CustomItem custom) {
    return new CarriedItem(null, custom, null, 1, "");
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
    return data().map(ItemData::name).or(() -> magazine().map(MagazineData::name))
        .orElseGet(() -> item == null ? "" : item.displayName());
  }

  /** Weight of one piece as printed ({@code <1} for magazines), {@code null} if none or unknown. */
  public @Nullable String printedWeight() {
    if (custom != null) {
      return Integer.toString(custom.weight());
    }
    if (magazine().isPresent()) {
      return MagazineData.WEIGHT;
    }
    return data().map(ItemData::weight).orElse(null);
  }

  /** The effect: the issue's for a magazine with a known issue, else the publication's, the item's or the custom text. */
  public @NonNull String effects() {
    if (custom != null) {
      return custom.effects();
    }
    Optional<MagazineData> publication = magazine();
    if (publication.isPresent()) {
      return publication.get().issue(issue).map(MagazineIssue::effect).orElse(publication.get().summary());
    }
    return data().map(ItemData::effects).orElse("");
  }

  public @NonNull CarriedItem withQuantity(int newQuantity) {
    return new CarriedItem(item, custom, issue, newQuantity, notes);
  }

  public @NonNull CarriedItem withIssue(@Nullable String newIssue) {
    return new CarriedItem(item, custom, newIssue, quantity, notes);
  }

  public @NonNull CarriedItem withNotes(@Nullable String newNotes) {
    return new CarriedItem(item, custom, issue, quantity, newNotes == null ? "" : newNotes);
  }
}
