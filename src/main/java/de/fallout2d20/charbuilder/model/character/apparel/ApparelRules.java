package de.fallout2d20.charbuilder.model.character.apparel;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Hints about worn apparel, in every mode; soft only, the values are computed anyway (user decision): armor
 * over an outfit (CRB p. 130), two items of one layer on the same location, human apparel on a Mister Handy
 * (only hats, without effect, CRB p. 146), robot armor on anyone else, single-limb pieces without a fitting
 * limb, and items outside the catalog.
 */
public final class ApparelRules {

  private static final @NonNull String TOPIC = "Apparel";

  private ApparelRules() {}

  /** Whether the character has the hit locations of a Mister Handy and so wears robot plating and armor. */
  public static boolean isRobot(@NonNull Character character) {
    return character.hitLocations().equals(HitLocation.misterHandy());
  }

  /** Whether an item of this kind suits the character: robot items (and hats) for a Mister Handy, the rest for others. */
  public static boolean suits(@NonNull Character character, @NonNull ApparelKind kind, boolean hat) {
    return isRobot(character) ? kind.isRobot() || hat : !kind.isRobot();
  }

  public static @NonNull List<CreationProblem> problems(@NonNull Character character) {
    List<CreationProblem> problems = new ArrayList<>();
    List<WornApparel> worn = character.getApparel();
    boolean robot = isRobot(character);
    if (has(worn, ApparelKind.OUTFIT) && has(worn, ApparelKind.ARMOR)) {
      problems.add(new CreationProblem(TOPIC, "Armor cannot be worn over an outfit (CRB p. 130)."));
    }
    for (int i = 0; i < worn.size(); i++) {
      WornApparel item = worn.get(i);
      Optional<ApparelKind> kind = item.values().map(ApparelValues::kind);
      if (kind.isEmpty()) {
        problems.add(new CreationProblem(TOPIC, "\"" + item.displayName() + "\" is not an item of the supported books."));
      }
      if (item.needsLocation()) {
        problems.add(new CreationProblem(TOPIC, "Choose where " + item.displayName() + " is worn."));
      }
      boolean hat = item.data().map(ApparelData::isHat).orElse(false);
      if (kind.isPresent() && robot && !suits(character, kind.get(), hat)) {
        problems.add(new CreationProblem(TOPIC, "A Mister Handy cannot wear " + item.displayName() + " (CRB p. 146)."));
      }
      if (kind.isPresent() && !robot && !suits(character, kind.get(), hat)) {
        problems.add(new CreationProblem(TOPIC, item.displayName() + " is made for a Mister Handy."));
      }
      for (int j = i + 1; j < worn.size(); j++) {
        WornApparel other = worn.get(j);
        Optional<ApparelKind> otherKind = other.values().map(ApparelValues::kind);
        if (kind.isPresent() && otherKind.isPresent() && layer(kind.get()) == layer(otherKind.get())) {
          item.locations().stream().filter(other.locations()::contains).findFirst().ifPresent(location ->
              problems.add(new CreationProblem(TOPIC, item.displayName() + " and " + other.displayName()
                  + " both cover the " + location.displayName() + "; the higher values count.")));
        }
      }
    }
    return problems;
  }

  /** Items of one layer cannot be worn on top of each other; clothing and outfits share a layer. */
  private static @NonNull ApparelKind layer(@NonNull ApparelKind kind) {
    return kind == ApparelKind.OUTFIT ? ApparelKind.CLOTHING : kind;
  }

  private static boolean has(@NonNull List<WornApparel> worn, @NonNull ApparelKind kind) {
    return worn.stream().anyMatch(item -> item.values().map(ApparelValues::kind).filter(kind::equals).isPresent());
  }
}
