package de.fallout2d20.charbuilder.model.character.apparel;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.apparel.ApparelKind;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.creation.CreationProblem;
import de.fallout2d20.charbuilder.model.character.creation.HintArea;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Hints about worn apparel, in every mode; soft only, the values are computed anyway (user decision): armor
 * over an outfit (CRB p. 130), two items of one layer on the same location, human apparel on a Mister Handy
 * (only hats, without effect, CRB p. 146) or on a humanoid robot such as the Protectron (house rule: it
 * wears robot plating and armor, user decision 2026-10-16), robot armor on anyone else, single-limb pieces without a fitting limb, and
 * items outside the catalog.
 */
public final class ApparelRules {

  private static final @NonNull String TOPIC = "Apparel";

  private ApparelRules() {}

  /** Whether the character is a robot (Mister Handy, Protectron, Assaultron) and so wears robot plating and armor. */
  public static boolean isRobot(@NonNull Character character) {
    return character.getOriginData().map(OriginData::robot).orElse(false);
  }

  /**
   * Whether an item of this kind suits the character: robot items for robots (plus hats for a Mister Handy,
   * CRB p. 146), the rest for others.
   */
  public static boolean suits(@NonNull Character character, @NonNull ApparelKind kind, boolean hat) {
    if (character.isMisterHandy()) {
      return kind.isRobot() || hat;
    }
    return isRobot(character) == kind.isRobot();
  }

  /**
   * Whether the add dialog offers an item of this kind: what {@link #suits} the character, and for a
   * humanoid robot also human apparel (allowed with GM permission, so only a hint), never for a Mister Handy.
   */
  public static boolean isOffered(@NonNull Character character, @NonNull ApparelKind kind, boolean hat) {
    return suits(character, kind, hat) || (isRobot(character) && !character.isMisterHandy());
  }

  /**
   * Whether a piece would clash with what the character wears: a worn piece of the same layer covers one
   * of its locations, or it is armor while an outfit is worn or the other way round (CRB p. 130). A new
   * piece that clashes starts carried (user decision 2026-10-16).
   */
  public static boolean clashesWithWorn(@NonNull Character character, @NonNull WornApparel item) {
    Optional<ApparelKind> kind = item.values().map(ApparelValues::kind);
    if (kind.isEmpty()) {
      return false;
    }
    List<WornApparel> worn = character.getWornApparel();
    if ((kind.get() == ApparelKind.ARMOR && has(worn, ApparelKind.OUTFIT))
        || (kind.get() == ApparelKind.OUTFIT && has(worn, ApparelKind.ARMOR))) {
      return true;
    }
    List<HitLocation> body = character.hitLocations();
    return worn.stream().anyMatch(other -> sharedLocation(item, other, body).isPresent());
  }

  /** A location both pieces cover while being of the same layer; empty otherwise or for unknown pieces. */
  private static @NonNull Optional<HitLocation> sharedLocation(@NonNull WornApparel item, @NonNull WornApparel other,
      @NonNull List<HitLocation> body) {
    Optional<ApparelKind> kind = item.values().map(ApparelValues::kind);
    Optional<ApparelKind> otherKind = other.values().map(ApparelValues::kind);
    if (kind.isEmpty() || otherKind.isEmpty() || layer(kind.get()) != layer(otherKind.get())) {
      return Optional.empty();
    }
    return item.locations(body).stream().filter(other.locations(body)::contains).findFirst();
  }

  /** Hints about the worn pieces (carried ones only have to be catalog items). */
  public static @NonNull List<CreationProblem> problems(@NonNull Character character) {
    List<CreationProblem> problems = new ArrayList<>();
    character.getApparel().stream().filter(item -> item.values().isEmpty()).forEach(item ->
        problems.add(new CreationProblem(HintArea.ARMOR, TOPIC,
            "\"" + item.displayName() + "\" is not an item of the supported books.")));
    List<WornApparel> worn = character.getWornApparel();
    boolean robot = isRobot(character);
    List<HitLocation> body = character.hitLocations();
    if (has(worn, ApparelKind.OUTFIT) && has(worn, ApparelKind.ARMOR)) {
      problems.add(new CreationProblem(HintArea.ARMOR, TOPIC, "Armor cannot be worn over an outfit (CRB p. 130)."));
    }
    for (int i = 0; i < worn.size(); i++) {
      WornApparel item = worn.get(i);
      Optional<ApparelKind> kind = item.values().map(ApparelValues::kind);
      if (item.needsLocation(body)) {
        problems.add(new CreationProblem(HintArea.ARMOR, TOPIC, "Choose where " + item.displayName() + " is worn."));
      }
      boolean hat = item.data().map(ApparelData::isHat).orElse(false);
      if (kind.isPresent() && robot && !suits(character, kind.get(), hat)) {
        problems.add(new CreationProblem(HintArea.ARMOR, TOPIC, character.isMisterHandy()
            ? "A Mister Handy cannot wear " + item.displayName() + " (CRB p. 146)."
            : "Robots like the " + character.getOriginData().map(OriginData::name).orElse("")
                + " wear robot plating and armor; " + item.displayName() + " only with GM permission."));
      }
      if (kind.isPresent() && !robot && !suits(character, kind.get(), hat)) {
        problems.add(new CreationProblem(HintArea.ARMOR, TOPIC, item.displayName() + " is made for robots."));
      }
      for (int j = i + 1; j < worn.size(); j++) {
        WornApparel other = worn.get(j);
        sharedLocation(item, other, body).ifPresent(location ->
            problems.add(new CreationProblem(HintArea.ARMOR, TOPIC, item.displayName() + " and " + other.displayName()
                + " both cover the " + location.displayName() + "; the higher values count.")));
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
