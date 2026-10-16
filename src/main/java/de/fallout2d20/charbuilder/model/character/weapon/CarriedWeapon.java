package de.fallout2d20.charbuilder.model.character.weapon;

import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A weapon the character carries: a catalog weapon or a custom one (exactly one of both), how many of
 * it (grenades, mines, thrown weapons; 1 for everything else) and a free note, e.g. installed mods
 * (user decision: mods as a note until they are modelled).
 */
public record CarriedWeapon(
    @Nullable CatalogRef weapon,
    @Nullable CustomWeapon custom,
    int quantity,
    @NonNull String notes) {

  public CarriedWeapon {
    if ((weapon == null) == (custom == null)) {
      throw new IllegalArgumentException("A carried weapon is either a catalog weapon or a custom one");
    }
    if (quantity < 1) {
      throw new IllegalArgumentException("Quantity must be at least 1");
    }
  }

  /** One catalog weapon without a note. */
  public static @NonNull CarriedWeapon of(@NonNull CatalogRef weapon) {
    return new CarriedWeapon(weapon, null, 1, "");
  }

  /** One custom weapon without a note. */
  public static @NonNull CarriedWeapon of(@NonNull CustomWeapon custom) {
    return new CarriedWeapon(null, custom, 1, "");
  }

  /** The catalog entry; empty for a custom weapon or an id outside the catalog. */
  public @NonNull Optional<WeaponData> data() {
    return weapon == null ? Optional.empty() : WeaponCatalog.byId(weapon.id());
  }

  /** The attack skill: the catalog weapon's type or the custom weapon's skill; empty if unknown. */
  public @NonNull Optional<Skill> skill() {
    return custom != null ? Optional.ofNullable(custom.skill()) : data().map(WeaponData::skill);
  }

  /**
   * Whether the quantity matters: explosives and thrown weapons are used up when attacking (CRB p. 27),
   * and any entry already held more than once.
   */
  public boolean isCounted() {
    return quantity > 1 || skill().map(Skill::isCountedWeapon).orElse(false);
  }

  public @NonNull String displayName() {
    if (custom != null) {
      return custom.name();
    }
    return data().map(WeaponData::name).orElseGet(() -> weapon == null ? "" : weapon.displayName());
  }

  public @NonNull CarriedWeapon withNotes(@Nullable String newNotes) {
    return new CarriedWeapon(weapon, custom, quantity, newNotes == null ? "" : newNotes);
  }

  /**
   * The custom weapon attacked with another skill.
   *
   * @throws IllegalStateException for a catalog weapon, whose skill is fixed by the book
   */
  public @NonNull CarriedWeapon withCustomSkill(@Nullable Skill skill) {
    if (custom == null) {
      throw new IllegalStateException("Only a custom weapon has a choosable skill");
    }
    return new CarriedWeapon(null, custom.withSkill(skill), quantity, notes);
  }

  public @NonNull CarriedWeapon withQuantity(int newQuantity) {
    return new CarriedWeapon(weapon, custom, newQuantity, notes);
  }
}
