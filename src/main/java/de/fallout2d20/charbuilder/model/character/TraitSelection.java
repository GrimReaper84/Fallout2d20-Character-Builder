package de.fallout2d20.charbuilder.model.character;

import de.fallout2d20.charbuilder.catalog.trait.TraitCatalog;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * A trait of a character (e.g. a Survivor trait).
 *
 * @param trait the trait
 * @param imported whether the trait comes from a legacy save; its effects on attributes and skills
 *     are then already part of the {@link LegacyBaseline}
 * @param attributeChoices chosen attributes for traits such as Gifted (empty for imported traits)
 */
public record TraitSelection(
    @NonNull CatalogRef trait, boolean imported, @NonNull List<Attribute> attributeChoices) {

  public TraitSelection {
    attributeChoices = List.copyOf(attributeChoices);
  }

  /**
   * Whether the player chooses attributes for this selection: a non-imported trait whose catalog entry
   * raises chosen attributes (Gifted, CRB p. 56). Imported traits are already in the legacy baseline.
   */
  public boolean choosesAttributes() {
    return !imported && TraitCatalog.byId(trait.id())
        .map(definition -> definition.attributeChoices() > 0).orElse(false);
  }
}
