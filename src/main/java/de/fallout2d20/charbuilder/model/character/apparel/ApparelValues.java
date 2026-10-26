package de.fallout2d20.charbuilder.model.character.apparel;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import org.jspecify.annotations.NonNull;

/**
 * What a worn item contributes, whether from the catalog or custom: its kind (how it layers), damage
 * resistances and robot carry weight change. Poison DR comes only from a legendary property (Poisoner's, WG p. 107).
 */
public record ApparelValues(
    @NonNull ApparelKind kind,
    int physical,
    int energy,
    int radiation,
    boolean radiationImmune,
    int carryWeight,
    int poison) {

  /** Values without Poison DR. */
  public ApparelValues(@NonNull ApparelKind kind, int physical, int energy, int radiation, boolean radiationImmune,
      int carryWeight) {
    this(kind, physical, energy, radiation, radiationImmune, carryWeight, 0);
  }

  public static @NonNull ApparelValues of(@NonNull ApparelData data) {
    return new ApparelValues(data.kind(), data.physical(), data.energy(), data.radiation(), data.radiationImmune(),
        data.carryWeight());
  }
}
