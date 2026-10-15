package de.fallout2d20.charbuilder.catalog.equipment;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.CatalogReader;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.jspecify.annotations.NonNull;

/**
 * The starting equipment catalog: the only place equipment packs, Tag skill items and the trinket
 * table are loaded. The data files are read once, when the class is first used.
 *
 * <p>Besides the per-entry validation of the records, loading checks that pack ids are unique, that
 * every skill has exactly one Tag skill items row (CRB p. 81) and that the trinket table covers every
 * d20 result (CRB p. 80).
 */
public final class EquipmentCatalog {

  private static final @NonNull String PACKS = "/de/fallout2d20/charbuilder/catalog/equipment_packs.json";
  private static final @NonNull String TAG_SKILL_ITEMS =
      "/de/fallout2d20/charbuilder/catalog/tag_skill_items.json";
  private static final @NonNull String TRINKETS = "/de/fallout2d20/charbuilder/catalog/trinkets.json";

  private static final @NonNull Map<String, EquipmentPackData> PACKS_BY_ID = loadPacks();
  private static final @NonNull Map<Skill, TagSkillItemData> TAG_ITEMS_BY_SKILL = loadTagSkillItems();
  private static final @NonNull List<TrinketData> TRINKET_TABLE = loadTrinkets();

  private EquipmentCatalog() {}

  /** All equipment packs in catalog order. */
  public static @NonNull List<EquipmentPackData> packs() {
    return List.copyOf(PACKS_BY_ID.values());
  }

  public static @NonNull Optional<EquipmentPackData> packById(@NonNull String id) {
    return Optional.ofNullable(PACKS_BY_ID.get(id));
  }

  /**
   * Packs a character of the origin may take (including those needing the GM's permission) from the
   * enabled books, in catalog order.
   */
  public static @NonNull List<EquipmentPackData> packsFor(@NonNull String originId, @NonNull Set<Book> books) {
    return PACKS_BY_ID.values().stream()
        .filter(pack -> pack.isOfferedTo(originId) && pack.isAvailableWith(books))
        .toList();
  }

  /** The items the Tag skill grants at creation (CRB p. 81). */
  public static @NonNull TagSkillItemData tagSkillItems(@NonNull Skill skill) {
    // Loading guarantees a row for every skill.
    return Objects.requireNonNull(TAG_ITEMS_BY_SKILL.get(skill));
  }

  /**
   * The trinket for a d20 result (CRB p. 80).
   *
   * @throws IllegalArgumentException if the roll is not between 1 and 20
   */
  public static @NonNull TrinketData trinket(int roll) {
    if (roll < 1 || roll > TrinketData.DIE_SIZE) {
      throw new IllegalArgumentException("A d20 roll lies between 1 and 20: " + roll);
    }
    return TRINKET_TABLE.get(roll - 1);
  }

  private static @NonNull Map<String, EquipmentPackData> loadPacks() {
    Map<String, EquipmentPackData> byId = new LinkedHashMap<>();
    for (EquipmentPackData pack : CatalogReader.readList(PACKS, EquipmentPackData.class)) {
      if (byId.put(pack.id(), pack) != null) {
        throw new IllegalStateException("Duplicate equipment pack id in " + PACKS + ": " + pack.id());
      }
    }
    return byId;
  }

  private static @NonNull Map<Skill, TagSkillItemData> loadTagSkillItems() {
    Map<Skill, TagSkillItemData> bySkill = new EnumMap<>(Skill.class);
    for (TagSkillItemData row : CatalogReader.readList(TAG_SKILL_ITEMS, TagSkillItemData.class)) {
      if (bySkill.put(row.skill(), row) != null) {
        throw new IllegalStateException("Duplicate Tag skill items row in " + TAG_SKILL_ITEMS + ": "
            + row.skill());
      }
    }
    if (bySkill.size() != Skill.values().length) {
      throw new IllegalStateException(TAG_SKILL_ITEMS + " needs one row per skill");
    }
    return bySkill;
  }

  private static @NonNull List<TrinketData> loadTrinkets() {
    List<TrinketData> table = CatalogReader.readList(TRINKETS, TrinketData.class);
    boolean complete = table.size() == TrinketData.DIE_SIZE
        && IntStream.range(0, table.size()).allMatch(i -> table.get(i).roll() == i + 1);
    if (!complete) {
      throw new IllegalStateException(TRINKETS + " must list the rolls 1 to 20 in order");
    }
    return table;
  }
}
