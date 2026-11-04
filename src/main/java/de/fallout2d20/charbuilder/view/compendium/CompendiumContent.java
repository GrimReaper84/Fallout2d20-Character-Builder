package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.catalog.Book;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/** What each catalog section of the GM compendium (H2) lists for the enabled books. */
final class CompendiumContent {

  private CompendiumContent() {}

  /**
   * @throws IllegalArgumentException for the party, the scavenging and the roll tables, which are no catalog lists
   */
  static @NonNull SectionContent of(@NonNull Section section, @NonNull Set<Book> books) {
    return switch (section) {
      case WEAPONS -> EquipmentEntries.weapons(books);
      case WEAPON_MODS -> EquipmentEntries.weaponMods(books);
      case AMMUNITION -> EquipmentEntries.ammunition(books);
      case APPAREL -> EquipmentEntries.apparel(books);
      case APPAREL_MODS -> EquipmentEntries.apparelMods(books);
      case ITEMS -> EquipmentEntries.items(books);
      case LEGENDARY -> EquipmentEntries.legendary(books);
      case ORIGINS -> RuleEntries.origins(books);
      case TRAITS -> RuleEntries.traits(books);
      case PERKS -> RuleEntries.perks(books);
      case SKILLS -> RuleEntries.skills();
      case PACKS -> RuleEntries.packs(books);
      case QUALITIES -> RuleEntries.qualities(books);
      case PARTY, SCAVENGING, ROLL_TABLES -> throw new IllegalArgumentException("Not a catalog section: " + section);
    };
  }
}
