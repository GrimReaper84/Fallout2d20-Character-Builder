package de.fallout2d20.charbuilder.model.character.creation;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelCatalog;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelModData;
import de.fallout2d20.charbuilder.catalog.item.ItemData;
import de.fallout2d20.charbuilder.catalog.item.MagazineData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryArmorData;
import de.fallout2d20.charbuilder.catalog.legendary.LegendaryWeaponData;
import de.fallout2d20.charbuilder.catalog.weapon.AmmunitionData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponModData;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import de.fallout2d20.charbuilder.model.character.item.CarriedItem;
import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * Hints, in every mode, about gear of a book that is switched off for the character (Character ▸ Rules): weapons,
 * their mods and legendary properties, ammunition, apparel with mods and legendary properties, items and magazines.
 * Nothing is removed (user decision 2026-11-02); custom things belong to no book. Origin, perks and the equipment
 * pack are checked by the creation rules.
 */
public final class BookRules {

  private static final @NonNull String TOPIC = "Books";

  private BookRules() {}

  public static @NonNull List<CreationProblem> problems(@NonNull PlayerCharacter character) {
    Set<Book> books = character.getEnabledBooks();
    List<CreationProblem> problems = new ArrayList<>();
    for (CarriedWeapon weapon : character.gear().getWeapons()) {
      weapon.data().ifPresent(data -> check(problems, HintArea.GEAR, books, data.name(), data.sources()));
      for (CatalogRef mod : weapon.mods().values()) {
        Optional<WeaponModData> data = WeaponCatalog.modById(mod.id());
        data.ifPresent(entry -> check(problems, HintArea.GEAR, books,
            entry.name() + " (" + weapon.displayName() + ")", entry.sources()));
      }
      Optional<LegendaryWeaponData> legendary = weapon.legendaryProperty();
      legendary.ifPresent(entry -> check(problems, HintArea.GEAR, books,
          entry.name() + " (" + weapon.displayName() + ")", entry.sources()));
    }
    for (String id : character.gear().getAmmunition().keySet()) {
      Optional<AmmunitionData> ammunition = WeaponCatalog.ammunitionById(id);
      ammunition.ifPresent(entry -> check(problems, HintArea.GEAR, books, entry.name(), entry.sources()));
    }
    for (WornApparel apparel : character.gear().getApparel()) {
      Optional<ApparelData> data = apparel.data();
      data.ifPresent(entry -> check(problems, HintArea.ARMOR, books, entry.name(), entry.sources()));
      for (CatalogRef mod : apparel.mods().values()) {
        Optional<ApparelModData> modData = ApparelCatalog.modById(mod.id());
        modData.ifPresent(entry -> check(problems, HintArea.ARMOR, books,
            entry.name() + " (" + apparel.displayName() + ")", entry.sources()));
      }
      Optional<LegendaryArmorData> legendary = apparel.legendaryProperty();
      legendary.ifPresent(entry -> check(problems, HintArea.ARMOR, books,
          entry.name() + " (" + apparel.displayName() + ")", entry.sources()));
    }
    for (CarriedItem item : character.gear().getItems()) {
      Optional<ItemData> data = item.data();
      data.ifPresent(entry -> check(problems, HintArea.GEAR, books, entry.name(), entry.sources()));
      Optional<MagazineData> magazine = item.magazine();
      magazine.ifPresent(entry -> check(problems, HintArea.GEAR, books, entry.name(), entry.sources()));
    }
    return problems.stream().distinct().toList();
  }

  /** Adds a hint if none of the entry's books is enabled; equal hints are merged by the caller. */
  private static void check(@NonNull List<CreationProblem> problems, @NonNull HintArea area, @NonNull Set<Book> books,
      @NonNull String name, @NonNull List<Source> sources) {
    if (sources.stream().anyMatch(source -> books.contains(source.book()))) {
      return;
    }
    List<String> titles = sources.stream().map(source -> source.book().title()).distinct().toList();
    problems.add(new CreationProblem(area, TOPIC, name + " is from " + String.join(" and ", titles)
        + (titles.size() == 1 ? ", which is" : ", which are") + " switched off for this character."));
  }
}
