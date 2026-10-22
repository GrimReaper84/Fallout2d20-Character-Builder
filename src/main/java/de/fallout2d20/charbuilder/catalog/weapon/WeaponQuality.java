package de.fallout2d20.charbuilder.catalog.weapon;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

/**
 * The weapon qualities of the books (CRB p. 90, WG pp. 56–57), with a short description in own words for tooltips.
 * Thrown comes with an ideal range, written {@code Thrown (C)} or {@code Thrown (M)}; the rated qualities of the
 * Wanderer's Guide with a number, e.g. {@code Recoil (7)} ({@code Delay (X)} when the number is set at the table).
 * Catalog rows and custom weapons keep their qualities as text, this enum only reads and writes that text.
 */
public enum WeaponQuality {
  ACCURATE("Accurate", "After aiming, spend up to 3 AP for +1 CD damage each (not together with ammo for damage)."),
  AMMO_HUNGRY("Ammo-Hungry", "Each shot, and each +1 CD bought with ammunition, spends X rounds; at most Fire Rate "
      + "extra CD.", Book.WG, 56, true),
  BLAST("Blast", "Attacks a whole zone instead of one target; a miss still deals half damage there."),
  BOMBARD("Bombard", "Like Blast; 2 more rounds per extra adjacent zone hit, limited by the Fire Rate.", Book.WG,
      56, false),
  CLOSE_QUARTERS("Close Quarters", "No difficulty increase when used within Reach of an enemy."),
  CONCEALED("Concealed", "Easy to hide; enemies only notice it when wielded or after a thorough search."),
  DEBILITATING("Debilitating", "Injuries it causes are harder to treat (+1 difficulty)."),
  DELAY("Delay", "Goes off X rounds after it is thrown or triggered.", Book.WG, 56, true),
  GATLING("Gatling", "Spends ammunition in bursts of 10; each burst spent for damage gives +2 CD."),
  INACCURATE("Inaccurate", "Aiming gives no benefit."),
  MINE("Mine", "Placed and primed, it damages whoever comes within Reach of it."),
  NIGHT_VISION("Night Vision", "When aiming, darkness does not make the attack harder."),
  PARRY("Parry", "Spend 1 AP to add +1 Defense against a melee attack."),
  PLACED("Placed", "Set in place with a PER + Explosives test (difficulty 1, 2 if attacked since your last turn).",
      Book.WG, 57, false),
  RECOIL("Recoil", "With STR below X, its attacks get +1 difficulty unless it is held two-handed or braced.",
      Book.WG, 57, true),
  RECON("Recon", "After aiming, mark the target: the next ally attacking it may re-roll one d20."),
  RELIABLE("Reliable", "Ignores the first complication per combat encounter."),
  SLOW_LOAD("Slow Load", "After each shot, a minor action reloads it before it can fire again.", Book.WG, 57,
      false),
  SUPPRESSED("Suppressed", "An unaware enemy does not notice the attack unless targeted or passing a test."),
  SURGE("Surge", "Vicious against robots, enemies in Power Armor and other machines.", Book.WG, 57, false),
  THROWN("Thrown", "Can be thrown as a ranged attack (AGI + Throwing) at the ideal range in brackets."),
  TWO_HANDED("Two-Handed", "Used in one hand, the attack gets +2 difficulty."),
  UNRELIABLE("Unreliable", "The complication range of its attacks grows by 1.");

  private static final @NonNull Pattern THROWN_WITH_RANGE = Pattern.compile("thrown \\(([cm])\\)");
  private static final @NonNull Pattern RATED = Pattern.compile("(.+?) \\((\\d+|x)\\)");

  private final @NonNull String displayName;
  private final @NonNull String summary;
  private final @NonNull Book book;
  private final int page;
  private final boolean rated;

  /** A quality of the Core Rulebook (CRB p. 90). */
  WeaponQuality(@NonNull String displayName, @NonNull String summary) {
    this(displayName, summary, Book.CRB, 90, false);
  }

  WeaponQuality(@NonNull String displayName, @NonNull String summary, @NonNull Book book, int page, boolean rated) {
    this.displayName = displayName;
    this.summary = summary;
    this.book = book;
    this.page = page;
    this.rated = rated;
  }

  /** The name as printed; Thrown without its range. */
  public @NonNull String displayName() {
    return displayName;
  }

  /** One sentence in own words, for tooltips. */
  public @NonNull String summary() {
    return summary;
  }

  /** Where the book describes the quality (CRB p. 90, WG pp. 56–57). */
  public @NonNull Source source() {
    return new Source(book, page);
  }

  /** Whether the quality carries a number X, e.g. {@code Recoil (7)} (WG pp. 56–57). */
  public boolean isRated() {
    return rated;
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
   * Quarters" spellings; a rated quality takes its number, or none for {@code (X)} or none written. What is not a
   * quality of the books (or Thrown without C or M, or a rating below 1 or too big, e.g. a typo in a custom weapon)
   * stays in {@link QualitySelection#other()}.
   */
  public static @NonNull QualitySelection parse(@NonNull String text) {
    Set<WeaponQuality> qualities = EnumSet.noneOf(WeaponQuality.class);
    Map<WeaponQuality, Integer> ratings = new EnumMap<>(WeaponQuality.class);
    WeaponRange thrownRange = null;
    List<String> other = new ArrayList<>();
    for (String part : text.split(",", -1)) {
      String typed = part.strip();
      if (typed.isEmpty()) {
        continue;
      }
      Matcher thrown = THROWN_WITH_RANGE.matcher(key(typed));
      Matcher rated = RATED.matcher(key(typed));
      Optional<WeaponQuality> ratedQuality = rated.matches() ? byName(rated.group(1)).filter(WeaponQuality::isRated)
          : Optional.empty();
      Optional<WeaponQuality> known = byName(typed);
      if (thrown.matches()) {
        qualities.add(THROWN);
        thrownRange = WeaponRange.byCode(thrown.group(1).toUpperCase(Locale.ROOT)).orElseThrow();
      } else if (ratedQuality.isPresent() && validRating(rated.group(2))) {
        qualities.add(ratedQuality.get());
        if (!rated.group(2).equals("x")) {
          ratings.put(ratedQuality.get(), Integer.parseInt(rated.group(2)));
        }
      } else if (known.isPresent() && known.get() != THROWN) {
        qualities.add(known.get());
      } else {
        other.add(typed);
      }
    }
    return new QualitySelection(qualities, thrownRange, ratings, String.join(", ", other));
  }

  /** {@code x}, or a whole number from 1 up that fits an int. */
  private static boolean validRating(@NonNull String rating) {
    if (rating.equals("x")) {
      return true;
    }
    try {
      return Integer.parseInt(rating) >= 1;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static @NonNull Optional<WeaponQuality> byName(@NonNull String name) {
    String key = key(name);
    return Arrays.stream(values()).filter(quality -> key(quality.displayName).equals(key)).findFirst();
  }

  private static @NonNull String key(@NonNull String value) {
    return value.strip().replace('-', ' ').toLowerCase(Locale.ROOT);
  }
}
