package de.fallout2d20.charbuilder.catalog.weapon;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.CatalogNames;
import de.fallout2d20.charbuilder.catalog.CatalogReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The weapon, weapon mod and ammunition catalogs: the only place they are loaded. The files are read once,
 * when the class is first used; every ammunition a weapon or mod names and every mod a weapon accepts must
 * exist.
 */
public final class WeaponCatalog {

  private static final @NonNull String WEAPONS = "/de/fallout2d20/charbuilder/catalog/weapons.json";
  private static final @NonNull String AMMUNITION = "/de/fallout2d20/charbuilder/catalog/ammunition.json";
  private static final @NonNull Map<String, AmmunitionData> AMMUNITION_BY_ID = loadAmmunition();
  private static final @NonNull String MODS = "/de/fallout2d20/charbuilder/catalog/weapon_mods.json";
  private static final @NonNull Map<String, WeaponModData> MODS_BY_ID = loadMods();
  private static final @NonNull Map<String, WeaponData> BY_ID = loadWeapons();

  private WeaponCatalog() {}

  /** All weapons in catalog (book) order. */
  public static @NonNull List<WeaponData> all() {
    return List.copyOf(BY_ID.values());
  }

  public static @NonNull Optional<WeaponData> byId(@NonNull String id) {
    return Optional.ofNullable(BY_ID.get(id));
  }

  /** Weapons of the enabled books, in catalog order. */
  public static @NonNull List<WeaponData> available(@NonNull Set<Book> books) {
    return BY_ID.values().stream().filter(weapon -> weapon.isAvailableWith(books)).toList();
  }

  /**
   * Weapons a character of this origin can add: those of the enabled books that are not built into other
   * origins ({@link WeaponData#isOfferedTo}), in catalog order.
   */
  public static @NonNull List<WeaponData> offered(@Nullable String originId, @NonNull Set<Book> books) {
    return available(books).stream().filter(weapon -> weapon.isOfferedTo(originId)).toList();
  }

  /** The weapon with this name, legacy name or alias (equipment pack wording), as {@link #byName}. */
  public static @NonNull Optional<WeaponData> byNameOrAlias(@NonNull String name) {
    String key = CatalogNames.normalize(name);
    return byName(name).or(() -> BY_ID.values().stream()
        .filter(weapon -> weapon.aliases().stream().anyMatch(alias -> CatalogNames.normalize(alias).equals(key)))
        .findFirst());
  }

  /** The weapon with this name or legacy name, ignoring case and surrounding blanks. */
  public static @NonNull Optional<WeaponData> byName(@NonNull String name) {
    String key = CatalogNames.normalize(name);
    return BY_ID.values().stream()
        .filter(weapon -> CatalogNames.normalize(weapon.name()).equals(key)
            || weapon.legacyNames().stream().anyMatch(legacy -> CatalogNames.normalize(legacy).equals(key)))
        .findFirst();
  }

  /** All ammunition types in catalog (rarity) order. */
  public static @NonNull List<AmmunitionData> ammunition() {
    return List.copyOf(AMMUNITION_BY_ID.values());
  }

  public static @NonNull Optional<AmmunitionData> ammunitionById(@NonNull String id) {
    return Optional.ofNullable(AMMUNITION_BY_ID.get(id));
  }

  /** All weapon mods in catalog (table) order. */
  public static @NonNull List<WeaponModData> mods() {
    return List.copyOf(MODS_BY_ID.values());
  }

  public static @NonNull Optional<WeaponModData> modById(@NonNull String id) {
    return Optional.ofNullable(MODS_BY_ID.get(id));
  }

  private static @NonNull Map<String, AmmunitionData> loadAmmunition() {
    Map<String, AmmunitionData> byId = new LinkedHashMap<>();
    for (AmmunitionData ammunition : CatalogReader.readList(AMMUNITION, AmmunitionData.class)) {
      if (byId.put(ammunition.id(), ammunition) != null) {
        throw new IllegalStateException("Duplicate ammunition id in " + AMMUNITION + ": " + ammunition.id());
      }
    }
    return byId;
  }

  private static @NonNull Map<String, WeaponModData> loadMods() {
    Map<String, WeaponModData> byId = new LinkedHashMap<>();
    for (WeaponModData mod : CatalogReader.readList(MODS, WeaponModData.class)) {
      for (ModEffect effect : mod.effects()) {
        if (effect.type() == ModEffectType.SET_AMMUNITION && !AMMUNITION_BY_ID.containsKey(effect.text())) {
          throw new IllegalStateException("Mod " + mod.id() + " uses unknown ammunition " + effect.text());
        }
      }
      if (byId.put(mod.id(), mod) != null) {
        throw new IllegalStateException("Duplicate mod id in " + MODS + ": " + mod.id());
      }
    }
    return byId;
  }

  private static @NonNull Map<String, WeaponData> loadWeapons() {
    Map<String, WeaponData> byId = new LinkedHashMap<>();
    for (WeaponData weapon : CatalogReader.readList(WEAPONS, WeaponData.class)) {
      for (String ammunition : weapon.ammunition()) {
        if (!AMMUNITION_BY_ID.containsKey(ammunition)) {
          throw new IllegalStateException("Weapon " + weapon.id() + " uses unknown ammunition " + ammunition);
        }
      }
      for (String mod : weapon.mods()) {
        if (!MODS_BY_ID.containsKey(mod)) {
          throw new IllegalStateException("Weapon " + weapon.id() + " accepts unknown mod " + mod);
        }
      }
      if (byId.put(weapon.id(), weapon) != null) {
        throw new IllegalStateException("Duplicate weapon id in " + WEAPONS + ": " + weapon.id());
      }
    }
    return byId;
  }
}
