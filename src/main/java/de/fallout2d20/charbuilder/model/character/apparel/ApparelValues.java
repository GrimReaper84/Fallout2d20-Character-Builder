package de.fallout2d20.charbuilder.model.character.apparel;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import org.jspecify.annotations.NonNull;

/**
 * What a worn item contributes, whether from the catalog or custom: its kind (how it layers), damage
 * resistances and robot carry weight change.
 */
public record ApparelValues(
    @NonNull ApparelKind kind,
    int physical,
    int energy,
    int radiation,
    boolean radiationImmune,
    int carryWeight) {

  public static @NonNull ApparelValues of(@NonNull ApparelData data) {
    return new ApparelValues(data.kind(), data.physical(), data.energy(), data.radiation(), data.radiationImmune(),
        data.carryWeight());
  }
}
