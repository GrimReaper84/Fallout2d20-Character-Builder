package de.fallout2d20.charbuilder.persistence.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fallout2d20.charbuilder.catalog.weapon.AmmunitionData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import de.fallout2d20.charbuilder.model.character.weapon.CustomWeapon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Reads the legacy {@code weapons} list ({@code name, dmg, eff, type, qual, rate, range, mag, pool}). A
 * weapon whose name (or legacy name) is in the catalog becomes that catalog weapon; legacy values that
 * differ from the book are reported and kept in the weapon's note (they may stem from mods). Any other
 * weapon becomes a custom weapon without skill. The magazine and pool shots are added to the weapon's
 * ammunition type (CRB p. 91: shots per type, user decision); without a known type they go into the
 * note with a warning. Entries without a name stay in the payload as {@code invalidWeapons}; an Unarmed
 * Strike is skipped, every character has it.
 */
final class LegacyWeaponImport {

  private static final @NonNull String WEAPONS = "weapons";
  private static final @NonNull String NO_VALUE = "-";
  /** Every character has it (CRB p. 111), so it is never a carried weapon. */
  private static final @NonNull String UNARMED_STRIKE = "unarmed-strike";

  private final @NonNull List<CarriedWeapon> weapons = new ArrayList<>();
  private final @NonNull Map<String, Integer> ammunition = new LinkedHashMap<>();
  private final @NonNull List<ImportWarning> warnings;

  /** Reads and removes the {@code weapons} field of the payload. */
  LegacyWeaponImport(@NonNull ObjectNode payload, @NonNull List<ImportWarning> warnings) {
    this.warnings = warnings;
    JsonNode node = payload.get(WEAPONS);
    if (!(node instanceof ArrayNode list)) {
      if (node != null) {
        warnings.add(new ImportWarning(WarningSeverity.WARNING, WEAPONS, "Weapons are not a list; kept in the legacy data."));
      }
      return;
    }
    payload.remove(WEAPONS);
    ArrayNode invalid = JsonNodeFactory.instance.arrayNode();
    for (JsonNode item : list) {
      String name = text(item.get("name"));
      if (name.isEmpty()) {
        invalid.add(item);
        continue;
      }
      read(name, item);
    }
    if (!invalid.isEmpty()) {
      payload.set("invalidWeapons", invalid);
      warnings.add(new ImportWarning(WarningSeverity.WARNING, WEAPONS,
          invalid.size() + " weapon entr(y/ies) without a name kept in the legacy data."));
    }
  }

  @NonNull List<CarriedWeapon> weapons() {
    return weapons;
  }

  @NonNull Map<String, Integer> ammunition() {
    return ammunition;
  }

  private void read(@NonNull String name, @NonNull JsonNode item) {
    int shots = nonNegative(item.get("mag")) + nonNegative(item.get("pool"));
    Optional<WeaponData> data = WeaponCatalog.byName(name);
    if (data.isPresent() && data.get().id().equals(UNARMED_STRIKE)) {
      info(name + " skipped: every character has it.");
      return;
    }
    CarriedWeapon weapon;
    List<String> weaponAmmunition;
    if (data.isPresent()) {
      String differences = differences(item, data.get());
      weapon = CarriedWeapon.of(CatalogRef.of(data.get().id()));
      if (!differences.isEmpty()) {
        weapon = weapon.withNotes("Legacy values: " + differences);
        info(name + ": legacy values differ from the book (" + differences + "), kept in the note.");
      }
      weaponAmmunition = data.get().ammunition();
    } else {
      weapon = CarriedWeapon.of(custom(name, item));
      info(name + " is not in the weapon catalog; imported as a custom weapon without skill.");
      weaponAmmunition = List.of();
    }
    if (shots > 0) {
      if (weaponAmmunition.isEmpty()) {
        weapon = weapon.withNotes(join(weapon.notes(), "Legacy ammunition: " + shots + " shots"));
        warnings.add(new ImportWarning(WarningSeverity.WARNING, WEAPONS,
            name + ": ammunition type unknown; " + shots + " shots kept in the weapon's note."));
      } else {
        ammunition.merge(weaponAmmunition.get(0), shots, Integer::sum);
        if (weaponAmmunition.size() > 1) {
          info(name + ": " + shots + " shots counted as " + ammunitionName(weaponAmmunition.get(0))
              + "; the weapon can also use " + weaponAmmunition.subList(1, weaponAmmunition.size()).stream()
                  .map(LegacyWeaponImport::ammunitionName).collect(Collectors.joining(", ")) + ".");
        }
      }
    }
    weapons.add(weapon);
  }

  /** Damage, effects, fire rate and range of the legacy entry that differ from the catalog. */
  private static @NonNull String differences(@NonNull JsonNode item, @NonNull WeaponData data) {
    List<String> differences = new ArrayList<>();
    Integer damage = LegacyNumbers.integer(item.get("dmg"));
    if (damage != null && damage != data.damage()) {
      differences.add("damage " + damage + " CD");
    }
    String effects = orNone(text(item.get("eff")));
    if (!normalized(effects).equals(normalized(String.join(", ", data.damageEffects())))) {
      differences.add("effects " + (effects.isEmpty() ? "none" : effects));
    }
    Integer fireRate = LegacyNumbers.integer(item.get("rate"));
    if (data.fireRate() != null && fireRate != null && !fireRate.equals(data.fireRate())) {
      differences.add("fire rate " + fireRate);
    }
    WeaponRange range = range(text(item.get("range")));
    if (data.range() != null && range != null && range != data.range()) {
      differences.add("range " + range.code());
    }
    return String.join(", ", differences);
  }

  private static @NonNull CustomWeapon custom(@NonNull String name, @NonNull JsonNode item) {
    Integer damage = LegacyNumbers.integer(item.get("dmg"));
    Integer fireRate = LegacyNumbers.integer(item.get("rate"));
    return new CustomWeapon(name, null, damage == null ? 0 : Math.max(0, damage), orNone(text(item.get("eff"))),
        damageType(text(item.get("type"))), fireRate == null || fireRate < 0 ? null : fireRate,
        range(text(item.get("range"))), orNone(text(item.get("qual"))), null);
  }

  /** Legacy damage type abbreviations ("Phys", "Ener/Rad", ...) as book names. */
  private static @NonNull String damageType(@NonNull String legacy) {
    return Arrays.stream(legacy.split("/")).map(String::strip).map(part -> switch (part) {
      case "Phys" -> "Physical";
      case "Ener" -> "Energy";
      case "Rad" -> "Radiation";
      default -> part;
    }).collect(Collectors.joining("/"));
  }

  private static @Nullable WeaponRange range(@NonNull String code) {
    return Arrays.stream(WeaponRange.values()).filter(range -> range.code().equals(code)).findFirst().orElse(null);
  }

  private static @NonNull String text(@Nullable JsonNode node) {
    return node == null || node.isNull() ? "" : node.asText().strip();
  }

  private static @NonNull String orNone(@NonNull String text) {
    return text.equals(NO_VALUE) || text.equals("–") ? "" : text;
  }

  private static @NonNull String normalized(@NonNull String effects) {
    return effects.replace(" ", "").toLowerCase(Locale.ROOT);
  }

  private static @NonNull String ammunitionName(@NonNull String id) {
    return WeaponCatalog.ammunitionById(id).map(AmmunitionData::name).orElse(id);
  }

  private static int nonNegative(@Nullable JsonNode node) {
    Integer value = LegacyNumbers.integer(node);
    return value == null || value < 0 ? 0 : value;
  }

  private static @NonNull String join(@NonNull String first, @NonNull String second) {
    return first.isEmpty() ? second : first + "; " + second;
  }

  private void info(@NonNull String message) {
    warnings.add(new ImportWarning(WarningSeverity.INFO, WEAPONS, message));
  }
}
