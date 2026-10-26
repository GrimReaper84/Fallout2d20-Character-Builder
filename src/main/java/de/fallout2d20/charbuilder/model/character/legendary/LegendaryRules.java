package de.fallout2d20.charbuilder.model.character.legendary;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorLocation;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryCatalog;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponType;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.apparel.ApparelValues;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * Which legendary properties an item can get (WG pp. 96–104) and how a legendary item is named.
 *
 * <p>Weapons: the properties with a d20 result in the weapon's column ({@link CarriedWeapon#legendaryType()});
 * bows, explosives and throwing weapons get none. Where a property's heading names other weapon types than its table
 * columns (Poisoner's, Lucky), the tables decide (assumption, plan F6d). Armor: armor pieces, robot armor and outfits
 * that count as armor, with the properties of any column of the locations they cover (WG p. 104: the player chooses
 * one covered location); clothing, headgear and other outfits get none (assumption). The properties need the
 * Wanderer's Guide (the properties' book must be enabled). In GM mode every property of the kind is offered for any item (user decision 2026-10-23).
 */
public final class LegendaryRules {

  /** The mark before the name of a legendary item (user decision 2026-10-23). */
  private static final @NonNull String MARK = "★";

  private LegendaryRules() {}

  /** The name of a legendary item: mark, property, name, e.g. {@code ★ Two-Shot Hunting Rifle}. */
  public static @NonNull String name(@NonNull String property, @NonNull String name) {
    return MARK + " " + property + " " + name;
  }

  /** The properties the weapon can get, in catalog order; empty if it cannot be legendary. */
  public static @NonNull List<LegendaryWeaponData> options(@NonNull PlayerCharacter character,
      @NonNull CarriedWeapon weapon) {
    if (character.isGmMode()) {
      return LegendaryCatalog.weaponProperties();
    }
    Optional<LegendaryWeaponType> type = weapon.legendaryType();
    Set<Book> books = character.getEnabledBooks();
    return type.map(column -> LegendaryCatalog.weaponProperties().stream()
        .filter(property -> property.isAvailableWith(books) && property.fits(column)).toList()).orElse(List.of());
  }

  /** The properties the piece can get, in catalog order; empty if it cannot be legendary. */
  public static @NonNull List<LegendaryArmorData> options(@NonNull PlayerCharacter character,
      @NonNull WornApparel item) {
    if (character.isGmMode()) {
      return LegendaryCatalog.armorProperties();
    }
    if (!isArmor(item)) {
      return List.of();
    }
    Set<LegendaryArmorLocation> columns = item.legendaryLocations();
    Set<Book> books = character.getEnabledBooks();
    return LegendaryCatalog.armorProperties().stream()
        .filter(property -> property.isAvailableWith(books) && columns.stream().anyMatch(property::fits)).toList();
  }

  /** Whether the piece can be legendary outside GM mode: armor, robot armor and outfits that count as armor. */
  private static boolean isArmor(@NonNull WornApparel item) {
    return item.isArmor() || item.values().map(ApparelValues::kind).filter(ApparelKind.ROBOT_ARMOR::equals).isPresent();
  }
}
