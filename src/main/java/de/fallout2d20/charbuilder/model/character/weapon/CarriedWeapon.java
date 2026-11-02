package de.fallout2d20.charbuilder.model.character.weapon;

import de.fallout2d20.charbuilder.catalog.legendary.LegendaryCatalog;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponType;
import de.fallout2d20.charbuilder.catalog.weapon.ModSlot;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponKind;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.equipment.StartingSource;
import de.fallout2d20.charbuilder.model.character.legendary.LegendaryRules;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A weapon the character carries: a catalog weapon or a custom one (exactly one of both), how many of
 * it (grenades, mines, thrown weapons; 1 for everything else) and a free note, e.g. installed mods
 * (user decision: mods as a note until they are modelled), and whether it is built into the character (a
 * robot's integrated weapon, e.g. the laser guns in a Protectron's claws, WOA p. 22): built-in weapons
 * weigh nothing against the carry weight (user decision 2026-10-16), and the mods installed per slot (CRB
 * p. 87: one per slot; values via {@link ModdedWeapon}, which mods fit via {@link WeaponMods}). A mod id the
 * catalog no longer knows is kept and has no effect. {@code startingSource} marks a weapon of the starting
 * equipment ({@code null}: added by hand). {@code legendary} is the weapon's legendary property (WG pp. 96–103; one
 * per weapon, which fit via {@link LegendaryRules}); an id the catalog does not know is kept and has no effect.
 */
public record CarriedWeapon(
    @Nullable CatalogRef weapon,
    @Nullable CustomWeapon custom,
    int quantity,
    @NonNull String notes,
    boolean builtIn,
    @NonNull Map<ModSlot, CatalogRef> mods,
    @Nullable StartingSource startingSource,
    @Nullable CatalogRef legendary) {

  public CarriedWeapon {
    if ((weapon == null) == (custom == null)) {
      throw new IllegalArgumentException("A carried weapon is either a catalog weapon or a custom one");
    }
    if (quantity < 1) {
      throw new IllegalArgumentException("Quantity must be at least 1");
    }
    mods = mods.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(mods));
  }

  /** A weapon that is not legendary. */
  public CarriedWeapon(@Nullable CatalogRef weapon, @Nullable CustomWeapon custom, int quantity, @NonNull String notes,
      boolean builtIn, @NonNull Map<ModSlot, CatalogRef> mods, @Nullable StartingSource startingSource) {
    this(weapon, custom, quantity, notes, builtIn, mods, startingSource, null);
  }

  /** A weapon added by hand. */
  public CarriedWeapon(@Nullable CatalogRef weapon, @Nullable CustomWeapon custom, int quantity, @NonNull String notes,
      boolean builtIn, @NonNull Map<ModSlot, CatalogRef> mods) {
    this(weapon, custom, quantity, notes, builtIn, mods, null);
  }

  /** A weapon added by hand, without mods. */
  public CarriedWeapon(@Nullable CatalogRef weapon, @Nullable CustomWeapon custom, int quantity, @NonNull String notes,
      boolean builtIn) {
    this(weapon, custom, quantity, notes, builtIn, Map.of(), null);
  }

  /** One catalog weapon without a note. */
  public static @NonNull CarriedWeapon of(@NonNull CatalogRef weapon) {
    return new CarriedWeapon(weapon, null, 1, "", false);
  }

  /** One custom weapon without a note. */
  public static @NonNull CarriedWeapon of(@NonNull CustomWeapon custom) {
    return new CarriedWeapon(null, custom, 1, "", false);
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
   * The kinds of the weapon for perks (CRB p. 70–71, WG p. 50): a catalog weapon's own and those of its installed
   * mods (a Flamer makes the Chainsaw fire-based), or the custom weapon's.
   */
  public @NonNull Set<WeaponKind> kinds() {
    if (custom != null) {
      return custom.kinds();
    }
    Set<WeaponKind> kinds = EnumSet.noneOf(WeaponKind.class);
    data().ifPresent(entry -> kinds.addAll(entry.kinds()));
    WeaponMods.installedMods(this).forEach(mod -> kinds.addAll(mod.kinds()));
    return WeaponKind.copyOf(kinds);
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
    return new CarriedWeapon(weapon, custom, quantity, newNotes == null ? "" : newNotes, builtIn, mods, startingSource,
        legendary);
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
    return new CarriedWeapon(null, custom.withSkill(skill), quantity, notes, builtIn, mods, startingSource, legendary);
  }

  /**
   * The custom weapon with other qualities.
   *
   * @throws IllegalStateException for a catalog weapon, whose qualities are fixed by the book
   */
  public @NonNull CarriedWeapon withCustomQualities(@NonNull String qualities) {
    if (custom == null) {
      throw new IllegalStateException("Only a custom weapon has choosable qualities");
    }
    return new CarriedWeapon(null, custom.withQualities(qualities), quantity, notes, builtIn, mods, startingSource,
        legendary);
  }

  /**
   * The custom weapon with other kinds.
   *
   * @throws IllegalStateException for a catalog weapon, whose kinds come from the catalog
   */
  public @NonNull CarriedWeapon withCustomKinds(@NonNull Set<WeaponKind> kinds) {
    if (custom == null) {
      throw new IllegalStateException("Only a custom weapon has choosable kinds");
    }
    return new CarriedWeapon(null, custom.withKinds(kinds), quantity, notes, builtIn, mods, startingSource, legendary);
  }

  public @NonNull CarriedWeapon withQuantity(int newQuantity) {
    return new CarriedWeapon(weapon, custom, newQuantity, notes, builtIn, mods, startingSource, legendary);
  }

  /** The weapon marked as part of the starting equipment; {@code null} for one added by hand. */
  public @NonNull CarriedWeapon withStartingSource(@Nullable StartingSource source) {
    return new CarriedWeapon(weapon, custom, quantity, notes, builtIn, mods, source, legendary);
  }

  public @NonNull CarriedWeapon withBuiltIn(boolean newBuiltIn) {
    return new CarriedWeapon(weapon, custom, quantity, notes, newBuiltIn, mods, startingSource, legendary);
  }

  /**
   * The weapon with {@code mod} in {@code slot}, replacing the mod there; {@code null} removes it. The fitting
   * rules are applied by {@link WeaponMods#install}.
   */
  public @NonNull CarriedWeapon withMod(@NonNull ModSlot slot, @Nullable CatalogRef mod) {
    Map<ModSlot, CatalogRef> newMods = new EnumMap<>(ModSlot.class);
    newMods.putAll(mods);
    if (mod == null) {
      newMods.remove(slot);
    } else {
      newMods.put(slot, mod);
    }
    return new CarriedWeapon(weapon, custom, quantity, notes, builtIn, newMods, startingSource, legendary);
  }

  /**
   * The custom weapon with other mod slots; mods of slots no longer chosen are removed.
   *
   * @throws IllegalStateException for a catalog weapon, whose slots are fixed by the book
   */
  public @NonNull CarriedWeapon withCustomModSlots(@NonNull Set<ModSlot> slots) {
    if (custom == null) {
      throw new IllegalStateException("Only a custom weapon has choosable mod slots");
    }
    Map<ModSlot, CatalogRef> kept = new EnumMap<>(ModSlot.class);
    mods.forEach((slot, mod) -> {
      if (slots.contains(slot)) {
        kept.put(slot, mod);
      }
    });
    return new CarriedWeapon(null, custom.withModSlots(slots), quantity, notes, builtIn, kept, startingSource,
        legendary);
  }

  /** The weapon with another legendary property; {@code null} makes it an ordinary weapon. */
  public @NonNull CarriedWeapon withLegendary(@Nullable CatalogRef property) {
    return new CarriedWeapon(weapon, custom, quantity, notes, builtIn, mods, startingSource, property);
  }

  /** The legendary property from the catalog; empty for an ordinary weapon or an id outside the catalog. */
  public @NonNull Optional<LegendaryWeaponData> legendaryProperty() {
    return legendary == null ? Optional.empty() : LegendaryCatalog.weaponPropertyById(legendary.id());
  }

  /**
   * The column of the Legendary Weapons tables the weapon rolls on (WG p. 96): by its skill, with the catalog's
   * unusual weapons; empty for bows, explosives, throwing weapons and an unknown skill.
   */
  public @NonNull Optional<LegendaryWeaponType> legendaryType() {
    if (custom != null) {
      List<String> qualities = Arrays.stream(custom.qualities().split(",")).map(String::strip)
          .filter(quality -> !quality.isEmpty()).toList();
      return LegendaryWeaponType.of(custom.skill(), qualities, false);
    }
    return data().flatMap(entry -> LegendaryWeaponType.of(entry.skill(), entry.qualities(),
        entry.legendaryAsSmallGun()));
  }

  /** The name with the legendary mark and property, e.g. {@code ★ Two-Shot Hunting Rifle}; else the name. */
  public @NonNull String legendaryName() {
    return legendaryProperty().map(property -> LegendaryRules.name(property.name(), displayName()))
        .orElseGet(this::displayName);
  }

  /**
   * Whether the weapon is part of the character: marked as built in, or a catalog weapon only offered to
   * the origins it is built into (WOA p. 22: Protectron weapons).
   */
  public boolean isIntegrated() {
    return builtIn || data().map(entry -> !entry.origins().isEmpty()).orElse(false);
  }
}
