package de.fallout2d20.charbuilder.model.character.apparel;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.character.derived.CarryLoad;
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
 * <p>An Assaultron's plating does not count on a location its robot armor covers ({@link
 * OriginData#robotArmorReplacesPlating()}, WG p. 18).
 *
 * <p>Humanoids: armor worn over clothing uses the higher resistance of each type (CRB p. 130); two pieces of one layer
 * on a location (a hint of {@link ApparelRules}) count the same way, the higher value. Robots (Mister Handy,
 * Protectron): the plating covers every location and robot armor adds to it (CRB p. 146). Innate resistances (perks such
 * as Toughness, origin immunities) are added on every location (assumption, legacy behavior: the perks name all
 * locations); apparel adds poison only through a legendary Poisoner's piece on the locations it covers (WG p. 107),
 * combined like the other types. An item covering a location with radiation immunity (Hazmat Suit, CRB p. 124) makes it
 * immune.
 *
 * <p>Perks of the Wanderer's Guide that need the worn gear add Physical and Energy DR on every location here (not in
 * {@link DerivedStats}, which {@link CarryLoad} uses): Evasive by AGI unless over-encumbered (WG p. 51), Ironclad
 * per rank while a worn piece is armor ({@link WornApparel#isArmor()}, WG p. 48), Junk Shield per 5 junk carried up
 * to its rank (WG p. 52). Their Power Armor exception does not arise yet (D3).
 */
public final class ArmorResistances {

  /** WG p. 52: Junk Shield gives +1 DR for every 5 junk carried. */
  private static final int JUNK_PER_DR = 5;

  private ArmorResistances() {}

  /** One entry per hit location of the character, in location order. */
  public static @NonNull List<LocationResistance> of(@NonNull PlayerCharacter character) {
    DerivedStats innate = DerivedStats.of(character);
    int perkDr = perkDr(character);
    List<LocationResistance> result = new ArrayList<>();
    List<HitLocation> body = character.hitLocations();
    boolean armorReplacesPlating = character.getOriginData().map(OriginData::robotArmorReplacesPlating)
        .orElse(false);
    for (HitLocation location : body) {
      List<ApparelValues> worn = new ArrayList<>();
      for (WornApparel item : character.gear().getWornApparel()) {
        if (item.locations(body).contains(location)) {
          item.values().ifPresent(worn::add);
        }
      }
      if (armorReplacesPlating && worn.stream().anyMatch(item -> item.kind() == ApparelKind.ROBOT_ARMOR)) {
        worn.removeIf(item -> item.kind() == ApparelKind.ROBOT_PLATING);
      }
      boolean immune = worn.stream().anyMatch(ApparelValues::radiationImmune);
      result.add(new LocationResistance(location,
          add(apparel(worn, ApparelValues::physical) + perkDr, innate.physical()),
          add(apparel(worn, ApparelValues::energy) + perkDr, innate.energy()),
          immune ? Resistance.IMMUNITY : add(apparel(worn, ApparelValues::radiation), innate.radiation()),
          add(apparel(worn, ApparelValues::poison), innate.poison())));
    }
    return result;
  }

  /** Physical and Energy DR of Evasive, Ironclad and Junk Shield (the same for both types on every location). */
  private static int perkDr(@NonNull PlayerCharacter character) {
    int dr = 0;
    if (character.perkEffect(perk -> perk.drByAgility() ? 1 : 0) > 0 && !CarryLoad.of(character).isOverEncumbered()) {
      dr += evasive(character.abilities().attributeValue(Attribute.AGILITY));
    }
    if (character.gear().getWornApparel().stream().anyMatch(WornApparel::isArmor)) {
      dr += character.perkEffect(PerkDefinition::wornArmorDrPerRank);
    }
    dr += Math.min(character.perkEffect(PerkDefinition::junkDrPerRank),
        character.gear().junkCount() / JUNK_PER_DR);
    return dr;
  }

  /** WG p. 51: +1 at AGI 7–8, +2 at 9–10, +3 at 11 or more. */
  private static int evasive(int agility) {
    if (agility >= 11) {
      return 3;
    }
    if (agility >= 9) {
      return 2;
    }
    return agility >= 7 ? 1 : 0;
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
