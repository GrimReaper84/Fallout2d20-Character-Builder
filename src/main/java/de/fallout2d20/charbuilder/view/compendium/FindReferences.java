package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.catalog.loot.LootRef;
import de.fallout2d20.charbuilder.model.scavenging.Find;
import de.fallout2d20.charbuilder.model.scavenging.FoundAmmunition;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Where a find links to in the compendium. */
final class FindReferences {

  private FindReferences() {}

  /** The compendium entry of the find; empty for results without a catalog entry. */
  static @NonNull Optional<Reference> of(@NonNull Find find) {
    return reference(find.ref(), find.text());
  }

  /** The compendium entry of the ammunition found with a weapon. */
  static @NonNull Optional<Reference> of(@NonNull FoundAmmunition ammunition) {
    return reference(ammunition.ref(), ammunition.text());
  }

  private static @NonNull Optional<Reference> reference(@Nullable LootRef ref, @NonNull String text) {
    if (ref == null) {
      return Optional.empty();
    }
    return Optional.of(switch (ref.kind()) {
      case WEAPON -> new Reference(Section.WEAPONS, ref.id(), text);
      case AMMUNITION -> new Reference(Section.AMMUNITION, ref.id(), text);
      case APPAREL -> new Reference(Section.APPAREL, ref.id(), text);
      case ITEM -> new Reference(Section.ITEMS, ref.id(), text);
      case MAGAZINE -> new Reference(Section.ITEMS, EquipmentEntries.magazineId(ref.id()), text);
    });
  }
}
