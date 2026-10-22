package de.fallout2d20.charbuilder.catalog.apparel;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.weapon.ModPerk;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One armor or clothing mod (CRB p. 126 Ballistic Weave, pp. 133–135 Material mods, p. 136 Armor Upgrades), all
 * installed with Repair. Weight and cost are the printed values; the doubling on torso armor (CRB pp. 133–135) is
 * applied by the model, the torso-only upgrade rows already show doubled values. Names repeat across tables (the
 * all-location upgrades Laminated, Resin and Microcarbon equal the first Synth materials, as printed), so the id
 * names the table.
 *
 * @param id stable catalog id, {@code <table>-<name>} ({@code upgrade-<name>} for upgrades), referenced by save files
 * @param name name as printed
 * @param table the table of a Material or Ballistic Weave mod; {@code null} for an Armor Upgrade
 * @param sources book and page of the table row
 * @param slot where the mod sits
 * @param placement which pieces an upgrade fits; {@code null} for the other slots
 * @param physical physical damage resistance added
 * @param energy energy damage resistance added
 * @param radiation radiation damage resistance added
 * @param special a rule that changes no value, in own words; {@code null} for none
 * @param shadowed whether the mod makes the piece Shadowed (CRB p. 133 sidebar)
 * @param weight lbs. added as printed; {@code null} for "–"
 * @param cost caps added as printed; {@code null} for "–"
 * @param perks perk ranks needed to install it (information only); empty for none
 */
public record ApparelModData(
    @JsonProperty(required = true) @NonNull String id,
    @JsonProperty(required = true) @NonNull String name,
    @Nullable ApparelModTable table,
    @JsonProperty(required = true) @NonNull List<Source> sources,
    @JsonProperty(required = true) @NonNull ApparelModSlot slot,
    @Nullable ModPlacement placement,
    int physical,
    int energy,
    int radiation,
    @Nullable String special,
    boolean shadowed,
    @Nullable Integer weight,
    @Nullable Integer cost,
    @NonNull List<ModPerk> perks) {

  public ApparelModData {
    if (id == null || id.isBlank() || name == null || name.isBlank() || sources == null || sources.isEmpty()
        || slot == null) {
      throw new IllegalArgumentException("Apparel mod needs id, name, source and slot: " + id);
    }
    boolean upgrade = slot == ApparelModSlot.UPGRADE;
    if (upgrade != (table == null) || upgrade != (placement != null) || (table != null && table.slot() != slot)) {
      throw new IllegalArgumentException("Apparel mod " + id + ": an upgrade has a placement, other mods a table of their slot");
    }
    if (physical < 0 || energy < 0 || radiation < 0 || (weight != null && weight < 0) || (cost != null && cost < 0)) {
      throw new IllegalArgumentException("Invalid values for apparel mod " + id);
    }
    if (special != null && special.isBlank()) {
      special = null;
    }
    sources = List.copyOf(sources);
    perks = perks == null ? List.of() : List.copyOf(perks);
  }

  /** Whether one of the books describing the mod is enabled. */
  public boolean isAvailableWith(@NonNull Set<Book> books) {
    return sources.stream().anyMatch(source -> books.contains(source.book()));
  }
}
