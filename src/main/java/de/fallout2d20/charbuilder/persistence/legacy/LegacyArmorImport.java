package de.fallout2d20.charbuilder.persistence.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.model.character.apparel.CustomApparel;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import de.fallout2d20.charbuilder.model.character.apparel.WornApparel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Reads the legacy {@code armor} map (hit location name → hand-typed physical, energy and radiation
 * resistance). The legacy app stored no pieces, so each location with a value becomes a custom item
 * "Legacy armor (location)" (user decision): armor on a humanoid location, plating on a Mister Handy
 * location (CRB p. 146). Unknown location names stay in the payload as {@code unknownArmorLocations}.
 */
final class LegacyArmorImport {

  private static final @NonNull String ARMOR = "armor";

  private final @NonNull List<WornApparel> apparel = new ArrayList<>();
  private final @NonNull List<ImportWarning> warnings;

  /** Reads and removes the {@code armor} field of the payload. */
  LegacyArmorImport(@NonNull ObjectNode payload, @NonNull List<ImportWarning> warnings) {
    this.warnings = warnings;
    JsonNode node = payload.get(ARMOR);
    if (!(node instanceof ObjectNode map)) {
      if (node != null) {
        warning("Armor is not a map of locations; kept in the legacy data.");
      }
      return;
    }
    payload.remove(ARMOR);
    ObjectNode unknown = JsonNodeFactory.instance.objectNode();
    for (Map.Entry<String, JsonNode> entry : map.properties()) {
      Optional<HitLocation> location = Arrays.stream(HitLocation.values())
          .filter(candidate -> candidate.displayName().equalsIgnoreCase(entry.getKey().strip())).findFirst();
      if (location.isEmpty()) {
        unknown.set(entry.getKey(), entry.getValue());
        continue;
      }
      read(location.get(), entry.getValue());
    }
    if (!unknown.isEmpty()) {
      payload.set("unknownArmorLocations", unknown);
      warning(unknown.size() + " armor location(s) not recognized; kept in the legacy data.");
    }
  }

  @NonNull List<WornApparel> apparel() {
    return apparel;
  }

  private void read(@NonNull HitLocation location, @NonNull JsonNode values) {
    int physical = nonNegative(values.get("physical"));
    int energy = nonNegative(values.get("energy"));
    int radiation = nonNegative(values.get("radiation"));
    if (physical == 0 && energy == 0 && radiation == 0) {
      return;
    }
    boolean robot = HitLocation.misterHandy().contains(location);
    String name = "Legacy armor (" + location.displayName() + ")";
    apparel.add(WornApparel.of(new CustomApparel(name, robot ? ApparelKind.ROBOT_PLATING : ApparelKind.ARMOR,
        Set.of(location), physical, energy, radiation, false, 0)));
    warnings.add(new ImportWarning(WarningSeverity.INFO, ARMOR, name + " imported with the hand-typed resistances "
        + physical + "/" + energy + "/" + radiation + "; replace it by the worn pieces."));
  }

  private static int nonNegative(@Nullable JsonNode node) {
    Integer value = LegacyNumbers.integer(node);
    return value == null || value < 0 ? 0 : value;
  }

  private void warning(@NonNull String message) {
    warnings.add(new ImportWarning(WarningSeverity.WARNING, ARMOR, message));
  }
}
