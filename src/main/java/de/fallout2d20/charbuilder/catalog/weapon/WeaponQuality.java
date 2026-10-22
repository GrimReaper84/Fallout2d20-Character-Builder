package de.fallout2d20.charbuilder.catalog.weapon;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

/**
 * The weapon qualities of the book (CRB p. 90), with a short description in own words for tooltips. Thrown
 * comes with an ideal range, written {@code Thrown (C)} or {@code Thrown (M)}; catalog rows and custom
 * weapons keep their qualities as text, this enum only reads and writes that text.
 */
public enum WeaponQuality {
  ACCURATE("Accurate", "After aiming, spend up to 3 AP for +1 CD damage each (not together with ammo for damage)."),
  BLAST("Blast", "Attacks a whole zone instead of one target; a miss still deals half damage there."),
  CLOSE_QUARTERS("Close Quarters", "No difficulty increase when used within Reach of an enemy."),
  CONCEALED("Concealed", "Easy to hide; enemies only notice it when wielded or after a thorough search."),
  DEBILITATING("Debilitating", "Injuries it causes are harder to treat (+1 difficulty)."),
  GATLING("Gatling", "Spends ammunition in bursts of 10; each burst spent for damage gives +2 CD."),
  INACCURATE("Inaccurate", "Aiming gives no benefit."),
  MINE("Mine", "Placed and primed, it damages whoever comes within Reach of it."),
  NIGHT_VISION("Night Vision", "When aiming, darkness does not make the attack harder."),
  PARRY("Parry", "Spend 1 AP to add +1 Defense against a melee attack."),
  RECON("Recon", "After aiming, mark the target: the next ally attacking it may re-roll one d20."),
  RELIABLE("Reliable", "Ignores the first complication per combat encounter."),
  SUPPRESSED("Suppressed", "An unaware enemy does not notice the attack unless targeted or passing a test."),
  THROWN("Thrown", "Can be thrown as a ranged attack (AGI + Throwing) at the ideal range in brackets."),
  TWO_HANDED("Two-Handed", "Used in one hand, the attack gets +2 difficulty."),
  UNRELIABLE("Unreliable", "The complication range of its attacks grows by 1.");

  /** CRB p. 90: every quality is described on this page. */
  public static final @NonNull Source SOURCE = new Source(Book.CRB, 90);

  private static final @NonNull Pattern THROWN_WITH_RANGE = Pattern.compile("thrown \\(([cm])\\)");

  private final @NonNull String displayName;
  private final @NonNull String summary;

  WeaponQuality(@NonNull String displayName, @NonNull String summary) {
    this.displayName = displayName;
    this.summary = summary;
  }

  /** The name as printed; Thrown without its range. */
  public @NonNull String displayName() {
    return displayName;
  }

  /** One sentence in own words, for tooltips. */
  public @NonNull String summary() {
    return summary;
  }

  /**
   * The quality that can not be on the same weapon (CRB p. 90: Accurate and Inaccurate, Reliable and
   * Unreliable); empty for the others.
   */
  public @NonNull Optional<WeaponQuality> opposite() {
    return switch (this) {
      case ACCURATE -> Optional.of(INACCURATE);
      case INACCURATE -> Optional.of(ACCURATE);
      case RELIABLE -> Optional.of(UNRELIABLE);
      case UNRELIABLE -> Optional.of(RELIABLE);
      default -> Optional.empty();
    };
  }

  /** The short description of a quality as written in a weapon, e.g. {@code Thrown (M)}; empty if unknown. */
  public static @NonNull Optional<String> summaryOf(@NonNull String quality) {
    QualitySelection one = parse(quality);
    return one.qualities().size() == 1 && one.other().isEmpty()
        ? Optional.of(one.qualities().iterator().next().summary()) : Optional.empty();
  }

  /**
   * Reads qualities written as comma-separated text, regardless of case and of the "Close-Quarters" / "Close
   * Quarters" spellings; what is not a quality of the book (or Thrown without C or M) stays in
   * {@link QualitySelection#other()}.
   */
  public static @NonNull QualitySelection parse(@NonNull String text) {
    Set<WeaponQuality> qualities = EnumSet.noneOf(WeaponQuality.class);
    WeaponRange thrownRange = null;
    List<String> other = new ArrayList<>();
    for (String part : text.split(",", -1)) {
      String typed = part.strip();
      if (typed.isEmpty()) {
        continue;
      }
      Matcher thrown = THROWN_WITH_RANGE.matcher(key(typed));
      Optional<WeaponQuality> known = byName(typed);
      if (thrown.matches()) {
        qualities.add(THROWN);
        thrownRange = WeaponRange.byCode(thrown.group(1).toUpperCase(Locale.ROOT)).orElseThrow();
      } else if (known.isPresent() && known.get() != THROWN) {
        qualities.add(known.get());
      } else {
        other.add(typed);
      }
    }
    return new QualitySelection(qualities, thrownRange, String.join(", ", other));
  }

  private static @NonNull Optional<WeaponQuality> byName(@NonNull String name) {
    String key = key(name);
    return Arrays.stream(values()).filter(quality -> key(quality.displayName).equals(key)).findFirst();
  }

  private static @NonNull String key(@NonNull String value) {
    return value.strip().replace('-', ' ').toLowerCase(Locale.ROOT);
  }
}
