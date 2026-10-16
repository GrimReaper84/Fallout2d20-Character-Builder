package de.fallout2d20.charbuilder.model.character.equipment;

import de.fallout2d20.charbuilder.catalog.CatalogNames;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.item.ItemCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.item.CustomItem;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Turns the starting equipment text ({@link StartingEquipmentItems#lines}) into inventory entries (user
 * decision 2026-10-16). Each line is matched by name or alias against the weapon, apparel and item
 * catalogs: a leading count ({@code 2 stimpaks}, {@code Two laser guns}) becomes the quantity, a plural
 * {@code s} is dropped, {@code X with …} adds weapon X and keeps the rest (usually an ammunition formula to
 * roll) as a custom item, and a line naming several things ({@code Lab coat, 1 dose of Mentats}) is split
 * when every part is known. A catalog match whose wording differs from the catalog name keeps the pack
 * wording as note (e.g. a pipe rifle is a modded Pipe Gun, or a ruling in parentheses); a weapon the text
 * calls built in ("built into the claws") is marked as built in and weighs nothing. Everything else becomes a custom item with the
 * line as name, so nothing is lost. The Mister Handy's arms are part of the robot, not the inventory, and
 * open choices are skipped.
 */
public final class PackInventory {

  private static final @NonNull Pattern COUNT = Pattern.compile("^(\\d+|one|two|three|four|five) (?!\\+)(.*)$",
      Pattern.CASE_INSENSITIVE);
  private static final @NonNull Map<String, Integer> NUMBER_WORDS =
      Map.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
  private static final @NonNull Pattern DOSE = Pattern.compile("^doses? of ", Pattern.CASE_INSENSITIVE);
  private static final @NonNull Pattern PLACEMENT =
      Pattern.compile(" (built into|integrated into|as the) .*$", Pattern.CASE_INSENSITIVE);
  private static final @NonNull Pattern PARENTHESES = Pattern.compile("\\s*\\([^)]*\\)$");
  private static final @NonNull Pattern SEPARATOR = Pattern.compile(", | and ");

  private PackInventory() {}

  public static @NonNull PackItems resolve(@NonNull Character character) {
    List<CarriedWeapon> weapons = new ArrayList<>();
    List<WornApparel> apparel = new ArrayList<>();
    List<CarriedItem> items = new ArrayList<>();
    for (String line : StartingEquipmentItems.lines(character)) {
      if (line.startsWith(StartingEquipmentItems.ARM) || line.endsWith(StartingEquipmentItems.NOT_CHOSEN)
          || line.endsWith(StartingEquipmentItems.NOT_CHOSEN + ")")) {
        continue;
      }
      if (line.startsWith(StartingEquipmentItems.TRINKET)) {
        String trinket = line.substring(StartingEquipmentItems.TRINKET.length());
        items.add(CarriedItem.of(new CustomItem(
            trinket.equals(StartingEquipmentItems.NOT_DESCRIBED) ? "Personal trinket" : trinket, 0,
            "")).withNotes("Personal trinket"));
        continue;
      }
      String text = withoutSkillPrefix(line);
      List<String> parts = Arrays.asList(SEPARATOR.split(text));
      if (parts.size() > 1 && parts.stream().allMatch(part -> match(part).isPresent())) {
        parts.forEach(part -> add(part, weapons, apparel, items));
      } else {
        add(text, weapons, apparel, items);
      }
    }
    return new PackItems(weapons, apparel, items);
  }

  /** CRB p. 81 lines read "Skill: items"; the skill name is not part of the item. */
  private static @NonNull String withoutSkillPrefix(@NonNull String line) {
    for (Skill skill : Skill.values()) {
      String prefix = skill.displayName() + ": ";
      if (line.startsWith(prefix)) {
        return line.substring(prefix.length());
      }
    }
    return line;
  }

  private static void add(@NonNull String text, @NonNull List<CarriedWeapon> weapons,
      @NonNull List<WornApparel> apparel, @NonNull List<CarriedItem> items) {
    Optional<Match> match = match(text);
    if (match.isEmpty()) {
      items.add(CarriedItem.of(new CustomItem(text.strip(), 0, "")));
      return;
    }
    Match found = match.get();
    String note = found.exact() ? "" : "Pack: " + text.strip();
    if (found.weapon() != null) {
      weapons.add(CarriedWeapon.of(CatalogRef.of(found.weapon().id())).withQuantity(found.quantity()).withNotes(note)
          .withBuiltIn(found.builtIn()));
    } else if (found.apparel() != null) {
      WornApparel worn = WornApparel.of(CatalogRef.of(found.apparel().id())).withNotes(note);
      for (int i = 0; i < found.quantity(); i++) {
        apparel.add(worn);
      }
    } else if (found.item() != null) {
      items.add(CarriedItem.of(CatalogRef.of(found.item().id())).withQuantity(found.quantity()).withNotes(note));
    }
    String rest = found.rest();
    if (rest != null) {
      items.add(CarriedItem.of(new CustomItem(rest, 0, "")));
    }
  }

  /** The catalog entry a piece of text names, with its quantity; empty if none. */
  static @NonNull Optional<Match> match(@NonNull String text) {
    String stripped = text.strip();
    String name = PARENTHESES.matcher(stripped).replaceAll("");
    // A ruling in parentheses, e.g. "(Two-Handed ignored)", must stay visible in the note.
    boolean annotated = !name.equals(stripped);
    int quantity = 1;
    Matcher count = COUNT.matcher(name);
    if (count.matches()) {
      String number = count.group(1).toLowerCase(Locale.ROOT);
      Integer word = NUMBER_WORDS.get(number);
      quantity = word != null ? word : Integer.parseInt(number);
      name = count.group(2);
    }
    name = DOSE.matcher(name).replaceAll("");
    String unplaced = PLACEMENT.matcher(name).replaceAll("");
    boolean placed = !unplaced.equals(name);
    name = unplaced;
    String rest = null;
    int with = name.indexOf(" with ");
    if (with > 0) {
      rest = name.substring(with + " with ".length());
      name = name.substring(0, with);
    }
    for (String candidate : candidates(name)) {
      Optional<Match> found = lookup(candidate, quantity, rest, placed, annotated);
      if (found.isPresent()) {
        return found;
      }
    }
    return Optional.empty();
  }

  /** The name, then without a plural "s" or "es". */
  private static @NonNull List<String> candidates(@NonNull String name) {
    List<String> candidates = new ArrayList<>(List.of(name));
    if (name.endsWith("s")) {
      candidates.add(name.substring(0, name.length() - 1));
    }
    if (name.endsWith("es")) {
      candidates.add(name.substring(0, name.length() - 2));
    }
    return candidates;
  }

  /**
   * {@code placed}: the text said where the thing is built in (a built-in weapon, kept in the note);
   * {@code annotated}: the text had a ruling in parentheses (kept in the note).
   */
  private static @NonNull Optional<Match> lookup(@NonNull String name, int quantity, @Nullable String rest,
      boolean placed, boolean annotated) {
    boolean plain = !placed && !annotated;
    Optional<WeaponData> weapon = WeaponCatalog.byNameOrAlias(name);
    if (weapon.isPresent()) {
      return Optional.of(new Match(weapon.get(), null, null, quantity, rest,
          plain && exact(name, weapon.get().name()), placed));
    }
    Optional<ApparelData> worn = ApparelCatalog.byName(name);
    return worn.map(apparelData -> new Match(null, apparelData, null, quantity, rest,
        plain && exact(name, apparelData.name()), false)).or(() -> ItemCatalog.byName(name).map(item -> new Match(null, null, item, quantity, rest,
        plain && exact(name, item.name()), false)));
  }

  /** Whether the pack names the entry like the catalog ({@link CatalogNames}). */
  private static boolean exact(@NonNull String name, @NonNull String catalogName) {
    return CatalogNames.normalize(name).equals(CatalogNames.normalize(catalogName));
  }
}
