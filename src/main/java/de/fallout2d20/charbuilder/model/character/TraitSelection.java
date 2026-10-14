package de.fallout2d20.charbuilder.model.character;

import java.util.List;

/**
 * A trait of a character (e.g. a Survivor trait).
 *
 * @param trait the trait
 * @param imported whether the trait comes from a legacy save; its effects on attributes and skills
 *     are then already part of the {@link LegacyBaseline}
 * @param attributeChoices chosen attributes for traits such as Gifted (empty for imported traits)
 */
public record TraitSelection(CatalogRef trait, boolean imported, List<Attribute> attributeChoices) {

  public TraitSelection {
    attributeChoices = List.copyOf(attributeChoices);
  }
}
