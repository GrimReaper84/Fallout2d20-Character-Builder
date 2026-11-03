package de.fallout2d20.charbuilder.view.compendium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponCatalog;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CompendiumContentTest {

  private static final Set<Book> ALL_BOOKS = EnumSet.allOf(Book.class);

  @ParameterizedTest
  @EnumSource(value = Section.class, names = {"PARTY", "ROLL_TABLES"}, mode = EnumSource.Mode.EXCLUDE)
  void everySectionListsEntriesWithOneCellPerColumnUniqueIdsAndKnownCategories(Section section) {
    SectionContent content = CompendiumContent.of(section, ALL_BOOKS);
    assertFalse(content.entries().isEmpty(), section + " lists entries");
    Set<String> ids = new HashSet<>();
    for (CompendiumEntry entry : content.entries()) {
      assertEquals(content.columns().size(), entry.cells().size(), entry.name());
      assertTrue(ids.add(entry.id()), "unique id " + entry.id());
      assertTrue(content.categories().isEmpty() || content.categories().contains(entry.category()),
          entry.name() + " has a category of the filter: " + entry.category());
    }
  }

  @Test
  void everyReferenceOpensAListedEntry() {
    for (Section section : catalogSections()) {
      for (CompendiumEntry entry : CompendiumContent.of(section, ALL_BOOKS).entries()) {
        for (DetailLine line : entry.details()) {
          if (line instanceof DetailLinks links) {
            for (Reference reference : links.references()) {
              assertTrue(CompendiumContent.of(reference.section(), ALL_BOOKS).entry(reference.id()).isPresent(),
                  entry.name() + " → " + reference);
            }
          }
        }
      }
    }
  }

  @Test
  void theBookFilterLeavesOutEntriesOfDisabledBooks() {
    Set<Book> core = EnumSet.of(Book.CRB);
    assertEquals(WeaponCatalog.available(core).size(), CompendiumContent.of(Section.WEAPONS, core).entries().size());
    assertEquals(PerkCatalog.available(core).size(), CompendiumContent.of(Section.PERKS, core).entries().size());
    assertTrue(CompendiumContent.of(Section.ORIGINS, core).entries().size()
        < CompendiumContent.of(Section.ORIGINS, ALL_BOOKS).entries().size());
    assertTrue(CompendiumContent.of(Section.LEGENDARY, core).entries().isEmpty(), "legendary properties are WG");
  }

  @Test
  void itemsListMagazinesAndLegendaryBothTables() {
    List<String> itemCategories = CompendiumContent.of(Section.ITEMS, ALL_BOOKS).entries().stream()
        .map(CompendiumEntry::category).distinct().toList();
    assertTrue(itemCategories.contains("Magazines"));
    List<String> legendary = CompendiumContent.of(Section.LEGENDARY, ALL_BOOKS).entries().stream()
        .map(CompendiumEntry::category).distinct().toList();
    assertEquals(List.of("Weapon properties", "Armor properties"), legendary);
  }

  @Test
  void aWeaponLinksItsAmmunitionAndQualities() {
    CompendiumEntry pistol = CompendiumContent.of(Section.WEAPONS, ALL_BOOKS).entries().stream()
        .filter(entry -> entry.name().equals("10mm Pistol")).findFirst().orElseThrow();
    List<Reference> links = pistol.details().stream().filter(DetailLinks.class::isInstance)
        .map(DetailLinks.class::cast).flatMap(line -> line.references().stream()).toList();
    assertTrue(links.stream().anyMatch(link -> link.section() == Section.AMMUNITION), links.toString());
    assertTrue(links.stream().anyMatch(link -> link.section() == Section.QUALITIES), links.toString());
    assertTrue(links.stream().anyMatch(link -> link.section() == Section.WEAPON_MODS), links.toString());
  }

  @Test
  void anEntrySortsMissingNumbersFirst() {
    assertEquals(Integer.MIN_VALUE, Cell.number(null).sortKey());
    assertEquals("–", Cell.number(null).text());
    assertEquals(3, Cell.number(3).sortKey());
  }

  private static List<Section> catalogSections() {
    return Arrays.stream(Section.values()).filter(Section::isCatalog).toList();
  }
}
