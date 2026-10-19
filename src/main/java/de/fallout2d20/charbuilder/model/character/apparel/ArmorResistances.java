package de.fallout2d20.charbuilder.model.character.apparel;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import de.fallout2d20.charbuilder.model.character.derived.Resistance;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import org.jspecify.annotations.NonNull;

/**
 * Damage resistance per hit location, computed on every call (never stored); the one place combining
 * apparel and innate resistances.
 *
 * <p>Humanoids: armor worn over clothing uses the higher resistance of each type (CRB p. 130); two pieces
 * of one layer on a location (a hint of {@link ApparelRules}) count the same way, the higher value. Robots
 * (Mister Handy, Protectron): the plating covers every location and robot armor adds to it (CRB p. 146). Innate
 * resistances (perks such as Toughness, origin immunities) are added on every location (assumption,
 * legacy behavior: the perks name all locations); poison comes only from them, apparel has no poison
 * value. An item covering a location with radiation immunity (Hazmat Suit, CRB p. 124) makes it immune.
 */
public final class ArmorResistances {

  private ArmorResistances() {}

  /** One entry per hit location of the character, in location order. */
  public static @NonNull List<LocationResistance> of(@NonNull PlayerCharacter character) {
    DerivedStats innate = DerivedStats.of(character);
    List<LocationResistance> result = new ArrayList<>();
    List<HitLocation> body = character.hitLocations();
    for (HitLocation location : body) {
      List<ApparelValues> worn = new ArrayList<>();
      for (WornApparel item : character.getWornApparel()) {
        if (item.locations(body).contains(location)) {
          item.values().ifPresent(worn::add);
        }
      }
      boolean immune = worn.stream().anyMatch(ApparelValues::radiationImmune);
      result.add(new LocationResistance(location,
          add(apparel(worn, ApparelValues::physical), innate.physical()),
          add(apparel(worn, ApparelValues::energy), innate.energy()),
          immune ? Resistance.IMMUNITY : add(apparel(worn, ApparelValues::radiation), innate.radiation()),
          innate.poison()));
    }
    return result;
  }

  /** The apparel value of one type: the highest robot plating plus the highest robot armor, or else the highest item. */
  private static int apparel(@NonNull List<ApparelValues> worn, @NonNull ToIntFunction<ApparelValues> value) {
    int plating = highest(worn, value, ApparelKind.ROBOT_PLATING);
    int robotArmor = highest(worn, value, ApparelKind.ROBOT_ARMOR);
    int other = worn.stream().filter(item -> !item.kind().isRobot()).mapToInt(value).max().orElse(0);
    return Math.max(plating + robotArmor, other);
  }

  private static int highest(@NonNull List<ApparelValues> worn, @NonNull ToIntFunction<ApparelValues> value,
      @NonNull ApparelKind kind) {
    return worn.stream().filter(item -> item.kind() == kind).mapToInt(value).max().orElse(0);
  }

  private static @NonNull Resistance add(int apparel, @NonNull Resistance innate) {
    return innate.immune() ? innate : Resistance.of(apparel + innate.value());
  }
}
